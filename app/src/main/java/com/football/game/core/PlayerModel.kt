package com.football.game.core

import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos

/**
 * 球员3D模型
 * 包含碰撞体积、动画状态、身体对抗
 */
class PlayerModel(
    var position: Vector3 = Vector3.ZERO,
    var rotation: Float = 0f,  // 朝向角度（弧度）
    val teamSide: GameState.TeamSide = GameState.TeamSide.HOME,
    val isGoalkeeper: Boolean = false
) {
    // ==================== 碰撞体积 ====================
    // 身体尺寸
    val bodyHeight: Float = 1.8f      // 身体高度
    val bodyRadius: Float = 0.35f     // 身体半径（圆柱体）
    val headRadius: Float = 0.15f     // 头部半径
    val shoulderWidth: Float = 0.5f   // 肩宽
    
    // 腿部碰撞（用于抢断）
    val legReach: Float = 0.8f        // 腿部伸展距离
    val legHeight: Float = 0.6f       // 腿部高度
    
    // 碰撞体中心
    val collisionCenter: Vector3
        get() = Vector3(position.x, bodyHeight / 2, position.z)
    
    // 碰撞体包围盒
    val boundingBox: BoundingBox
        get() = BoundingBox(
            min = Vector3(
                position.x - bodyRadius,
                0f,
                position.z - bodyRadius
            ),
            max = Vector3(
                position.x + bodyRadius,
                bodyHeight,
                position.z + bodyRadius
            )
        )
    
    // ==================== 运动状态 ====================
    var velocity: Vector3 = Vector3.ZERO
    var targetVelocity: Vector3 = Vector3.ZERO
    var acceleration: Float = 25f     // 加速度
    var maxSpeed: Float = 7f          // 最大速度
    var sprintMultiplier: Float = 1.4f // 冲刺倍率
    var isSprinting: Boolean = false
    
    // 体力
    var stamina: Float = 100f
    var maxStamina: Float = 100f
    var staminaDrain: Float = 8f      // 冲刺体力消耗/秒
    var staminaRecover: Float = 5f    // 体力恢复/秒
    
    // ==================== 球权状态 ====================
    var hasBall: Boolean = false
    var ballDistance: Float = 0.5f     // 球与脚的距离
    var ballHeight: Float = 0.1f      // 球的高度
    
    // ==================== 动画状态 ====================
    var animState: AnimState = AnimState.IDLE
    var animTime: Float = 0f
    var actionCooldown: Float = 0f
    
    // 身体部位位置（用于动画）
    var leftFootPos: Vector3 = Vector3.ZERO
    var rightFootPos: Vector3 = Vector3.ZERO
    var bodyLean: Float = 0f          // 身体倾斜（盘带时）
    var armSwing: Float = 0f          // 手臂摆动
    
    // ==================== 对抗状态 ====================
    var isShielding: Boolean = false   // 是否在护球
    var shieldingStrength: Float = 0f  // 护球强度
    var contactForce: Vector3 = Vector3.ZERO  // 接触力
    var isKnockedBack: Boolean = false // 是否被撞开
    var knockbackTimer: Float = 0f
    
    enum class AnimState {
        IDLE, WALK, RUN, SPRINT, 
        DRIBBLE, SPRINT_DRIBBLE,  // 盘带
        SHIELD,                   // 护球
        KICK, TACKLE, HEADER,
        CELEBRATE, FALL
    }
    
    /**
     * 更新模型
     */
    fun update(delta: Float) {
        // 更新动画时间
        animTime += delta
        
        // 更新动作冷却
        if (actionCooldown > 0) {
            actionCooldown -= delta
        }
        
        // 更新体力
        updateStamina(delta)
        
        // 更新身体部位位置
        updateBodyParts(delta)
        
        // 更新动画状态
        updateAnimState()
        
        // 应用移动
        applyMovement(delta)
        
        // 应用对抗效果
        applyContactForce(delta)
    }
    
    /**
     * 更新体力
     */
    private fun updateStamina(delta: Float) {
        if (isSprinting && velocity.length() > 1f) {
            stamina -= staminaDrain * delta
        } else {
            stamina += staminaRecover * delta
        }
        stamina = stamina.coerceIn(0f, maxStamina)
        
        // 体力不足时无法冲刺
        if (stamina < 10f) {
            isSprinting = false
        }
    }
    
    /**
     * 更新身体部位位置
     */
    private fun updateBodyParts(delta: Float) {
        val speed = velocity.length()
        val walkCycle = animTime * 6f
        val runCycle = animTime * 10f
        val sprintCycle = animTime * 14f
        
        val cycle = when {
            speed > 6f -> sprintCycle
            speed > 3f -> runCycle
            speed > 0.5f -> walkCycle
            else -> 0f
        }
        
        // 腿部摆动
        val legSwing = sin(cycle) * 0.3f
        val legOffset = Vector3(
            cos(rotation) * legSwing,
            0f,
            sin(rotation) * legSwing
        )
        
        leftFootPos = position + Vector3(
            -cos(rotation + 1.57f) * 0.2f,
            0.05f,
            -sin(rotation + 1.57f) * 0.2f
        ) + legOffset
        
        rightFootPos = position + Vector3(
            cos(rotation + 1.57f) * 0.2f,
            0.05f,
            sin(rotation + 1.57f) * 0.2f
        ) - legOffset
        
        // 身体倾斜（盘带时）
        bodyLean = when {
            animState == AnimState.DRIBBLE -> sin(animTime * 3f) * 0.15f
            animState == AnimState.SPRINT_DRIBBLE -> sin(animTime * 4f) * 0.2f
            speed > 5f -> sin(animTime * 8f) * 0.1f
            else -> 0f
        }
        
        // 手臂摆动
        armSwing = when {
            speed > 5f -> sin(animTime * 10f) * 0.4f
            speed > 2f -> sin(animTime * 6f) * 0.3f
            else -> sin(animTime * 2f) * 0.1f
        }
    }
    
    /**
     * 更新动画状态
     */
    private fun updateAnimState() {
        if (actionCooldown > 0) return
        
        val speed = velocity.length()
        animState = when {
            isKnockedBack -> AnimState.FALL
            isShielding -> AnimState.SHIELD
            hasBall && speed > 6f -> AnimState.SPRINT_DRIBBLE
            hasBall && speed > 1f -> AnimState.DRIBBLE
            speed > 6f -> AnimState.SPRINT
            speed > 3f -> AnimState.RUN
            speed > 0.5f -> AnimState.WALK
            else -> AnimState.IDLE
        }
    }
    
    /**
     * 应用移动
     */
    private fun applyMovement(delta: Float) {
        // 计算实际速度（受体力影响）
        val staminaFactor = 0.5f + (stamina / maxStamina) * 0.5f
        var actualMaxSpeed = maxSpeed * staminaFactor
        
        if (isSprinting && stamina > 10f) {
            actualMaxSpeed *= sprintMultiplier
        }
        
        // 门将出击时更快
        if (isGoalkeeper && isSprinting) {
            actualMaxSpeed *= 1.3f
        }
        
        // 平滑加速
        val speedDiff = targetVelocity.length() - velocity.length()
        val accel = acceleration * delta
        val newSpeed = (velocity.length() + speedDiff.coerceIn(-accel, accel))
            .coerceIn(0f, actualMaxSpeed)
        
        // 保持方向
        val dir = if (targetVelocity.length() > 0.01f) {
            targetVelocity.normalized()
        } else if (velocity.length() > 0.01f) {
            velocity.normalized()
        } else {
            Vector3.FORWARD
        }
        
        velocity = dir * newSpeed
        
        // 应用位置
        position = position + velocity * delta
        position = Vector3(position.x, 0f, position.z)
        
        // 更新朝向
        if (velocity.length() > 0.5f) {
            targetRotation = atan2(velocity.x, velocity.z)
        }
        rotation = lerpAngle(rotation, targetRotation, delta * 10f)
    }
    
    private var targetRotation: Float = 0f
    
    /**
     * 应用接触力（对抗）
     */
    private fun applyContactForce(delta: Float) {
        if (contactForce.length() > 0.01f) {
            position = position + contactForce * delta
            contactForce = contactForce * 0.9f  // 力衰减
        }
        
        if (isKnockedBack) {
            knockbackTimer -= delta
            if (knockbackTimer <= 0) {
                isKnockedBack = false
            }
        }
    }
    
    /**
     * 设置盘带方向
     */
    fun setDribbleDirection(direction: Vector3) {
        val speed = if (isSprinting) maxSpeed * sprintMultiplier else maxSpeed
        targetVelocity = direction.normalized() * speed
    }
    
    /**
     * 爆趟（快速带球突破）
     */
    fun sprintDribble(direction: Vector3) {
        if (stamina > 20f) {
            isSprinting = true
            targetVelocity = direction.normalized() * maxSpeed * sprintMultiplier
            actionCooldown = 0.3f
        }
    }
    
    /**
     * 护球
     */
    fun shieldBall(opponentDirection: Vector3) {
        isShielding = true
        shieldingStrength = 0.8f
        
        // 身体转向对手相反方向
        val awayFromOpponent = -opponentDirection.normalized()
        targetVelocity = awayFromOpponent * 2f  // 慢速移动护球
    }
    
    /**
     * 停止护球
     */
    fun stopShielding() {
        isShielding = false
        shieldingStrength = 0f
    }
    
    /**
     * 执行踢球动作
     */
    fun kickBall(power: Float, direction: Vector3) {
        actionCooldown = 0.4f
        // 踢球动画通过bodyLean和legReach模拟
    }
    
    /**
     * 执行抢断动作
     */
    fun tackle(): Boolean {
        if (actionCooldown > 0) return false
        actionCooldown = 0.5f
        return true
    }
    
    /**
     * 被撞开
     */
    fun knockback(force: Vector3, duration: Float = 0.3f) {
        isKnockedBack = true
        knockbackTimer = duration
        contactForce = force
        hasBall = false
    }
    
    /**
     * 获取控球区域
     */
    fun getBallControlZone(): BoundingCircle {
        val controlRadius = if (hasBall) 0.8f else 1.2f
        return BoundingCircle(
            center = Vector3(position.x, 0f, position.z),
            radius = controlRadius
        )
    }
    
    /**
     * 获取抢断区域
     */
    fun getTackleZone(): BoundingCircle {
        return BoundingCircle(
            center = Vector3(
                position.x + cos(rotation) * legReach * 0.5f,
                0f,
                position.z + sin(rotation) * legReach * 0.5f
            ),
            radius = legReach
        )
    }
    
    /**
     * 重置到初始位置
     */
    fun resetToHome(homePosition: Vector3) {
        position = homePosition
        velocity = Vector3.ZERO
        targetVelocity = Vector3.ZERO
        hasBall = false
        isSprinting = false
        isShielding = false
        isKnockedBack = false
        stamina = maxStamina
        actionCooldown = 0f
    }
}

