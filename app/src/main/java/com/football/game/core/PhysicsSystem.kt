package com.football.game.core

import kotlin.math.sqrt
import kotlin.math.abs

/**
 * 物理系统
 * 处理碰撞检测、身体对抗、球的物理
 */
class PhysicsSystem {
    
    companion object {
        // 球场边界
        val FIELD_BOUNDS = BoundingBox(
            min = Vector3(-GameState.FIELD_WIDTH / 2, 0f, -GameState.FIELD_LENGTH / 2),
            max = Vector3(GameState.FIELD_WIDTH / 2, 0.5f, GameState.FIELD_LENGTH / 2)
        )
        
        // 球门区域
        val HOME_GOAL = BoundingBox(
            min = Vector3(-GameState.GOAL_WIDTH / 2, 0f, -GameState.FIELD_LENGTH / 2 - 2f),
            max = Vector3(GameState.GOAL_WIDTH / 2, GameState.GOAL_HEIGHT, -GameState.FIELD_LENGTH / 2)
        )
        
        val AWAY_GOAL = BoundingBox(
            min = Vector3(-GameState.GOAL_WIDTH / 2, 0f, GameState.FIELD_LENGTH / 2),
            max = Vector3(GameState.GOAL_WIDTH / 2, GameState.GOAL_HEIGHT, GameState.FIELD_LENGTH / 2 + 2f)
        )
    }
    
    // ==================== 球员碰撞 ====================
    
    /**
     * 处理两个球员之间的碰撞
     */
    fun resolvePlayerCollision(p1: PlayerModel, p2: PlayerModel): Boolean {
        val box1 = p1.boundingBox
        val box2 = p2.boundingBox
        
        if (!box1.intersects(box2)) return false
        
        // 计算推开向量
        val dx = p1.position.x - p2.position.x
        val dz = p1.position.z - p2.position.z
        val dist = sqrt(dx * dx + dz * dz)
        val minDist = p1.bodyRadius + p2.bodyRadius
        
        if (dist < 0.001f || dist >= minDist) return false
        
        // 推开方向
        val pushDir = Vector3(dx / dist, 0f, dz / dist)
        val overlap = minDist - dist
        
        // 根据体重/力量分配推开量
        val p1Weight = if (p1.isSprinting) 1.2f else 1.0f
        val p2Weight = if (p2.isSprinting) 1.2f else 1.0f
        val totalWeight = p1Weight + p2Weight
        
        val p1Push = overlap * (p2Weight / totalWeight) * 0.5f
        val p2Push = overlap * (p1Weight / totalWeight) * 0.5f
        
        p1.position = p1.position + pushDir * p1Push
        p2.position = p2.position - pushDir * p2Push
        
        return true
    }
    
    /**
     * 处理身体对抗（抢断时）
     */
    fun resolvePhysicalChallenge(
        tackler: PlayerModel, 
        ballCarrier: PlayerModel
    ): PhysicalChallengeResult {
        val dist = tackler.position.distanceTo(ballCarrier.position)
        val challengeRange = tackler.bodyRadius + ballCarrier.bodyRadius + 0.3f
        
        if (dist > challengeRange) {
            return PhysicalChallengeResult.MISS
        }
        
        // 计算对抗强度
        val tacklerStrength = calculateStrength(tackler)
        val carrierStrength = calculateStrength(ballCarrier)
        
        // 护球加成
        val shieldBonus = if (ballCarrier.isShielding) 1.3f else 1.0f
        
        // 冲刺加成
        val sprintBonus = if (tackler.isSprinting) 1.2f else 1.0f
        
        // 位置优势（从侧面或后面抢断更容易）
        val positionAdvantage = calculatePositionAdvantage(tackler, ballCarrier)
        
        // 总对抗值
        val tacklerPower = tacklerStrength * sprintBonus * positionAdvantage
        val carrierPower = carrierStrength * shieldBonus
        
        // 随机因素
        val randomFactor = 0.8f + Math.random().toFloat() * 0.4f
        
        return when {
            tacklerPower * randomFactor > carrierPower * 1.2f -> {
                // 抢断成功，球被断下
                ballCarrier.knockback(
                    tackler.position.directionTo(ballCarrier.position) * 3f,
                    0.2f
                )
                PhysicalChallengeResult.SUCCESS
            }
            tacklerPower * randomFactor > carrierPower * 0.8f -> {
                // 对抗中，球可能失控
                PhysicalChallengeResult.BALL_FREE
            }
            carrierPower * 0.9f > tacklerPower -> {
                // 持球者突破，抢断者被撞开
                tackler.knockback(
                    ballCarrier.position.directionTo(tackler.position) * 4f,
                    0.3f
                )
                PhysicalChallengeResult.CARRIER_WINS
            }
            else -> {
                // 双方都失去平衡
                PhysicalChallengeResult.BOTH_STAGGER
            }
        }
    }
    
