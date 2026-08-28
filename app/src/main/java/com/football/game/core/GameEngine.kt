package com.football.game.core

import com.football.game.ai.PlayerAI
import com.football.game.ai.TeamAI
import com.football.game.model.Match
import com.football.game.model.Player
import com.football.game.model.Team
import kotlin.random.Random

/**
 * 游戏引擎核心类
 * 管理比赛逻辑、物理、AI、碰撞等
 */
class GameEngine(
    private val match: Match,
    private val homePlayers: List<Player>,
    private val awayPlayers: List<Player>
) {
    // 球的状态
    var ballPosition = Vector3.ZERO
        private set
    var ballVelocity = Vector3.ZERO
        private set
    var ballHeight = 0.0f
        private set
    var ballHeightVelocity = 0.0f
        private set
    var ballOwner: Player? = null
        private set
    var ballSpin = 0.0f
        private set

    // 比赛状态
    var lastTouchTeam: GameState.TeamSide? = null
    private var lastTouchPlayer: Player? = null
    private var goalCelebrationTime = 0.0f
    private var outOfBoundsTime = 0.0f

    // AI
    private val playerAIs = mutableMapOf<Player, PlayerAI>()
    private val homeTeamAI = TeamAI()
    private val awayTeamAI = TeamAI()

    // 进球追踪
    private val hatTrickTracker = mutableMapOf<String, Int>()

    // 玩家控制
    var activePlayer: Player? = null
        private set
    var playerSide: GameState.TeamSide = GameState.TeamSide.HOME

    // 输入
    var inputVector = Vector2D.ZERO
    var isSprinting = false

    init {
        // 初始化AI
        homePlayers.forEach { playerAIs[it] = PlayerAI() }
        awayPlayers.forEach { playerAIs[it] = PlayerAI() }
    }

    /**
     * 2D向量（简化版，用于输入）
     */
    data class Vector2D(val x: Float = 0.0f, val y: Float = 0.0f) {
        companion object {
            val ZERO = Vector2D()
        }
        fun normalized(): Vector2D {
            val len = kotlin.math.sqrt(x * x + y * y)
            return if (len > 0.0001f) Vector2D(x / len, y / len) else ZERO
        }
    }

    /**
     * 初始化比赛
     */
    fun initialize() {
        // 重置球员位置
        resetPositions()

        // 球放中圈
        ballPosition = Vector3.ZERO
        ballVelocity = Vector3.ZERO
        ballHeight = 0.0f
        ballHeightVelocity = 0.0f
        ballOwner = null
        ballSpin = 0.0f

        // 设置活跃球员
        switchToNearestPlayer()

        // 开球
        kickoff()
    }

    /**
     * 重置球员位置
     */
    private fun resetPositions() {
        val formation = GameState.getFormation(match.config.toString())  // 简化，实际应从config获取

        homePlayers.forEachIndexed { index, player ->
            if (index < formation.size) {
                val (role, x, z) = formation[index]
                player.position = Vector3(x, 0.0f, z)
                player.homePosition = Vector3(x, 0.0f, z)
            }
            player.resetToHome()
        }

        awayPlayers.forEachIndexed { index, player ->
            if (index < formation.size) {
                val (role, x, z) = formation[index]
                player.position = Vector3(x, 0.0f, -z)  // 客队镜像
                player.homePosition = Vector3(x, 0.0f, -z)
            }
            player.resetToHome()
        }
    }

    /**
     * 开球
     */
    fun kickoff() {
        resetPositions()

        ballPosition = Vector3.ZERO
        ballVelocity = Vector3.ZERO
        ballHeight = 0.0f
        ballHeightVelocity = 0.0f
        ballOwner = null
        ballSpin = 0.0f

        // 开球方（失球方或上半场主队）
        var kickoffSide = GameState.TeamSide.HOME
        if (match.homeScore + match.awayScore > 0) {
            kickoffSide = if (match.homeScore > match.awayScore) {
                GameState.TeamSide.AWAY
            } else {
                GameState.TeamSide.HOME
            }
        }
        if (match.currentHalf == 2 && match.homeScore == 0 && match.awayScore == 0) {
            kickoffSide = GameState.TeamSide.AWAY
        }

        // 让开球方的一名前锋靠近球
        val kickoffTeam = if (kickoffSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        for (player in kickoffTeam) {
            if (player.role in listOf("ST", "CF", "CAM")) {
                player.position = Vector3(0.0f, 0.0f, if (kickoffSide == GameState.TeamSide.HOME) 0.5f else -0.5f)
                break
            }
        }

        match.phase = GameState.MatchPhase.KICKOFF
        outOfBoundsTime = 1.5f  // 1.5秒后开始
    }

    /**
     * 更新游戏逻辑
     */
    fun update(delta: Float) {
        when (match.phase) {
            GameState.MatchPhase.PLAYING -> {
                match.matchTime += delta
                if (match.matchTime >= match.config.halfDuration) {
                    endHalf()
                }
                updatePlayerInput()
                updateAI(delta)
            }
            GameState.MatchPhase.GOAL -> {
                goalCelebrationTime -= delta
                if (goalCelebrationTime <= 0) {
                    kickoff()
                }
            }
            GameState.MatchPhase.BALL_OUT, GameState.MatchPhase.KICKOFF -> {
                outOfBoundsTime -= delta
                if (outOfBoundsTime <= 0) {
                    if (match.phase == GameState.MatchPhase.KICKOFF) {
                        match.phase = GameState.MatchPhase.PLAYING
                    } else {
                        resumeFromOut()
                    }
                }
            }
            GameState.MatchPhase.PAUSED -> {
                // 暂停时不更新
            }
            else -> {}
        }

        // 物理更新
        if (match.phase == GameState.MatchPhase.PLAYING || match.phase == GameState.MatchPhase.KICKOFF) {
            updateBallPhysics(delta)
            checkCollisions()
            checkBoundsAndGoals()
        }

        // 更新球员动画
        updatePlayerAnimations(delta)
    }

    /**
     * 更新球员输入
     */
    private fun updatePlayerInput() {
        val player = activePlayer ?: return

        if (match.phase != GameState.MatchPhase.PLAYING) {
            player.inputDirection = Vector3.ZERO
            return
        }

        // 读取移动输入
        val normalizedInput = inputVector.normalized()
        player.inputDirection = Vector3(normalizedInput.x, 0.0f, normalizedInput.y)
        player.isSprinting = isSprinting
        player.isPlayerControlled = true
    }

    /**
     * 更新AI
     */
    private fun updateAI(delta: Float) {
        // 更新球队战术
        val homeScore = match.homeScore
        val awayScore = match.awayScore
        val timeRemaining = match.config.halfDuration - match.matchTime
        val hasPossession = ballOwner?.teamSide == GameState.TeamSide.HOME

        homeTeamAI.updateTactics(homeScore, awayScore, timeRemaining, hasPossession)
        awayTeamAI.updateTactics(awayScore, homeScore, timeRemaining, !hasPossession)

        // 更新球员AI
        for (player in homePlayers + awayPlayers) {
            if (player == activePlayer && player.isPlayerControlled) continue

            val ai = playerAIs[player] ?: continue
            val teammates = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
            val opponents = if (player.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers
            val teamAI = if (player.teamSide == GameState.TeamSide.HOME) homeTeamAI else awayTeamAI

            ai.update(
                player = player,
                delta = delta,
                hasBall = ballOwner == player,
                isNearestToBall = isNearestToBall(player),
                ballPosition = ballPosition,
                teammates = teammates,
                opponents = opponents,
                aiParams = GameState.getAIParams(),
                teamAI = teamAI
            )
        }
    }

    /**
     * 更新球员动画
     */
    private fun updatePlayerAnimations(delta: Float) {
        for (player in homePlayers + awayPlayers) {
            // 更新动画状态
            val speed = player.velocity.length()
            player.animState = when {
                player.actionCooldown > 0 -> Player.AnimState.KICK
                speed < 0.5f -> Player.AnimState.IDLE
                speed < 3.0f -> Player.AnimState.WALK
                speed < 6.0f -> Player.AnimState.RUN
                else -> Player.AnimState.SPRINT
            }

            // 更新动作冷却
            if (player.actionCooldown > 0) {
                player.actionCooldown -= delta
            }

            // 更新2过1状态
            if (player.oneTwoTimer > 0) {
                player.oneTwoTimer -= delta
                if (player.oneTwoTimer <= 0) {
                    player.oneTwoPartner = null
                }
            }

            // 更新体力
            val stats = player.getGameStats()
            if (player.isSprinting && player.inputDirection.length() > 0.1f) {
                player.currentStamina -= stats.staminaDrain * delta
            } else {
                player.currentStamina += stats.staminaRecover * delta
            }
            player.currentStamina = player.currentStamina.coerceIn(0.0f, stats.stamina)
        }
    }

    /**
     * 更新球的物理
     */
    private fun updateBallPhysics(delta: Float) {
        val owner = ballOwner
        if (owner != null) {
            // 如果有控球者，球跟随控球者
            val forward = owner.facingDirection
            ballPosition = owner.position + Vector3(forward.x, 0.0f, forward.z) * 0.8f + Vector3(0.0f, GameState.BALL_RADIUS, 0.0f)
            ballVelocity = Vector3.ZERO
            ballHeight = 0.0f
            ballHeightVelocity = 0.0f
        } else {
            // 地面摩擦
            val friction = GameState.BALL_FRICTION * delta
            ballVelocity = ballVelocity * (1.0f - friction)

            // 空气阻力（球在空中时）
            if (ballHeight > 0.1f) {
                ballVelocity = ballVelocity * (1.0f - GameState.BALL_AIR_DRAG * delta)
            }

            // 旋转效应（搓射/电梯球）
            if (kotlin.math.abs(ballSpin) > 0.01f && ballHeight > 0.1f) {
                // Magnus效应：旋转产生侧向力
                val perp = Vector3(-ballVelocity.z, 0.0f, ballVelocity.x).normalized()
                ballVelocity = ballVelocity + perp * ballSpin * delta * 3.0f
                ballSpin *= (1.0f - delta * 0.5f)  // 旋转衰减
            }

            // 重力
            if (ballHeight > 0 || ballHeightVelocity > 0) {
                ballHeightVelocity -= GameState.BALL_GRAVITY * delta
                ballHeight += ballHeightVelocity * delta
                if (ballHeight < 0) {
                    ballHeight = 0.0f
                    // 弹跳
                    if (kotlin.math.abs(ballHeightVelocity) > 1.0f) {
                        ballHeightVelocity = -ballHeightVelocity * 0.5f
                    } else {
                        ballHeightVelocity = 0.0f
                    }
                }
            } else {
                ballHeight = 0.0f
                ballHeightVelocity = 0.0f
            }

            // 应用速度
            ballPosition = ballPosition + ballVelocity * delta
            ballPosition = Vector3(ballPosition.x, 0.0f, ballPosition.z)

            // 检查控球
            checkBallPossession()
        }
    }

    /**
     * 检查球的控球权
     */
    private fun checkBallPossession() {
        if (ballOwner != null) return

        val allPlayers = homePlayers + awayPlayers
        var nearestPlayer: Player? = null
        var nearestDist = Float.MAX_VALUE

        for (player in allPlayers) {
            // 球在空中时，只有高度低于1.5米才能控球
            if (ballHeight > 1.5f) continue

            val dist = player.position.distanceTo(ballPosition)
            val controlRadius = player.getGameStats().controlRadius
            if (dist < controlRadius && dist < nearestDist) {
                nearestDist = dist
                nearestPlayer = player
            }
        }

        if (nearestPlayer != null) {
            ballOwner = nearestPlayer
            nearestPlayer.hasBall = true
            lastTouchTeam = nearestPlayer.teamSide
            lastTouchPlayer = nearestPlayer

            // 如果是玩家控制的队伍，切换到控球者
            if (nearestPlayer.teamSide == playerSide && !nearestPlayer.isGoalkeeper) {
                switchToPlayer(nearestPlayer)
            }
        }
    }

    /**
     * 检查碰撞
     */
    private fun checkCollisions() {
        // 球员间碰撞和抢断检测
        for (player in homePlayers + awayPlayers) {
            val opponents = if (player.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers
            for (opponent in opponents) {
                val dist = player.position.distanceTo(opponent.position)
                if (dist < 1.0f) {
                    // 碰撞推开
                    val pushDir = (player.position - opponent.position).normalized()
                    player.position = player.position + pushDir * 0.02f
                    opponent.position = opponent.position - pushDir * 0.02f

                    // 抢断检测
                    if (player.isPlayerControlled || player.isActive) {
                        if (dist < player.getGameStats().tackleRadius) {
                            checkTackle(player, opponent)
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查抢断
     */
    private fun checkTackle(tackler: Player, target: Player) {
        if (ballOwner == target) {
            val aiParams = GameState.getAIParams()
            var successChance = aiParams.tackleSuccess

            // 根据防守属性调整
            val tackleAttr = target.defending / 100.0f
            successChance *= (0.5f + tackleAttr * 0.5f)

            if (Random.nextFloat() < successChance) {
                // 抢断成功
                ballOwner = null
                target.hasBall = false
                val dir = (ballPosition - tackler.position).flatten().normalized()
                ballVelocity = dir * 8.0f
                ballHeightVelocity = 1.0f
                tackler.playAction(Player.AnimState.TACKLE, 0.3f)
            } else {
                // 抢断犯规检测
                val foulSeverity = Random.nextFloat()
                if (foulSeverity > 0.7f) {
                    // 犯规
                    match.recordFoul(tackler.teamSide)
                }
            }
        }
    }

    /**
     * 检查边界和进球
     */
    private fun checkBoundsAndGoals() {
        if (match.phase != GameState.MatchPhase.PLAYING) return

        val halfL = GameState.FIELD_LENGTH / 2
        val halfW = GameState.FIELD_WIDTH / 2
        val goalHalf = GameState.GOAL_WIDTH / 2

        // 进球判定（球在球门内且高度低于横梁）
        if (ballPosition.z < -halfL && kotlin.math.abs(ballPosition.x) < goalHalf && ballHeight < GameState.GOAL_HEIGHT) {
            // 进了主队球门 -> 客队得分
            val isOwnGoal = lastTouchTeam == GameState.TeamSide.AWAY
            scoreGoal(GameState.TeamSide.AWAY, isOwnGoal)
            return
        }

        if (ballPosition.z > halfL && kotlin.math.abs(ballPosition.x) < goalHalf && ballHeight < GameState.GOAL_HEIGHT) {
            // 进了客队球门 -> 主队得分
            val isOwnGoal = lastTouchTeam == GameState.TeamSide.HOME
            scoreGoal(GameState.TeamSide.HOME, isOwnGoal)
            return
        }

        // 出界判定
        if (kotlin.math.abs(ballPosition.x) > halfW) {
            // 边线出界 -> 界外球
            val throwInSide = if (lastTouchTeam == GameState.TeamSide.HOME) {
                GameState.TeamSide.AWAY
            } else {
                GameState.TeamSide.HOME
            }
            // 简化处理，直接恢复比赛
            match.phase = GameState.MatchPhase.BALL_OUT
            outOfBoundsTime = 2.0f
            return
        }

        if (ballPosition.z < -halfL && kotlin.math.abs(ballPosition.x) > goalHalf) {
            // 主队底线出界
            match.phase = GameState.MatchPhase.BALL_OUT
            outOfBoundsTime = 2.0f
            return
        }

        if (ballPosition.z > halfL && kotlin.math.abs(ballPosition.x) > goalHalf) {
            // 客队底线出界
            match.phase = GameState.MatchPhase.BALL_OUT
            outOfBoundsTime = 2.0f
            return
        }
    }

    /**
     * 进球
     */
    private fun scoreGoal(scoringTeam: GameState.TeamSide, isOwnGoal: Boolean) {
        val scorer = lastTouchPlayer
        val scorerId = scorer?.id ?: ""
        val scorerNumber = scorer?.number ?: 0
        val minute = match.getCurrentMinute()

        // 判断进球类型
        var goalType = "普通进球"

        // 帽子戏法追踪
        if (scorerId.isNotEmpty()) {
            hatTrickTracker[scorerId] = (hatTrickTracker[scorerId] ?: 0) + 1
            when (hatTrickTracker[scorerId]) {
                3 -> goalType = "帽子戏法！"
                4 -> goalType = "大四喜！"
                in 5..Int.MAX_VALUE -> goalType = "独中五元！"
            }
        }

        // 世界波判定（远射）
        if (scorer != null) {
            val goalZ = if (scoringTeam == GameState.TeamSide.HOME) {
                GameState.FIELD_LENGTH / 2
            } else {
                -GameState.FIELD_LENGTH / 2
            }
            if (kotlin.math.abs(scorer.position.z - goalZ) > 25 && goalType == "普通进球") {
                goalType = "世界波！"
            }
        }

        val goalRecord = Match.GoalRecord(
            playerId = scorerId,
            playerName = scorer?.name ?: "球员$scorerNumber",
            playerNumber = scorerNumber,
            minute = minute,
            type = goalType,
            isOwnGoal = isOwnGoal
        )

        match.addGoal(scoringTeam, goalRecord)

        // 统计
        match.recordShot(scoringTeam, true)

        match.phase = GameState.MatchPhase.GOAL
        goalCelebrationTime = 3.0f
        ballOwner = null
    }

    /**
     * 从出界恢复
     */
    private fun resumeFromOut() {
        match.phase = GameState.MatchPhase.PLAYING
    }

    /**
     * 半场结束
     */
    private fun endHalf() {
        if (match.currentHalf == 1) {
            match.currentHalf = 2
            match.matchTime = 0.0f
            kickoff()
        } else {
            match.phase = GameState.MatchPhase.FULLTIME
        }
    }

    /**
     * 切换到最近的球员
     */
    fun switchToNearestPlayer() {
        val team = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        if (team.isEmpty()) return

        var bestPlayer: Player? = null
        var bestDist = Float.MAX_VALUE

        for (player in team) {
            if (player.isGoalkeeper) continue
            val dist = player.position.distanceTo(ballPosition)
            if (dist < bestDist) {
                bestDist = dist
                bestPlayer = player
            }
        }

        if (bestPlayer == null && team.isNotEmpty()) {
            bestPlayer = team[0]
        }

        bestPlayer?.let { switchToPlayer(it) }
    }

    /**
     * 手动切换到下一个球员
     */
    fun switchToNextPlayer() {
        val team = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        if (team.isEmpty()) return

        val currentIdx = team.indexOf(activePlayer)

        // 找下一个非门将球员
        for (i in 1..team.size) {
            val idx = (currentIdx + i) % team.size
            if (!team[idx].isGoalkeeper) {
                switchToPlayer(team[idx])
                return
            }
        }
    }

    /**
     * 切换到指定球员
     */
    private fun switchToPlayer(player: Player) {
        activePlayer?.let {
            it.isActive = false
            it.isPlayerControlled = false
        }

        activePlayer = player
        player.isActive = true
        player.isPlayerControlled = true
    }

    /**
     * 检查球员是否离球最近
     */
    private fun isNearestToBall(player: Player): Boolean {
        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val myDist = player.position.distanceTo(ballPosition)

        for (teammate in team) {
            if (teammate == player || teammate.isGoalkeeper) continue
            if (teammate.position.distanceTo(ballPosition) < myDist) {
                return false
            }
        }
        return true
    }

    // ==================== 玩家动作 ====================

    /**
     * 传球
     */
    fun doPass() {
        val player = activePlayer ?: return
        if (ballOwner != player) return

        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1

        // 寻找最佳传球目标
        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE

        for (teammate in team) {
            if (teammate == player) continue
            val forwardScore = (teammate.position.z - player.position.z) * forwardDir
            val dist = player.position.distanceTo(teammate.position)
            if (dist in 3.0f..40.0f) {
                val score = forwardScore - dist * 0.2f
                if (score > bestScore) {
                    bestScore = score
                    bestTarget = teammate
                }
            }
        }

        if (bestTarget != null) {
            val dir = (bestTarget.position - player.position).flatten().normalized()

            // 传球准确率
            val aiParams = GameState.getAIParams()
            val passAcc = aiParams.passAccuracy * (0.5f + player.passing / 200.0f)
            val finalDir = if (Random.nextFloat() > passAcc) {
                Vector3(
                    dir.x + Random.nextFloat(-0.2f, 0.2f),
                    0.0f,
                    dir.z + Random.nextFloat(-0.2f, 0.2f)
                ).normalized()
            } else {
                dir
            }

            ballVelocity = finalDir * GameState.PASS_SPEED
            ballHeightVelocity = 0.5f
            ballOwner = null
            player.hasBall = false
            player.playAction(Player.AnimState.KICK, 0.3f)
        }
    }

    /**
     * 射门
     */
    fun doShoot() {
        val player = activePlayer ?: return
        if (ballOwner != player) return

        val targetGoalZ = if (player.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }

        val dir = Vector3(
            Random.nextFloat(-3.0f, 3.0f) - player.position.x * 0.05f,
            0.0f,
            targetGoalZ - player.position.z
        ).normalized()

        // 射门属性影响
        val aiParams = GameState.getAIParams()
        val accuracy = aiParams.shotAccuracy * (0.5f + player.shooting / 200.0f)
        val finalDir = if (Random.nextFloat() > accuracy) {
            Vector3(
                dir.x + Random.nextFloat(-0.3f, 0.3f),
                0.0f,
                dir.z
            ).normalized()
        } else {
            dir
        }

        val power = GameState.SHOT_SPEED * Random.nextFloat(0.85f, 1.0f)
        ballVelocity = finalDir * power
        ballHeightVelocity = 2.0f + Random.nextFloat() * 2.0f
        ballOwner = null
        player.hasBall = false
        player.playAction(Player.AnimState.KICK, 0.5f)

        // 统计
        match.recordShot(player.teamSide, true)
    }

    /**
     * 直塞
     */
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
            if (dist in 5.0f..40.0f && forwardScore > 5) {
                if (forwardScore > bestScore) {
                    bestScore = forwardScore
                    bestTarget = teammate
                }
            }
        }

        if (bestTarget != null) {
            val lead = Vector3(0.0f, 0.0f, forwardDir * 8.0f)
            val targetPos = bestTarget.position + lead
            val dir = (targetPos - player.position).flatten().normalized()

            ballVelocity = dir * (GameState.PASS_SPEED * 1.3f)
            ballHeightVelocity = 0.3f
            ballOwner = null
            player.hasBall = false
            player.playAction(Player.AnimState.KICK, 0.3f)
        }
    }

    /**
     * 切换控制的球员
     */
    fun switchPlayer() {
        switchToNextPlayer()
    }
}