/**
 * 轴对齐包围盒
 */
data class BoundingBox(
    val min: Vector3,
    val max: Vector3
) {
    fun intersects(other: BoundingBox): Boolean {
        return min.x <= other.max.x && max.x >= other.min.x &&
                min.y <= other.max.y && max.y >= other.min.y &&
                min.z <= other.max.z && max.z >= other.min.z
    }
    
    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
                point.y >= min.y && point.y <= max.y &&
                point.z >= min.z && point.z <= max.z
    }
    
    fun overlap(other: BoundingBox): Vector3? {
        if (!intersects(other)) return null
        
        val overlapX = minOf(max.x, other.max.x) - maxOf(min.x, other.min.x)
        val overlapY = minOf(max.y, other.max.y) - maxOf(min.y, other.min.y)
        val overlapZ = minOf(max.z, other.max.z) - maxOf(min.z, other.min.z)
        
        // 返回最小重叠方向
        return when {
            overlapX <= overlapY && overlapX <= overlapZ -> Vector3(overlapX, 0f, 0f)
            overlapY <= overlapX && overlapY <= overlapZ -> Vector3(0f, overlapY, 0f)
            else -> Vector3(0f, 0f, overlapZ)
        }
    }
}

/**
 * 圆形碰撞体
 */
data class BoundingCircle(
    val center: Vector3,
    val radius: Float
) {
    fun contains(point: Vector3): Boolean {
        val dx = point.x - center.x
        val dz = point.z - center.z
        return dx * dx + dz * dz <= radius * radius
    }
    
    fun intersects(other: BoundingCircle): Boolean {
        val dx = center.x - other.center.x
        val dz = center.z - other.center.z
        val dist = sqrt(dx * dx + dz * dz)
        return dist <= radius + other.radius
    }
    
    fun overlap(other: BoundingCircle): Vector3? {
        val dx = center.x - other.center.x
        val dz = center.z - other.center.z
        val dist = sqrt(dx * dx + dz * dz)
        val minDist = radius + other.radius
        
        if (dist >= minDist) return null
        
        // 返回推开向量
        return if (dist > 0.001f) {
            Vector3(dx / dist, 0f, dz / dist) * (minDist - dist)
        } else {
            Vector3(minDist, 0f, 0f)
        }
    }
}

/**
 * 角度线性插值
 */
private fun lerpAngle(from: Float, to: Float, t: Float): Float {
    var diff = to - from
    while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()
    while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
    return from + diff * t.coerceIn(0f, 1f)
}