    /**
     * 计算球员力量值
     */
    private fun calculateStrength(player: PlayerModel): Float {
        // 基础力量 + 体力加成
        val baseStrength = 50f
        val staminaBonus = (player.stamina / player.maxStamina) * 30f
        return baseStrength + staminaBonus
    }
    
    /**
     * 计算位置优势
     */
    private fun calculatePositionAdvantage(
        tackler: PlayerModel, 
        carrier: PlayerModel
    ): Float {
        // 从侧面或后面抢断更容易
        val tacklerDir = tackler.velocity.normalized()
        val carrierDir = carrier.velocity.normalized()
        
        val dotProduct = tacklerDir.dot(carrierDir)
        
        return when {
            dotProduct > 0.7f -> 0.8f   // 正面拦截（困难）
            dotProduct > 0f -> 1.0f     // 侧面（一般）
            dotProduct > -0.5f -> 1.2f  // 斜后方（容易）
            else -> 1.4f                // 正后方（最容易）
        }
    }
    
    /**
     * 处理所有球员间的碰撞
     */
    fun resolveAllPlayerCollisions(players: List<PlayerModel>) {
        for (i in players.indices) {
            for (j in i + 1 until players.size) {
                resolvePlayerCollision(players[i], players[j])
            }
        }
    }
    
    /**
     * 处理球员与球场边界的碰撞
     */
    fun resolveFieldBounds(player: PlayerModel) {
        val pos = player.position
        val radius = player.bodyRadius
        
        // 左右边线
        if (pos.x - radius < -GameState.FIELD_WIDTH / 2) {
            player.position = Vector3(-GameState.FIELD_WIDTH / 2 + radius, 0f, pos.z)
            player.velocity = Vector3(0f, player.velocity.y, player.velocity.z)
        }
        if (pos.x + radius > GameState.FIELD_WIDTH / 2) {
            player.position = Vector3(GameState.FIELD_WIDTH / 2 - radius, 0f, pos.z)
            player.velocity = Vector3(0f, player.velocity.y, player.velocity.z)
        }
        
        // 底线（球门区域除外）
        if (pos.z - radius < -GameState.FIELD_LENGTH / 2) {
            if (abs(pos.x) > GameState.GOAL_WIDTH / 2) {
                player.position = Vector3(pos.x, 0f, -GameState.FIELD_LENGTH / 2 + radius)
                player.velocity = Vector3(player.velocity.x, player.velocity.y, 0f)
            }
        }
        if (pos.z + radius > GameState.FIELD_LENGTH / 2) {
            if (abs(pos.x) > GameState.GOAL_WIDTH / 2) {
                player.position = Vector3(pos.x, 0f, GameState.FIELD_LENGTH / 2 - radius)
                player.velocity = Vector3(player.velocity.x, player.velocity.y, 0f)
            }
        }
    }
    
    // ==================== 球的物理 ====================
    
    /**
     * 更新球的物理
     */
    fun updateBallPhysics(
        ball: Ball,
        players: List<PlayerModel>,
        delta: Float
    ) {
        if (ball.owner != null) {
            // 球跟随控球者
            updateBallWithOwner(ball, ball.owner!!, delta)
        } else {
            // 球自由运动
            updateFreeBall(ball, delta)
        }
        
        // 检查球的控球权
        checkBallPossession(ball, players)
    }
    
    /**
     * 球跟随控球者
     */
    private fun updateBallWithOwner(ball: Ball, owner: PlayerModel, delta: Float) {
        // 球在控球者脚下
        val footOffset = if (owner.hasBall) {
            // 右脚前方
            Vector3(
                cos(owner.rotation) * 0.6f,
                0.1f,
                sin(owner.rotation) * 0.6f
            )
        } else {
            Vector3(0f, 0.1f, 0f)
        }
        
        val targetPos = owner.position + footOffset
        ball.position = ball.position.lerp(targetPos, delta * 15f)
        ball.velocity = Vector3.ZERO
        ball.height = 0.1f
        ball.heightVelocity = 0f
        ball.groundFriction = 0f
    }
    
