package com.football.game.core

/**
 * 全局游戏状态管理器
 * 管理比赛配置、当前活动、AI难度等跨场景状态
 */
object GameState {

    // ---- 比赛状态枚举 ----
    enum class MatchPhase {
        KICKOFF,       // 开球
        PLAYING,       // 比赛中
        BALL_OUT,      // 球出界
        GOAL,          // 进球后重置
        HALFTIME,      // 半场休息
        FULLTIME,      // 比赛结束
        PAUSED         // 暂停
    }

    // ---- AI 难度等级 ----
    enum class AIDifficulty {
        EASY,      // 简单：反应慢、传球失误多、不逼抢
        NORMAL,    // 普通：正常反应、偶尔逼抢
        HARD,      // 困难：快速反应、积极逼抢、战术多变
        LEGEND     // 传奇：极快反应、完美站位、高压逼抢
    }

    // ---- 球队枚举 ----
    enum class TeamSide {
        HOME,   // 主队（玩家默认控制）
        AWAY    // 客队
    }

    // ---- 当前比赛配置 ----
    var currentMatchConfig: MutableMap<String, Any> = mutableMapOf(
        "home_team_name" to "红队",
        "away_team_name" to "蓝队",
        "home_team_id" to "home",
        "away_team_id" to "away",
        "formation" to "4-4-2",
        "difficulty" to AIDifficulty.NORMAL,
        "half_duration" to 180.0f,     // 每半场游戏内秒数（3分钟=180秒，全场6分钟）
        "player_controls" to TeamSide.HOME,
        "initial_score" to intArrayOf(0, 0)
    )

    // ---- 当前活动配置 ----
    var currentEvent: MutableMap<String, Any> = mutableMapOf(
        "name" to "快速比赛",
        "type" to "quick_match",
        "modifiers" to mutableMapOf(
            "force_scorer" to null,
            "ai_no_enter_zones" to emptyList<Any>(),
            "ai_defensive_mode" to false,
            "max_shots" to -1,
            "multi_ball" to false,
            "extra_time" to false,
            "penalties" to false
        )
    )

    // ---- 球场尺寸（米，标准 FIFA 尺寸）----
    const val FIELD_LENGTH = 105.0f   // 球场长度
    const val FIELD_WIDTH = 68.0f     // 球场宽度
    const val GOAL_WIDTH = 7.32f      // 球门宽度
    const val GOAL_HEIGHT = 2.44f     // 球门高度
    const val PENALTY_AREA_DEPTH = 16.5f
    const val PENALTY_AREA_WIDTH = 40.3f

    // ---- 阵型定义 ----
    // 坐标系：球场中心为原点，主队球门在 -Z 方向，客队球门在 +Z 方向
    // X = 左右（-34 到 +34），Z = 前后（-52.5 到 +52.5）
    val FORMATIONS: Map<String, List<Triple<String, Float, Float>>> = mapOf(
        "4-4-2" to listOf(
            Triple("GK", 0.0f, -48.0f),
            Triple("LB", -22.0f, -32.0f),
            Triple("CB", -8.0f, -35.0f),
            Triple("CB", 8.0f, -35.0f),
            Triple("RB", 22.0f, -32.0f),
            Triple("LM", -22.0f, -12.0f),
            Triple("CM", -8.0f, -15.0f),
            Triple("CM", 8.0f, -15.0f),
            Triple("RM", 22.0f, -12.0f),
            Triple("ST", -8.0f, 8.0f),
            Triple("ST", 8.0f, 8.0f)
        ),
        "4-3-3" to listOf(
            Triple("GK", 0.0f, -48.0f),
            Triple("LB", -22.0f, -32.0f),
            Triple("CB", -8.0f, -35.0f),
            Triple("CB", 8.0f, -35.0f),
            Triple("RB", 22.0f, -32.0f),
            Triple("CM", -12.0f, -15.0f),
            Triple("CM", 0.0f, -18.0f),
            Triple("CM", 12.0f, -15.0f),
            Triple("LW", -18.0f, 10.0f),
            Triple("ST", 0.0f, 12.0f),
            Triple("RW", 18.0f, 10.0f)
        ),
        "3-5-2" to listOf(
            Triple("GK", 0.0f, -48.0f),
            Triple("CB", -15.0f, -35.0f),
            Triple("CB", 0.0f, -37.0f),
            Triple("CB", 15.0f, -35.0f),
            Triple("LM", -28.0f, -10.0f),
            Triple("CM", -12.0f, -15.0f),
            Triple("CM", 0.0f, -18.0f),
            Triple("CM", 12.0f, -15.0f),
            Triple("RM", 28.0f, -10.0f),
            Triple("ST", -8.0f, 8.0f),
            Triple("ST", 8.0f, 8.0f)
        )
    )

