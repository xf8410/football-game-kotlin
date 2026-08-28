package com.football.game.core

import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

/**
 * 盘带系统
 * 处理控球、盘带、爆趟、变向
 */
class DribbleSystem {
    
    companion object {
        // 盘带参数
        const val BASE_DRIBBLE_SPEED = 4.5f      // 基础盘带速度
        const val SPRINT_DRIBBLE_SPEED = 7.5f     // 爆趟速度
        const val DRIBBLE_CONTROL_RADIUS = 0.8f   // 盘带控球半径
        const val SPRINT_CONTROL_RADIUS = 1.2f    // 爆趟控球半径
        
        // 变向参数
        const val DIRECTION_CHANGE_SPEED = 5f     // 变向速度
        const val CUT_MOVE_THRESHOLD = 2f         // 切入移动阈值
        
        // 技术动作参数
        const val SKILL_MOVE_COOLDOWN = 0.5f      // 技术动作冷却
        const val SKILL_MOVE_STAMINA_COST = 10f   // 技术动作体力消耗
    }
    
    /**
     * 盘带状态
     */
    data class DribbleState(
        var isDribbling: Boolean = false,
        var isSprintDribbling: Boolean = false,
        var dribbleDirection: Vector3 = Vector3.ZERO,
        var lastDribbleDirection: Vector3 = Vector3.ZERO,
        var dribbleSpeed: Float = 0f,
        var ballControl: Float = 1f,  // 0-1 控球质量
        var timeSinceLastTouch: Float = 0f,
        var touchesCount: Int = 0,
        var isDoingSkillMove: Boolean = false,
        var skillMoveTimer: Float = 0f
    )
    
    /**
     * 技术动作类型
     */
    enum class SkillMove {
        NONE,
        STEP_OVER,      // 踩单车
        BODY_FEINT,     // 身体假动作
        SCISSORS,       // 剪刀脚
        CRUYFF_TURN,    // 克鲁伊夫转身
        HEEL_FLIK,      // 脚后跟磕球
        NUTMEG          // 穿裆
    }
    
    /**
     * 更新盘带
     */
    fun updateDribble(
        player: PlayerModel,
        inputDirection: Vector3,
        isSprinting: Boolean,
        opponents: List<PlayerModel>,
        delta: Float
    ): DribbleState {
        val state = DribbleState()
        
        if (!player.hasBall) {
            return state
        }
        
        state.isDribbling = true
        state.isSprintDribbling = isSprinting
        state.dribbleDirection = inputDirection
        
        // 计算盘带速度
        val baseSpeed = if (isSprinting) {
            SPRINT_DRIBBLE_SPEED
        } else {
            BASE_DRIBBLE_SPEED
        }
        
        // 技术属性影响盘带速度
        val techBonus = 1f  // 未来从球员属性获取
        state.dribbleSpeed = baseSpeed * techBonus
        
        // 检查附近对手，影响控球质量
        state.ballControl = calculateBallControl(player, opponents, isSprinting)
        
        // 更新控球时间
        state.timeSinceLastTouch = player.actionCooldown
        state.touchesCount = player.animTime.toInt() % 10
        
        // 检查是否在做技术动作
        state.isDoingSkillMove = player.actionCooldown > 0
        state.skillMoveTimer = player.actionCooldown
        
        return state
    }
    
    /**
     * 计算控球质量
     */
    private fun calculateBallControl(
        player: PlayerModel,
        opponents: List<PlayerModel>,
        isSprinting: Boolean
    ): Float {
        var control = 1f
        
        // 冲刺时控球质量下降
        if (isSprinting) {
            control *= 0.7f
        }
        
        // 附近对手影响控球
        val nearbyOpponents = opponents.filter { 
            it.position.distanceTo(player.position) < 3f 
        }
        control -= nearbyOpponents.size * 0.1f
        
        // 体力影响控球
        control *= (player.stamina / player.maxStamina * 0.3f + 0.7f)
        
        return control.coerceIn(0.3f, 1f)
    }
    
    /**
     * 执行爆趟
     */
    fun executeSprintDribble(
        player: PlayerModel,
        direction: Vector3
    ): Boolean {
        if (player.stamina < 20f) return false
        if (player.actionCooldown > 0) return false
        
        player.isSprinting = true
        player.sprintDribble(direction)
        player.actionCooldown = 0.3f
        
        return true
    }
    
