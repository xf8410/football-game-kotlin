package com.football.game.core

import kotlin.random.Random

/**
 * 铲球规则判定
 * 依据 IFAB《足球竞赛规则》Law 12（犯规与不正当行为）：
 * - careless（不谨慎）：直接任意球，无需纪律处罚
 * - reckless（不顾对方危险）：必须黄牌警告
 * - excessive force（过度用力 / 危及对方安全）：红牌罚令出场
 * 犯规发生在防守方本方禁区内 → 点球（Law 14）
 */
object TackleRules {

    /** 铲球类型 */
    enum class TackleType(val displayName: String) {
        STANDING("抢断"),
        SLIDE_BALL("铲球"),
        SLIDE_LEG("铲腿"),
        SLIDE_ANKLE("铲脚踝"),
        FLYING("飞铲")
    }

    /** 被铲位置判定：先碰球=干净，铲到腿/脚踝=犯规 */
    enum class ContactKind(val displayName: String) {
        BALL("先碰到球"),
        LEG("铲到腿"),
        ANKLE("铲到脚踝"),
        MISS("未触到")
    }

    /** 严重程度（Law 12 三档） */
    enum class Severity(val displayName: String) {
        CLEAN("干净抢断"),
        CARELESS("不谨慎"),
        RECKLESS("不顾对方危险"),
        EXCESSIVE("过度用力")
    }

    /** 纪律处罚 */
    enum class CardType(val displayName: String) {
        NONE(""), YELLOW("黄牌"), RED("红牌")
    }

    data class Outcome(
        val isFoul: Boolean,
        val ballWon: Boolean,
        val card: CardType,
        val severity: Severity,
        val contact: ContactKind,
        val reason: String
    )

    /**
     * 判定一次铲球的结果
     *
     * @param type        铲球类型（抢断/铲球/铲腿/铲脚踝/飞铲）
     * @param contact     被铲位置判定（先碰球/铲腿/铲脚踝/扑空）
     * @param fromBehind  是否从背后（背后铲人加重处罚）
     * @param shielding   进攻方是否正背身护球（护球更难被抢断；肩部合理冲撞可不吹）
     * @param defending   防守方抢断属性
     * @param dribbling   进攻方盘带属性
     * @param defenderSpeed 防守方当前速度（高速铲球加重）
     * @param rng         随机源
     */
    fun judge(
        type: TackleType,
        contact: ContactKind,
        fromBehind: Boolean,
        shielding: Boolean,
        defending: Int,
        dribbling: Int,
        defenderSpeed: Float,
        rng: Random
    ): Outcome {
        // 未碰到任何东西：无事发生
        if (contact == ContactKind.MISS) {
            return Outcome(false, false, CardType.NONE, Severity.CLEAN, contact, "扑空")
        }

        // 接触点为球时的抢断成功率
        if (contact == ContactKind.BALL) {
            val base = when (type) {
                TackleType.STANDING -> 0.58f
                TackleType.SLIDE_BALL -> 0.72f
                TackleType.SLIDE_LEG -> 0.52f
                TackleType.SLIDE_ANKLE -> 0.46f
                TackleType.FLYING -> 0.42f
            }
            var p = base * (0.75f + (defending - dribbling) / 180f)
            // 盘带护球判定：背身护球时更难被抢断
            if (shielding) p *= 0.6f
            if (rng.nextFloat() < p) {
                return Outcome(false, true, CardType.NONE, Severity.CLEAN, contact, "干净断球")
            }
            // 碰到球但带倒对方：依然构成绊摔犯规
        }

        // 合理冲撞：护球时肩部对肩部（球在可控距离内允许公平冲撞），裁判可放行
        if (shielding && type == TackleType.STANDING && contact == ContactKind.LEG && rng.nextFloat() < 0.45f) {
            return Outcome(false, false, CardType.NONE, Severity.CLEAN, contact, "合理冲撞")
        }

        // 严重程度判定（Law 12 三档）
        val severity = when (type) {
            TackleType.STANDING ->
                if (fromBehind && defenderSpeed > 5f) Severity.RECKLESS else Severity.CARELESS
            TackleType.SLIDE_BALL ->
                if (fromBehind || defenderSpeed > 6.5f) Severity.RECKLESS else Severity.CARELESS
            TackleType.SLIDE_LEG ->
                if (fromBehind || defenderSpeed > 6f) Severity.RECKLESS else Severity.CARELESS
            TackleType.SLIDE_ANKLE ->
                if (fromBehind || defenderSpeed > 5.5f) Severity.EXCESSIVE else Severity.RECKLESS
            TackleType.FLYING ->
                if (fromBehind || defenderSpeed > 6.5f) Severity.EXCESSIVE else Severity.RECKLESS
        }

        val card = when (severity) {
            Severity.CARELESS -> CardType.NONE      // 直接任意球，无牌
            Severity.RECKLESS -> CardType.YELLOW    // 黄牌警告
            else -> CardType.RED                    // 红牌罚下
        }
        return Outcome(true, false, card, severity, contact, "${type.displayName} · ${contact.displayName}")
    }
}
