package com.football.game.core

import com.football.game.model.Match
import com.football.game.model.Player
import kotlin.random.Random

/**
 * 游戏引擎核心类
 * 除了传球/射门/切人等动作接口，还提供 update() 实时模拟：
 * 球员移动 AI、控球/抢球、球的物理、进球判定与开球重置
 */
class GameEngine(
    val match: Match,
    val homePlayers: List<Player>,
    val awayPlayers: List<Player>
) {
    var ballPosition = Vector3.ZERO
    var ballVelocity = Vector3.ZERO
    var ballHeight = 0f
    var ballHeightVelocity = 0f
    var ballOwner: Player? = null
    
    var activePlayer: Player? = null
    var playerSide: GameState.TeamSide = GameState.TeamSide.HOME
    var inputVector = Vector2D.ZERO
    var isSprinting = false
    
    /** 进球回调（HOME = 主队进攻 +z 方向球门） */
    var onGoal: ((GameState.TeamSide) -> Unit)? = null
    
    /** 最后触球者（进球播报用） */
    var lastTouch: Player? = null
    
    private var pickupCooldown = 0f      // 出球后短暂不可再拿球
    private var aiDecisionCooldown = 0f  // AI 持球决策间隔
    
    data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
        companion object { val ZERO = Vector2D() }
        fun normalized(): Vector2D {
            val len = kotlin.math.sqrt(x * x + y * y)
            return if (len > 0.001f) Vector2D(x / len, y / len) else ZERO
        }
    }
    
    // ==================== 实时模拟 ====================
    
    /**
     * 每帧调用（delta 秒）
     */
    fun update(delta: Float) {
        if (delta <= 0f) return
        pickupCooldown = (pickupCooldown - delta).coerceAtLeast(0f)
        aiDecisionCooldown -= delta
        
        updateControlledPlayer(delta)
        updateAIPlayers(delta)
        updateBall(delta)
        checkGoalAndBounds()
        clampPositions()
    }
    
    private fun updateControlledPlayer(delta: Float) {
        val p = activePlayer ?: return
        if (inputVector.length() > 0.15f) {
            val dir = Vector3(inputVector.x, 0f, inputVector.y).normalized()
            val stats = p.getGameStats()
            val speed = stats.speed * if (isSprinting) 1.35f else 1f
            p.velocity = dir * speed
            p.position = p.position + p.velocity * delta
            p.facingDirection = dir
        } else {
            p.velocity = Vector3.ZERO
        }
        if (ballOwner == p) lastTouch = p
    }
    
    private fun updateAIPlayers(delta: Float) {
        val chaser = pickChaser()
        for (p in homePlayers + awayPlayers) {
            if (p == activePlayer && p.isPlayerControlled) continue
            when {
                p == ballOwner -> dribbleAI(p, delta)
                p.isGoalkeeper -> goalkeeperAI(p, delta)
                p === chaser -> {
                    movePlayer(p, Vector3(ballPosition.x, 0f, ballPosition.z), delta, 1.05f)
                    tryPossession(p)
                }
                else -> formationAI(p, delta)
            }
        }
    }
    
    /** 无主球时距离最近的外场球员追球 */
    private fun pickChaser(): Player? {
        if (ballOwner != null) return null
        var best: Player? = null
        var bestDist = Float.MAX_VALUE
        for (p in homePlayers + awayPlayers) {
            if (p.isGoalkeeper) continue
            val d = p.position.distanceTo(ballPosition)
            if (d < bestDist) {
                bestDist = d
                best = p
            }
        }
        return best
    }
    
    private fun tryPossession(p: Player) {
        if (pickupCooldown > 0f || ballHeight > 1.4f) return
        val dx = p.position.x - ballPosition.x
        val dz = p.position.z - ballPosition.z
        if (dx * dx + dz * dz < 1.2f * 1.2f) {
            ballOwner = p
            p.hasBall = true
            lastTouch = p
            ballHeight = 0.1f
            ballHeightVelocity = 0f
        }
    }
    
    private fun dribbleAI(p: Player, delta: Float) {
        lastTouch = p
        val attackZ = if (p.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }
        // 带球往对方球门推进，稍微往中路收
        val target = Vector3(p.position.x * 0.85f, 0f, attackZ)
        movePlayer(p, target, delta, 0.85f)
        
        if (aiDecisionCooldown > 0f) return
        aiDecisionCooldown = 0.35f
        
        val distToGoal = kotlin.math.abs(attackZ - p.position.z)
        val inShootRange = distToGoal < 24f && kotlin.math.abs(p.position.x) < 18f
        val pressured = nearestOpponentDist(p) < 2.5f
        when {
            inShootRange && (distToGoal < 12f || Random.nextFloat() < 0.55f) -> shootFrom(p)
            pressured || Random.nextFloat() < 0.30f -> passFrom(p)
            else -> {} // 继续带球
        }
    }
    
    private fun nearestOpponentDist(p: Player): Float {
        val opponents = if (p.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers
        var best = Float.MAX_VALUE
        for (o in opponents) {
            val d = p.position.distanceTo(o.position)
            if (d < best) best = d
        }
        return best
    }
    
    private fun formationAI(p: Player, delta: Float) {
        // 阵型位置随球整体移动 25%
        val target = Vector3(
            p.homePosition.x * 0.75f + ballPosition.x * 0.25f,
            0f,
            p.homePosition.z * 0.75f + ballPosition.z * 0.25f
        )
        movePlayer(p, target, delta, 0.8f)
    }
    
    private fun goalkeeperAI(p: Player, delta: Float) {
        val ownGoalZ = if (p.teamSide == GameState.TeamSide.HOME) {
            -GameState.FIELD_LENGTH / 2 + 2.5f
        } else {
            GameState.FIELD_LENGTH / 2 - 2.5f
        }
        val targetX = (ballPosition.x * 0.45f).coerceIn(-5.5f, 5.5f)
        movePlayer(p, Vector3(targetX, 0f, ownGoalZ), delta, 1.1f)
        
        // 抱住进入小禁区的自由球
        if (ballOwner == null && pickupCooldown <= 0f && ballHeight < 2.2f &&
            p.position.distanceTo(ballPosition) < 2.0f
        ) {
            ballOwner = p
            p.hasBall = true
            lastTouch = p
        }
        
        // 持球后大脚开往前场
        if (ballOwner == p && aiDecisionCooldown <= 0f) {
            aiDecisionCooldown = 0.5f
            val dirSign = if (p.teamSide == GameState.TeamSide.HOME) 1 else -1
            val target = Vector3(
                Random.nextFloat() * 20f - 10f, 0f,
                dirSign * (GameState.FIELD_LENGTH / 2 - 20f)
            )
            val dir = (target - p.position).flatten().normalized()
            ballVelocity = dir * (GameState.PASS_SPEED * 1.4f)
            ballHeightVelocity = 3f
            ballOwner = null
            p.hasBall = false
            lastTouch = p
            pickupCooldown = 0.5f
        }
    }
    
    private fun movePlayer(p: Player, target: Vector3, delta: Float, speedFactor: Float) {
        val toTarget = Vector3(target.x - p.position.x, 0f, target.z - p.position.z)
        val dist = toTarget.length()
        if (dist < 0.4f) {
            p.velocity = Vector3.ZERO
            return
        }
        val dir = toTarget.normalized()
        val speed = p.getGameStats().speed * speedFactor
        p.velocity = dir * speed.coerceAtMost(dist / delta)
        p.position = p.position + p.velocity * delta
        p.facingDirection = dir
    }
    
    private fun updateBall(delta: Float) {
        val owner = ballOwner ?: run {
            updateFreeBall(delta)
            return
        }
        // 球粘在控球者前方
        val dir = owner.facingDirection
        val targetX = owner.position.x + dir.x * 0.55f
        val targetZ = owner.position.z + dir.z * 0.55f
        val follow = (delta * 12f).coerceAtMost(1f)
        ballPosition = Vector3(
            ballPosition.x + (targetX - ballPosition.x) * follow,
            0f,
            ballPosition.z + (targetZ - ballPosition.z) * follow
        )
        ballHeight = 0.11f
        ballHeightVelocity = 0f
        ballVelocity = Vector3.ZERO
    }
    
    private fun updateFreeBall(delta: Float) {
        // 高度物理（重力 + 弹跳）
        ballHeightVelocity -= GameState.BALL_GRAVITY * delta
        ballHeight += ballHeightVelocity * delta
        if (ballHeight < 0f) {
            ballHeight = 0f
            ballHeightVelocity = if (kotlin.math.abs(ballHeightVelocity) > 1.5f) {
                -ballHeightVelocity * 0.55f
            } else {
                0f
            }
        }
        // 地面摩擦 / 空气阻力
        val drag = if (ballHeight < 0.15f) GameState.BALL_FRICTION else GameState.BALL_AIR_DRAG
        ballVelocity = ballVelocity * (1f - drag * delta)
        ballPosition = ballPosition + ballVelocity * delta
        
        // 边线反弹
        val halfW = GameState.FIELD_WIDTH / 2
        if (ballPosition.x < -halfW) {
            ballPosition = Vector3(-halfW, 0f, ballPosition.z)
            ballVelocity = Vector3(-ballVelocity.x * 0.6f, 0f, ballVelocity.z)
        } else if (ballPosition.x > halfW) {
            ballPosition = Vector3(halfW, 0f, ballPosition.z)
            ballVelocity = Vector3(-ballVelocity.x * 0.6f, 0f, ballVelocity.z)
        }
        // 底线反弹（球门范围内除外，由 checkGoalAndBounds 处理）
        val halfL = GameState.FIELD_LENGTH / 2
        val outsideGoalMouth = kotlin.math.abs(ballPosition.x) > GameState.GOAL_WIDTH / 2 || ballHeight > 2.44f
        if (outsideGoalMouth) {
            if (ballPosition.z > halfL) {
                ballPosition = Vector3(ballPosition.x, 0f, halfL)
                ballVelocity = Vector3(ballVelocity.x, 0f, -ballVelocity.z * 0.6f)
            } else if (ballPosition.z < -halfL) {
                ballPosition = Vector3(ballPosition.x, 0f, -halfL)
                ballVelocity = Vector3(ballVelocity.x, 0f, -ballVelocity.z * 0.6f)
            }
        }
    }
    
    private fun checkGoalAndBounds() {
        val halfL = GameState.FIELD_LENGTH / 2
        val inGoalMouth = kotlin.math.abs(ballPosition.x) < GameState.GOAL_WIDTH / 2 && ballHeight < 2.44f
        
        if (ballPosition.z > halfL + 0.2f) {
            // 主队进攻 +z 球门
            if (inGoalMouth) {
                onGoal?.invoke(GameState.TeamSide.HOME)
                kickoffReset()
            } else if (ballPosition.z > halfL + 3f) {
                kickoffReset()  // 出底线，简化为重新开球
            }
        } else if (ballPosition.z < -halfL - 0.2f) {
            if (inGoalMouth) {
                onGoal?.invoke(GameState.TeamSide.AWAY)
                kickoffReset()
            } else if (ballPosition.z < -halfL - 3f) {
                kickoffReset()
            }
        }
    }
    
    /** 开球重置 */
    fun kickoffReset() {
        ballPosition = Vector3.ZERO
        ballVelocity = Vector3.ZERO
        ballHeight = 0f
        ballHeightVelocity = 0f
        ballOwner = null
        lastTouch = null
        pickupCooldown = 0.3f
        for (p in homePlayers + awayPlayers) {
            p.position = p.homePosition
            p.velocity = Vector3.ZERO
            p.hasBall = false
            p.facingDirection = if (p.teamSide == GameState.TeamSide.HOME) {
                Vector3(0f, 0f, 1f)
            } else {
                Vector3(0f, 0f, -1f)
            }
        }
    }
    
    private fun clampPositions() {
        val halfW = GameState.FIELD_WIDTH / 2 + 1f
        val halfL = GameState.FIELD_LENGTH / 2 + 1f
        for (p in homePlayers + awayPlayers) {
            if (kotlin.math.abs(p.position.x) > halfW || kotlin.math.abs(p.position.z) > halfL) {
                p.position = Vector3(
                    p.position.x.coerceIn(-halfW, halfW),
                    0f,
                    p.position.z.coerceIn(-halfL, halfL)
                )
            }
        }
    }
    
    // ==================== 动作接口 ====================
    
    fun doPass() {
        val player = activePlayer ?: return
        passFrom(player)
    }
    
    fun doShoot() {
        val player = activePlayer ?: return
        shootFrom(player)
    }
    
    fun doThroughBall() {
        val player = activePlayer ?: return
        if (ballOwner != player) return
        
        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1
        
        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE
        
        for (teammate in team) {
            if (teammate == player) continue
            val forwardScore = (teammate.position.z - player.position.z) * forwardDir
            val dist = player.position.distanceTo(teammate.position)
            if (dist in 5f..40f && forwardScore > 5) {
                if (forwardScore > bestScore) {
                    bestScore = forwardScore
                    bestTarget = teammate
                }
            }
        }
        
        if (bestTarget != null) {
            val lead = Vector3(0f, 0f, forwardDir * 8f)
            val targetPos = bestTarget.position + lead
            val dir = (targetPos - player.position).flatten().normalized()
            ballVelocity = dir * (GameState.PASS_SPEED * 1.3f)
            ballHeightVelocity = 0.3f
            ballOwner = null
            player.hasBall = false
            lastTouch = player
            pickupCooldown = 0.4f
        }
    }
    
    /** 通用传球（指定出球者） */
    fun passFrom(player: Player) {
        if (ballOwner != player) return
        
        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1
        
        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE
        
        for (teammate in team) {
            if (teammate == player) continue
            val forwardScore = (teammate.position.z - player.position.z) * forwardDir
            val dist = player.position.distanceTo(teammate.position)
            if (dist in 3f..40f) {
                val score = forwardScore - dist * 0.2f
                if (score > bestScore) {
                    bestScore = score
                    bestTarget = teammate
                }
            }
        }
        
        if (bestTarget != null) {
            val dir = (bestTarget.position - player.position).flatten().normalized()
            ballVelocity = dir * GameState.PASS_SPEED
            ballHeightVelocity = 0.5f
            ballOwner = null
            player.hasBall = false
            lastTouch = player
            pickupCooldown = 0.4f
        }
    }
    
    /** 通用射门（指定射门者） */
    fun shootFrom(player: Player) {
        if (ballOwner != player) return
        
        val targetGoalZ = if (player.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }
        
        val dir = Vector3(
            Random.nextFloat() * 6f - 3f - player.position.x * 0.05f,
            0f,
            targetGoalZ - player.position.z
        ).normalized()
        
        ballVelocity = dir * GameState.SHOT_SPEED
        ballHeightVelocity = 2f + Random.nextFloat() * 2f
        ballOwner = null
        player.hasBall = false
        lastTouch = player
        pickupCooldown = 0.4f
    }
    
    fun switchPlayer() {
        val team = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        if (team.isEmpty()) return
        
        val currentIdx = team.indexOf(activePlayer)
        for (i in 1..team.size) {
            val idx = (currentIdx + i) % team.size
            if (!team[idx].isGoalkeeper) {
                activePlayer?.isActive = false
                activePlayer?.isPlayerControlled = false
                activePlayer = team[idx]
                team[idx].isActive = true
                team[idx].isPlayerControlled = true
                return
            }
        }
    }
    
    companion object {
        private val ROLES = listOf("GK", "LB", "CB", "CB", "RB", "CM", "CM", "CM", "LW", "ST", "RW")
        private val NUMBERS = listOf(1, 3, 4, 5, 2, 6, 8, 10, 11, 9, 7)
        
        // 4-3-3 基础站位 (x, z)，主队进攻 +z，场地 105 x 68
        private val BASE_POSITIONS = listOf(
            Pair(0f, -44f),                                                   // GK
            Pair(-22f, -30f), Pair(-8f, -33f), Pair(8f, -33f), Pair(22f, -30f), // DF
            Pair(-18f, -12f), Pair(0f, -16f), Pair(18f, -12f),                 // MF
            Pair(-24f, 4f), Pair(0f, 2f), Pair(24f, 4f)                        // FW
        )
        
        /**
         * 为一支球队生成 11 名球员（4-3-3）
         * 下标 0 = 门将，下标 9 = 中锋（招牌球星位）
         */
        fun createTeamPlayers(team: com.football.game.model.Team?, side: GameState.TeamSide): List<Player> {
            val sign = if (side == GameState.TeamSide.HOME) 1f else -1f
            return BASE_POSITIONS.mapIndexed { i, (x, z) ->
                Player(
                    id = "${team?.id ?: side.name}_$i",
                    name = "${NUMBERS[i]}号",
                    number = NUMBERS[i],
                    role = ROLES[i],
                    teamSide = side,
                    teamId = team?.id ?: "",
                    teamName = team?.name ?: "",
                    isGoalkeeper = i == 0,
                    position = Vector3(x * sign, 0f, z * sign),
                    homePosition = Vector3(x * sign, 0f, z * sign)
                )
            }
        }
    }
}
