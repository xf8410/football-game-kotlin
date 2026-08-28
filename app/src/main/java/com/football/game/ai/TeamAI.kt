package com.football.game.ai

import com.football.game.core.GameState
import com.football.game.core.Vector3

/**
 * 球队战术AI（第二层）
 * 管理球队整体战术：进攻/平衡/防守、高位逼抢/退守、短传/长传/反击
 * 根据比分、时间、球权决定战术模式
 */
class TeamAI {
    /**
     * 战术模式
     */
    enum class TacticMode {
        ATTACKING,     // 进攻：全员压上
        BALANCED,      // 平衡：正常阵型
        DEFENSIVE,     // 防守：退守
        HIGH_PRESS,    // 高位逼抢
        COUNTER,       // 快速反击
        TIME_WASTE     // 拖延时间（领先时）
    }

    /**
     * 进攻策略
     */
    enum class AttackStyle {
        SHORT_PASS,    // 短传渗透
        LONG_PASS,     // 长传冲吊
        WING_PLAY,     // 边路进攻
        COUNTER        // 快速反击
    }

    var currentMode: TacticMode = TacticMode.BALANCED
        private set
    var currentAttackStyle: AttackStyle = AttackStyle.SHORT_PASS
        private set

    /**
     * 更新战术决策
     * @param scoreFor 本方得分
     * @param scoreAgainst 对方得分
     * @param timeRemaining 剩余时间（秒）
     * @param hasPossession 是否有球权
     */
    fun updateTactics(scoreFor: Int, scoreAgainst: Int, timeRemaining: Float, hasPossession: Boolean) {
        val goalDiff = scoreFor - scoreAgainst

        if (hasPossession) {
            // 有球权时的战术
            when {
                goalDiff > 0 && timeRemaining < 60 -> {
                    // 领先且时间不多：拖延时间
                    currentMode = TacticMode.TIME_WASTE
                    currentAttackStyle = AttackStyle.SHORT_PASS
                }
                goalDiff < 0 && timeRemaining < 120 -> {
                    // 落后且时间不多：全力进攻
                    currentMode = TacticMode.ATTACKING
                    currentAttackStyle = AttackStyle.LONG_PASS
                }
                goalDiff < 0 -> {
                    // 落后：进攻
                    currentMode = TacticMode.ATTACKING
                    currentAttackStyle = AttackStyle.WING_PLAY
                }
                else -> {
                    // 平衡
                    currentMode = TacticMode.BALANCED
                    currentAttackStyle = AttackStyle.SHORT_PASS
                }
            }
        } else {
            // 无球权时的战术
            when {
                goalDiff > 0 && timeRemaining < 60 -> {
                    // 领先且时间不多：退守
                    currentMode = TacticMode.DEFENSIVE
                }
                goalDiff <= 0 && timeRemaining < 120 -> {
                    // 落后或平局且时间不多：高位逼抢
                    currentMode = TacticMode.HIGH_PRESS
                }
                else -> {
                    // 平衡
                    currentMode = TacticMode.BALANCED
                }
            }
        }
    }

    /**
     * 应用活动修正（第三层）
     */
    fun applyEventModifiers(eventModifiers: Map<String, Any>) {
        // AI最后几分钟全力防守
        val defensiveMode = eventModifiers["ai_defensive_mode"] as? Boolean ?: false
        if (defensiveMode) {
            currentMode = TacticMode.DEFENSIVE
        }
    }

    /**
     * 获取阵型偏移（根据战术模式调整站位）
     */
    fun getFormationOffset(teamSide: GameState.TeamSide): Vector3 {
        val forward = if (teamSide == GameState.TeamSide.HOME) 1.0f else -1.0f

        return when (currentMode) {
            TacticMode.ATTACKING -> Vector3(0.0f, 0.0f, 8.0f * forward)
            TacticMode.DEFENSIVE -> Vector3(0.0f, 0.0f, -8.0f * forward)
            TacticMode.HIGH_PRESS -> Vector3(0.0f, 0.0f, 15.0f * forward)
            TacticMode.COUNTER -> Vector3(0.0f, 0.0f, -3.0f * forward)
            TacticMode.TIME_WASTE -> Vector3(0.0f, 0.0f, -5.0f * forward)
            TacticMode.BALANCED -> Vector3.ZERO
        }
    }

    /**
     * 获取逼抢强度（0-1）
     */
    fun getPressIntensity(): Float {
        return when (currentMode) {
            TacticMode.HIGH_PRESS -> 1.0f
            TacticMode.ATTACKING -> 0.7f
            TacticMode.BALANCED -> 0.5f
            TacticMode.DEFENSIVE -> 0.3f
            TacticMode.TIME_WASTE -> 0.2f
            TacticMode.COUNTER -> 0.4f
        }
    }

    /**
     * 获取传球偏好（0=短传, 1=长传）
     */
    fun getPassPreference(): Float {
        return when (currentAttackStyle) {
            AttackStyle.SHORT_PASS -> 0.2f
            AttackStyle.LONG_PASS -> 0.8f
            AttackStyle.WING_PLAY -> 0.5f
            AttackStyle.COUNTER -> 0.6f
        }
    }

    /**
     * 获取当前战术名称（用于UI显示）
     */
    fun getTacticName(): String {
        val modeNames = mapOf(
            TacticMode.ATTACKING to "进攻",
            TacticMode.BALANCED to "平衡",
            TacticMode.DEFENSIVE to "防守",
            TacticMode.HIGH_PRESS to "高位逼抢",
            TacticMode.COUNTER to "快速反击",
            TacticMode.TIME_WASTE to "控制节奏"
        )
        val styleNames = mapOf(
            AttackStyle.SHORT_PASS to "短传",
            AttackStyle.LONG_PASS to "长传",
            AttackStyle.WING_PLAY to "边路",
            AttackStyle.COUNTER to "反击"
        )
        return "${modeNames[currentMode]}/${styleNames[currentAttackStyle]}"
    }
}