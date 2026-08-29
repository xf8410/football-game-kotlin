package com.football.game.model

import com.football.game.core.GameState
import com.football.game.core.Vector3

/**
 * 球员数据模型
 */
data class Player(
    val id: String = "",
    val name: String = "",
    val number: Int = 0,
    val role: String = "ST",        // GK, CB, LB, RB, CM, ST, etc.
    val teamSide: GameState.TeamSide = GameState.TeamSide.HOME,
    val teamId: String = "",
    val teamName: String = "",
    val isGoalkeeper: Boolean = false,

    // 属性（0-100）
    val pace: Int = 70,
    val shooting: Int = 70,
    val passing: Int = 70,
    val dribbling: Int = 70,
    val defending: Int = 70,
    val physical: Int = 70,

    // 状态
    var position: Vector3 = Vector3.ZERO,
    var homePosition: Vector3 = Vector3.ZERO,
    var velocity: Vector3 = Vector3.ZERO,
    var inputDirection: Vector3 = Vector3.ZERO,
    var facingDirection: Vector3 = Vector3.FORWARD,
    var isSprinting: Boolean = false,
    var hasBall: Boolean = false,
    var isActive: Boolean = false,
    var isPlayerControlled: Boolean = false,

    // 体力
    var currentStamina: Float = 100.0f,

    // 动画状态
    var animState: AnimState = AnimState.IDLE,
    var actionCooldown: Float = 0.0f,

    // 2过1状态
    var oneTwoPartner: Player? = null,
    var oneTwoTimer: Float = 0.0f,

    // 门将状态
    var gkRushing: Boolean = false,
    var gkRushTarget: Vector3 = Vector3.ZERO,

    // 裁判/铲球系统状态
    var yellowCards: Int = 0,        // 黄牌数（两黄变一红）
    var sentOff: Boolean = false,    // 红牌罚下
    var fallTimer: Float = 0f,       // 被铲倒后的倒地时间（>0 = 倒地）
    var slideTimer: Float = 0f,      // 铲球滑行时间（>0 = 滑铲姿态）
    var tackleCooldown: Float = 0f   // 铲球动作冷却
) {
    /**
     * 动画状态枚举
     * FALL = 被铲倒地（裁判判罚任意球/点球时）
     */
    enum class AnimState {
        IDLE, WALK, RUN, SPRINT, KICK, TACKLE, HEADER, DIVE, CELEBRATE, FALL
    }

    /**
     * 计算游戏内属性（基于数据库属性）
     */
    fun getGameStats(): GameState.PlayerStats {
        return GameState.PlayerStats(
            speed = 5.0f + pace / 10.0f,
            acceleration = 15.0f + pace / 5.0f,
            passPower = 12.0f + passing / 8.0f,
            shotPower = 15.0f + shooting / 6.0f,
            controlRadius = 1.2f + dribbling / 100.0f,
            tackleRadius = 1.5f + defending / 100.0f
        )
    }

    /**
     * 重置到初始位置
     */
    fun resetToHome() {
        position = homePosition
        velocity = Vector3.ZERO
        inputDirection = Vector3.ZERO
        hasBall = false
        currentStamina = getGameStats().stamina
        gkRushing = false
        oneTwoPartner = null
        oneTwoTimer = 0.0f
        actionCooldown = 0.0f
    }

    /**
     * 触发动作动画
     */
    fun playAction(state: AnimState, duration: Float = 0.5f) {
        animState = state
        actionCooldown = duration
    }

    /**
     * 开始2过1配合
     */
    fun startOneTwo(partner: Player) {
        oneTwoPartner = partner
        oneTwoTimer = 3.0f // 3秒内完成2过1
    }

    /**
     * 门将出击
     */
    fun goalkeeperRush(target: Vector3) {
        if (isGoalkeeper) {
            gkRushing = true
            gkRushTarget = target
        }
    }

    /**
     * 获取球员信息
     */
    fun getInfo(): Map<String, Any> {
        return mapOf(
            "team" to teamName,
            "role" to role,
            "number" to number,
            "is_gk" to isGoalkeeper,
            "stamina" to currentStamina,
            "max_stamina" to getGameStats().stamina,
            "position" to position,
            "player_id" to id,
            "name" to name.ifEmpty { "Player $number" }
        )
    }
}