    /**
     * 自由球运动
     */
    private fun updateFreeBall(ball: Ball, delta: Float) {
        // 地面摩擦
        if (ball.height < 0.2f) {
            ball.groundFriction = GameState.BALL_FRICTION
            ball.velocity = ball.velocity * (1f - ball.groundFriction * delta)
        }
        
        // 空气阻力
        if (ball.height > 0.1f) {
            ball.velocity = ball.velocity * (1f - GameState.BALL_AIR_DRAG * delta)
        }
        
        // 旋转效应（Magnus效应）
        if (abs(ball.spin) > 0.01f && ball.height > 0.1f) {
            val perp = Vector3(-ball.velocity.z, 0f, ball.velocity.x).normalized()
            ball.velocity = ball.velocity + perp * ball.spin * delta * 3f
            ball.spin *= (1f - delta * 0.5f)
        }
        
        // 重力
        if (ball.height > 0f || ball.heightVelocity > 0f) {
            ball.heightVelocity -= GameState.BALL_GRAVITY * delta
            ball.height += ball.heightVelocity * delta
            
            if (ball.height < 0f) {
                ball.height = 0f
                // 弹跳
                if (abs(ball.heightVelocity) > 1f) {
                    ball.heightVelocity = -ball.heightVelocity * 0.5f
                } else {
                    ball.heightVelocity = 0f
                }
            }
        }
        
        // 应用速度
        ball.position = ball.position + ball.velocity * delta
        ball.position = Vector3(
            ball.position.x.coerceIn(-GameState.FIELD_WIDTH / 2, GameState.FIELD_WIDTH / 2),
            0f,
            ball.position.z
        )
    }
    
    /**
     * 检查球的控球权
     */
    private fun checkBallPossession(ball: Ball, players: List<PlayerModel>) {
        if (ball.owner != null) return
        if (ball.height > 1.5f) return  // 球在空中太高无法控球
        
        var nearestPlayer: PlayerModel? = null
        var nearestDist = Float.MAX_VALUE
        
        for (player in players) {
            val dist = player.position.distanceTo(ball.position)
            val controlZone = player.getBallControlZone()
            
            if (controlZone.contains(ball.position) && dist < nearestDist) {
                nearestDist = dist
                nearestPlayer = player
            }
        }
        
        if (nearestPlayer != null) {
            ball.owner = nearestPlayer
            nearestPlayer!!.hasBall = true
        }
    }
    
    /**
     * 球出界检查
     */
    fun checkBallOutOfBounds(ball: Ball): BallOutOfBoundsResult {
        val pos = ball.position
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2
        val goalHalf = GameState.GOAL_WIDTH / 2
        
        // 边线出界
        if (abs(pos.x) > halfW) {
            return BallOutOfBoundsResult.THROW_IN
        }
        
        // 底线出界
        if (pos.z < -halfL) {
            return if (abs(pos.x) < goalHalf && ball.height < GameState.GOAL_HEIGHT) {
                BallOutOfBoundsResult.GOAL_AWAY  // 客队得分
            } else {
                BallOutOfBoundsResult.GOAL_KICK
            }
        }
        
        if (pos.z > halfL) {
            return if (abs(pos.x) < goalHalf && ball.height < GameState.GOAL_HEIGHT) {
                BallOutOfBoundsResult.GOAL_HOME  // 主队得分
            } else {
                BallOutOfBoundsResult.CORNER_KICK
            }
        }
        
        return BallOutOfBoundsResult.IN_PLAY
    }
}

/**
 * 球数据类
 */
class Ball(
    var position: Vector3 = Vector3.ZERO,
    var velocity: Vector3 = Vector3.ZERO,
    var height: Float = 0f,
    var heightVelocity: Float = 0f,
    var spin: Float = 0f,
    var groundFriction: Float = 0f,
    var owner: PlayerModel? = null
)

/**
 * 对抗结果
 */
enum class PhysicalChallengeResult {
    MISS,           // 没碰到
    SUCCESS,        // 抢断成功
    BALL_FREE,      // 球失控
    CARRIER_WINS,   // 持球者突破
    BOTH_STAGGER    // 双方都失去平衡
}

/**
 * 球出界结果
 */
enum class BallOutOfBoundsResult {
    IN_PLAY,
    THROW_IN,
    GOAL_KICK,
    CORNER_KICK,
    GOAL_HOME,
    GOAL_AWAY
}