    // ---- AI 难度参数表 ----
    data class AIParams(
        val reactionTime: Float,       // AI反应时间（秒）
        val passAccuracy: Float,       // 传球准确率
        val shotAccuracy: Float,       // 射门准确率
        val chaseSpeedMult: Float,     // 追球速度倍率
        val pressIntensity: Float,     // 逼抢强度
        val tackleSuccess: Float,      // 抢断成功率
        val formationDiscipline: Float // 阵型纪律
    )

    val AI_PARAMS: Map<AIDifficulty, AIParams> = mapOf(
        AIDifficulty.EASY to AIParams(
            reactionTime = 0.6f,
            passAccuracy = 0.65f,
            shotAccuracy = 0.45f,
            chaseSpeedMult = 0.85f,
            pressIntensity = 0.3f,
            tackleSuccess = 0.5f,
            formationDiscipline = 0.5f
        ),
        AIDifficulty.NORMAL to AIParams(
            reactionTime = 0.35f,
            passAccuracy = 0.80f,
            shotAccuracy = 0.65f,
            chaseSpeedMult = 0.95f,
            pressIntensity = 0.6f,
            tackleSuccess = 0.65f,
            formationDiscipline = 0.7f
        ),
        AIDifficulty.HARD to AIParams(
            reactionTime = 0.18f,
            passAccuracy = 0.90f,
            shotAccuracy = 0.78f,
            chaseSpeedMult = 1.0f,
            pressIntensity = 0.85f,
            tackleSuccess = 0.78f,
            formationDiscipline = 0.85f
        ),
        AIDifficulty.LEGEND to AIParams(
            reactionTime = 0.08f,
            passAccuracy = 0.96f,
            shotAccuracy = 0.88f,
            chaseSpeedMult = 1.05f,
            pressIntensity = 1.0f,
            tackleSuccess = 0.88f,
            formationDiscipline = 0.95f
        )
    )

    // ---- 球员基础属性 ----
    data class PlayerStats(
        val speed: Float = 7.0f,          // 最大移动速度（米/秒）
        val acceleration: Float = 25.0f,  // 加速度
        val passPower: Float = 18.0f,     // 传球力度
        val shotPower: Float = 25.0f,     // 射门力度
        val stamina: Float = 100.0f,      // 体力
        val staminaDrain: Float = 2.0f,   // 冲刺时体力消耗/秒
        val staminaRecover: Float = 5.0f, // 非冲刺时体力恢复/秒
        val controlRadius: Float = 1.5f,  // 控球半径
        val tackleRadius: Float = 2.0f    // 抢断半径
    )

    val BASE_PLAYER_STATS = PlayerStats()

    // ---- 物理常量 ----
    const val BALL_RADIUS = 0.11f
    const val PLAYER_RADIUS = 0.4f
    const val BALL_FRICTION = 0.5f
    const val BALL_AIR_DRAG = 0.15f
    const val BALL_GRAVITY = 9.8f
    const val PASS_SPEED = 18.0f
    const val SHOT_SPEED = 28.0f
    const val LOB_SPEED = 12.0f
    const val CROSS_SPEED = 20.0f
    const val LONG_SHOT_SPEED = 32.0f
    const val CAMERA_HEIGHT = 45.0f
    const val CAMERA_DISTANCE = 30.0f
    const val CAMERA_ANGLE = 55.0f

    // ---- 辅助函数 ----

    /**
     * 获取当前AI难度参数
     */
    fun getAIParams(): AIParams {
        val difficulty = currentMatchConfig["difficulty"] as? AIDifficulty ?: AIDifficulty.NORMAL
        return AI_PARAMS[difficulty] ?: AI_PARAMS[AIDifficulty.NORMAL]!!
    }

    /**
     * 获取阵型坐标
     */
    fun getFormation(formationName: String): List<Triple<String, Float, Float>> {
        return FORMATIONS[formationName] ?: FORMATIONS["4-4-2"]!!
    }

    /**
     * 难度枚举转字符串
     */
    fun difficultyToString(d: AIDifficulty): String {
        return when (d) {
            AIDifficulty.EASY -> "简单"
            AIDifficulty.NORMAL -> "普通"
            AIDifficulty.HARD -> "困难"
            AIDifficulty.LEGEND -> "传奇"
        }
    }

    /**
     * 字符串转难度枚举
     */
    fun stringToDifficulty(s: String): AIDifficulty {
        return when (s.lowercase()) {
            "easy" -> AIDifficulty.EASY
            "normal" -> AIDifficulty.NORMAL
            "hard" -> AIDifficulty.HARD
            "legend" -> AIDifficulty.LEGEND
            else -> AIDifficulty.NORMAL
        }
    }
}