    /**
     * 执行技术动作
     */
    fun executeSkillMove(
        player: PlayerModel,
        move: SkillMove,
        targetOpponent: PlayerModel?
    ): Boolean {
        if (player.stamina < SKILL_MOVE_STAMINA_COST) return false
        if (player.actionCooldown > 0) return false
        
        player.actionCooldown = SKILL_MOVE_COOLDOWN
        player.stamina -= SKILL_MOVE_STAMINA_COST
        
        when (move) {
            SkillMove.STEP_OVER -> {
                // 踩单车：快速左右晃动
                // 效果：降低对手抢断成功率
            }
            SkillMove.BODY_FEINT -> {
                // 身体假动作：向一侧倾斜然后向另一侧移动
                if (targetOpponent != null) {
                    val feintDir = -targetOpponent.position.directionTo(player.position)
                    player.velocity = feintDir * 3f
                }
            }
            SkillMove.SCISSORS -> {
                // 剪刀脚：腿绕球一圈
                // 效果：迷惑对手，创造空间
            }
            SkillMove.CRUYFF_TURN -> {
                // 克鲁伊夫转身：180度转身
                player.velocity = -player.velocity
                player.rotation += Math.PI.toFloat()
            }
            SkillMove.HEEL_FLIK -> {
                // 脚后跟磕球：向后磕球
                val backDir = -player.velocity.normalized()
                player.velocity = backDir * 5f
            }
            SkillMove.NUTMEG -> {
                // 穿裆：球从对手两腿间穿过
                if (targetOpponent != null) {
                    val nutmegDir = player.position.directionTo(targetOpponent.position)
                    // 球会从对手身下穿过（视觉效果）
                }
            }
            SkillMove.NONE -> {}
        }
        
        return true
    }
    
    /**
     * 计算最佳突破方向
     */
    fun findBestBreakthroughDirection(
        player: PlayerModel,
        opponents: List<PlayerModel>
    ): Vector3 {
        val playerPos = player.position
        
        // 分析各个方向的对手密度
        val directions = listOf(
            Vector3(0f, 0f, 1f),   // 前
            Vector3(0f, 0f, -1f),  // 后
            Vector3(1f, 0f, 0f),   // 右
            Vector3(-1f, 0f, 0f),  // 左
            Vector3(0.7f, 0f, 0.7f),  // 右前
            Vector3(-0.7f, 0f, 0.7f), // 左前
            Vector3(0.7f, 0f, -0.7f), // 右后
            Vector3(-0.7f, 0f, -0.7f) // 左后
        )
        
        var bestDir = Vector3.FORWARD
        var bestScore = -Float.MAX_VALUE
        
        for (dir in directions) {
            val normalizedDir = dir.normalized()
            var score = 0f
            
            // 向前移动得分
            val forwardComponent = normalizedDir.z  // Z轴是前进方向
            score += forwardComponent * 10f
            
            // 计算该方向的对手密度
            for (opponent in opponents) {
                val toOpponent = opponent.position - playerPos
                val dist = toOpponent.length()
                
                if (dist < 5f) {
                    // 对手在路径上的惩罚
                    val dot = normalizedDir.dot(toOpponent.normalized())
                    if (dot > 0.5f) {
                        score -= (5f - dist) * 2f
                    }
                }
            }
            
            // 空间奖励
            val space = calculateSpaceInDirection(playerPos, normalizedDir, opponents)
            score += space * 3f
            
            if (score > bestScore) {
                bestScore = score
                bestDir = normalizedDir
            }
        }
        
        return bestDir
    }
    
    /**
     * 计算某个方向的空间
     */
    private fun calculateSpaceInDirection(
        from: Vector3,
        direction: Vector3,
        opponents: List<PlayerModel>
    ): Float {
        var space = 10f  // 基础空间
        
        for (opponent in opponents) {
            val toOpponent = opponent.position - from
            val dist = toOpponent.length()
            
            if (dist < 8f) {
                val dot = direction.dot(toOpponent.normalized())
                if (dot > 0.3f) {
                    space -= (8f - dist) * dot
                }
            }
        }
        
        return space.coerceIn(0f, 10f)
    }
    
    /**
     * 模拟盘带轨迹
     */
    fun simulateDribblePath(
        player: PlayerModel,
        direction: Vector3,
        duration: Float,
        delta: Float
    ): List<Vector3> {
        val path = mutableListOf<Vector3>()
        var pos = player.position
        var vel = direction * player.maxSpeed
        var time = 0f
        
        while (time < duration) {
            path.add(pos)
            pos = pos + vel * delta
            time += delta
        }
        
        return path
    }
    
    /**
     * 检查是否可以执行技术动作
     */
    fun canPerformSkillMove(
        player: PlayerModel,
        opponents: List<PlayerModel>
    ): List<SkillMove> {
        val availableMoves = mutableListOf<SkillMove>()
        
        if (player.stamina < SKILL_MOVE_STAMINA_COST) return availableMoves
        if (player.actionCooldown > 0) return availableMoves
        
        val nearbyOpponents = opponents.filter { 
            it.position.distanceTo(player.position) < 2f 
        }
        
        if (nearbyOpponents.isNotEmpty()) {
            availableMoves.add(SkillMove.BODY_FEINT)
            availableMoves.add(SkillMove.STEP_OVER)
            
            // 检查是否可以穿裆
            for (opp in nearbyOpponents) {
                val toOpp = opp.position - player.position
                if (toOpp.length() < 1.5f) {
                    availableMoves.add(SkillMove.NUTMEG)
                }
            }
        }
        
        // 检查转身空间
        val behindSpace = calculateSpaceInDirection(
            player.position,
            -player.velocity.normalized(),
            opponents
        )
        if (behindSpace > 3f) {
            availableMoves.add(SkillMove.CRUYFF_TURN)
        }
        
        return availableMoves
    }
}