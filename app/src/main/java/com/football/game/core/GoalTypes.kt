package com.football.game.core

/**
 * 进球方式和术语系统
 * 包含所有进球类型、播报术语、庆祝动作
 */
object GoalTypes {

    // ==================== 进球方式 ====================
    enum class GoalMethod(
        val displayName: String,
        val description: String,
        val difficultyBonus: Float,  // 难度加成（用于评分）
        val excitementBonus: Float   // 精彩程度
    ) {
        // 射门类
        SHOT_NORMAL("普通射门", "常规射门得分", 1.0f, 0.5f),
        SHOT_POWERFUL("大力抽射", "力量十足的射门", 1.1f, 0.7f),
        SHOT_PLACEMENT("巧射", "角度刁钻的射门", 1.2f, 0.6f),
        SHOT_FAR_POST("远射", "禁区外远射得分", 1.5f, 0.9f),
        SHOT_ONE_TOUCH("一脚射门", "不停球直接射门", 1.3f, 0.7f),
        
        // 特殊射门
        SHOT_CURLED("搓射", "弧线球射门", 1.4f, 0.9f),
        SHOT_KNUCKLE("电梯球", "不旋转的飘忽射门", 1.6f, 1.0f),
        SHOT_CHIP("吊射", "挑过门将的射门", 1.3f, 0.8f),
        SHOT_TRIVELA("外脚背", "外脚背射门", 1.5f, 0.9f),
        SHOT_BICYCLE("倒钩射门", "腾空倒钩", 2.0f, 1.0f),
        SHOT_HALF_VOLLEY("凌空抽射", "球落地前射门", 1.8f, 1.0f),
        SHOT_VOLLEY("凌空", "球在空中时射门", 1.7f, 0.9f),
        SHOT_OVERHEAD("倒挂金钩", "身体后仰腾空射门", 2.2f, 1.0f),
        SHOT_BACKHEEL("脚后跟射门", "脚后跟磕球射门", 1.8f, 0.9f),
        
        // 头球类
        HEADER_NORMAL("头球", "常规头球攻门", 1.2f, 0.6f),
        HEADER_POWERFUL("头球冲顶", "力量十足的头球", 1.4f, 0.8f),
        HEADER_DIVING("鱼跃头球", "腾空头球攻门", 1.6f, 0.9f),
        HEADER_REDIRECT("头球蹭射", "改变球方向的头球", 1.3f, 0.7f),
        HEADER_GLANCING("头球甩头", "甩头攻门", 1.4f, 0.7f),
        
        // 配合类
        SHOT_CROSS("传中射门", "接传中球射门得分", 1.2f, 0.7f),
        HEADER_CROSS("传中头球", "接传中球头球攻门", 1.3f, 0.8f),
        SHOT_CUTBACK("倒三角射门", "接倒三角传球射门", 1.2f, 0.7f),
        SHOT_LAYOFF("做球射门", "接队友做球射门", 1.1f, 0.6f),
        SHOT_GIVE_AND_GO("二过一射门", "撞墙配合后射门", 1.3f, 0.8f),
        SHOT_ONE_TWO("二过一射门", "传球后跑位接回传射门", 1.3f, 0.8f),
        
        // 补射类
        REBOUND("补射", "门将扑出后补射得分", 1.0f, 0.6f),
        REBOUND_HEADER("补射头球", "头球补射得分", 1.1f, 0.7f),
        SCRAMBLE("混战进球", "禁区内混战中进球", 0.8f, 0.5f),
        
        // 任意球
        FREE_KICK_DIRECT("直接任意球", "任意球直接破门", 1.5f, 0.9f),
        FREE_KICK_INDIRECT("任意球配合", "任意球配合后射门", 1.3f, 0.8f),
        
        // 角球
        CORNER_HEADER("角球头球", "角球传中头球攻门", 1.3f, 0.8f),
        CORNER_SHOT("角球直接射门", "角球直接旋进球门", 2.0f, 1.0f),
        
        // 点球
        PENALTY_NORMAL("点球", "点球罚进", 1.0f, 0.5f),
        PENALTY_CHIP("点球吊射", "点球吊射罚进", 1.5f, 0.8f),
        PANENKA("勺子点球", "勺子点球罚进", 1.8f, 0.9f),
        
        // 乌龙球
        OWN_GOAL("乌龙球", "对方球员自摆乌龙", 0.5f, 0.3f),
        
        // 其他
        LONG_SHOT("远距离射门", "超远距离射门得分", 1.8f, 1.0f),
        SOLO_GOAL("个人突破进球", "带球突破多人后射门", 1.8f, 1.0f),
        COUNTER_ATTACK("快速反击进球", "反击中快速进攻得分", 1.2f, 0.7f),
        SLOW_BUILD_UP("阵地战进球", "耐心组织后的进球", 1.0f, 0.6f),
    }

    // ==================== 进球术语 ====================
    
    /**
     * 单场进球术语
     */
    enum class GoalCountTerm(
        val count: Int,
        val term: String,
        val description: String
    ) {
        FIRST(1, "首开纪录", "打进本场比赛第一球"),
        DOUBLE(2, "梅开二度", "同一球员打进两球"),
        HAT_TRICK(3, "帽子戏法", "同一球员打进三球"),
        FOUR_GOALS(4, "大四喜", "同一球员打进四球"),
        FIVE_GOALS(5, "五子登科", "同一球员打进五球"),
        SIX_GOALS(6, "独中六元", "同一球员打进六球"),
        SEVEN_PLUS(7, "超级大四喜+", "同一球员打进七球以上"),
    }
    
    /**
     * 进球位置术语
     */
    enum class GoalPosition(
        val position: String,
        val term: String
    ) {
        OUTSIDE_BOX("禁区外", "远射破门"),
        INSIDE_BOX("禁区内", "禁区内破门"),
        PENALTY_SPOT("点球点", "点球破门"),
        SIX_YARD("六码区", "近距离破门"),
        FAR_POST("远门柱", "后点破门"),
        NEAR_POST("近门柱", "前点破门"),
        TOP_CORNER("球门死角", "世界波"),
        BOTTOM_CORNER("球门下角", "死角破门"),
    }
    
    /**
     * 进球时间术语
     */
    enum class GoalTiming(
        val minuteRange: IntRange,
        val term: String
    ) {
        EARLY(1..15, "闪击破门"),
        OPENING(16..30, "早早取得领先"),
        BEFORE_HALF(31..45, "半场前破门"),
        INJURY_TIME_FIRST(45..45, "上半场伤停补时破门"),
        SECOND_HALF_START(46..60, "下半场开局破门"),
        CRITICAL(61..75, "关键时刻破门"),
        LATE(76..90, "终场前破门"),
        INJURY_TIME(91..99, "绝杀！"),
        EXTRA_TIME(101..120, "加时赛进球"),
    }
    
    /**
     * 比赛场景术语
     */
    enum class GoalContext(
        val context: String,
        val term: String
    ) {
        EQUALIZER("扳平比分", "将比分扳平！"),
        TAKE_LEAD("取得领先", "取得领先！"),
        EXTEND_LEAD("扩大领先", "扩大领先优势！"),
        WINNER("绝杀！", "比赛制胜球！"),
        INSURANCE("锁定胜局", "锁定胜局！"),
        CONSOLATION("挽回颜面", "打入安慰球"),
        COMEBACK("逆转比分", "逆转比分！"),
        EQUALIZER_LATE("终场前扳平", "终场前扳平比分！"),
    }
    
    /**
     * 连续进球术语（同一名球员）
     */
    enum class ConsecutiveGoals(
        val count: Int,
        val term: String
    ) {
        TWO_IN_TWO("两场进球", "连续两场比赛进球"),
        THREE_IN_THREE("三场进球", "连续三场比赛进球"),
        FIVE_IN_FIVE("五场进球", "连续五场比赛进球"),
        TEN_PLUS("十场连续进球", "连续十场以上进球"),
    }

    // ==================== 进球播报 ====================
    
    /**
     * 生成进球播报文本
     */
    fun generateGoalAnnouncement(
        goalMethod: GoalMethod,
        scorerName: String,
        minute: Int,
        goalCount: Int,  // 该球员本场进球数
        teamName: String,
        opponentName: String,
        currentScore: Pair<Int, Int>,  // (主队进球, 客队进球)
        goalPosition: GoalPosition? = null,
        goalContext: GoalContext? = null
    ): GoalAnnouncement {
        
        // 确定进球术语
        val countTerm = getGoalCountTerm(goalCount)
        
        // 确定精彩程度
        val excitement = calculateExcitement(goalMethod, goalPosition, goalContext)
        
        // 生成播报文本
        val mainText = buildString {
            append("⚽ 进球！")
            append(scorerName)
            append(" $minute'")
            
            if (goalCount > 1) {
                append("\n")
                append(countTerm.term)
            }
        }
        
        val subText = buildString {
            append(goalMethod.displayName)
            if (goalPosition != null) {
                append(" · ${goalPosition.term}")
            }
            if (goalContext != null) {
                append("\n")
                append(goalContext.term)
            }
        }
        
        val scoreText = "$teamName ${currentScore.first} - ${currentScore.second} $opponentName"
        
        return GoalAnnouncement(
            mainText = mainText,
            subText = subText,
            scoreText = scoreText,
            goalMethod = goalMethod,
            countTerm = countTerm,
            excitement = excitement,
            goalCount = goalCount
        )
    }
    
    /**
     * 获取进球计数术语
     */
    fun getGoalCountTerm(count: Int): GoalCountTerm {
        return when (count) {
            1 -> GoalCountTerm.FIRST
            2 -> GoalCountTerm.DOUBLE
            3 -> GoalCountTerm.HAT_TRICK
            4 -> GoalCountTerm.FOUR_GOALS
            5 -> GoalCountTerm.FIVE_GOALS
            6 -> GoalCountTerm.SIX_GOALS
            else -> GoalCountTerm.SEVEN_PLUS
        }
    }
    
    /**
     * 计算精彩程度
     */
    private fun calculateExcitement(
        method: GoalMethod,
        position: GoalPosition?,
        context: GoalContext?
    ): Float {
        var excitement = method.excitementBonus
        
        if (position == GoalPosition.TOP_CORNER || position == GoalPosition.BOTTOM_CORNER) {
            excitement += 0.2f
        }
        
        if (context == GoalContext.WINNER || context == GoalContext.EQUALIZER_LATE) {
            excitement += 0.3f
        }
        
        return excitement.coerceIn(0f, 1f)
    }
    
    /**
     * 检测进球方式
     */
    fun detectGoalMethod(
        ballHeight: Float,
        distanceToGoal: Float,
        hasHeader: Boolean,
        isCurled: Boolean,
        isKnuckle: Boolean,
        isChip: Boolean,
        isFromCross: Boolean,
        isRebound: Boolean,
        isFreeKick: Boolean,
        isCorner: Boolean,
        isPenalty: Boolean,
        ballSpeed: Float,
        playerCount: Int
    ): GoalMethod {
        // 点球
        if (isPenalty) {
            return GoalMethod.PENALTY_NORMAL
        }
        
        // 任意球
        if (isFreeKick) {
            return GoalMethod.FREE_KICK_DIRECT
        }
        
        // 角球
        if (isCorner) {
            return GoalMethod.CORNER_HEADER
        }
        
        // 头球
        if (hasHeader) {
            return when {
                ballHeight > 1.5f -> GoalMethod.HEADER_DIVING
                ballSpeed > 8f -> GoalMethod.HEADER_POWERFUL
                else -> GoalMethod.HEADER_NORMAL
            }
        }
        
        // 补射
        if (isRebound) {
            return GoalMethod.REBOUND
        }
        
        // 传中
        if (isFromCross) {
            return GoalMethod.SHOT_CROSS
        }
        
        // 特殊射门
        if (isCurled) return GoalMethod.SHOT_CURLED
        if (isKnuckle) return GoalMethod.SHOT_KNUCKLE
        if (isChip) return GoalMethod.SHOT_CHIP
        
        // 远射
        if (distanceToGoal > 25f) {
            return when {
                ballSpeed > 30f -> GoalMethod.SHOT_POWERFUL
                isCurled -> GoalMethod.SHOT_CURLED
                else -> GoalMethod.SHOT_FAR_POST
            }
        }
        
        // 普通射门
        return when {
            ballSpeed > 25f -> GoalMethod.SHOT_POWERFUL
            ballSpeed < 15f -> GoalMethod.SHOT_PLACEMENT
            else -> GoalMethod.SHOT_NORMAL
        }
    }
    
    /**
     * 检测进球位置
     */
    fun detectGoalPosition(
        ballX: Float,
        ballZ: Float,
        goalWidth: Float
    ): GoalPosition {
        val halfGoal = goalWidth / 2
        val absX = kotlin.math.abs(ballX)
        
        return when {
            absX > halfGoal * 0.8f -> GoalPosition.TOP_CORNER
            absX > halfGoal * 0.5f -> GoalPosition.BOTTOM_CORNER
            ballZ < -45f -> GoalPosition.FAR_POST
            ballZ > 45f -> GoalPosition.NEAR_POST
            absX < 3f -> GoalPosition.SIX_YARD
            else -> GoalPosition.INSIDE_BOX
        }
    }
    
    /**
     * 检测比赛场景
     */
    fun detectGoalContext(
        teamScore: Int,
        opponentScore: Int,
        minute: Int,
        isExtraTime: Boolean,
        isSecondHalf: Boolean
    ): GoalContext {
        val goalDiff = teamScore - opponentScore
        
        return when {
            minute >= 90 && goalDiff == 1 -> GoalContext.WINNER
            minute >= 90 && goalDiff == 0 -> GoalContext.EQUALIZER_LATE
            isExtraTime -> GoalContext.WINNER
            teamScore == 0 && opponentScore == 0 -> GoalContext.TAKE_LEAD
            goalDiff > 0 -> GoalContext.EXTEND_LEAD
            goalDiff == 0 -> GoalContext.EQUALIZER
            teamScore == 1 && opponentScore > 1 -> GoalContext.CONSOLATION
            else -> GoalContext.TAKE_LEAD
        }
    }
}

/**
 * 进球播报数据类
 */
data class GoalAnnouncement(
    val mainText: String,
    val subText: String,
    val scoreText: String,
    val goalMethod: GoalTypes.GoalMethod,
    val countTerm: GoalTypes.GoalCountTerm,
    val excitement: Float,
    val goalCount: Int
) {
    /**
     * 获取播报颜色
     */
    fun getAnnouncementColor(): Long {
        return when {
            goalCount >= 4 -> 0xFFFFD700  // 金色（大四喜以上）
            goalCount == 3 -> 0xFFFF6B6B  // 红色（帽子戏法）
            goalCount == 2 -> 0xFF4ECDC4  // 青色（梅开二度）
            excitement > 0.8f -> 0xFFFF6B6B  // 精彩进球
            else -> 0xFFFFFFFF  // 白色
        }
    }
    
    /**
     * 获取播报持续时间
     */
    fun getDisplayDuration(): Float {
        return when {
            goalCount >= 4 -> 5f    // 大四喜以上显示5秒
            goalCount == 3 -> 4f    // 帽子戏法显示4秒
            goalCount == 2 -> 3.5f  // 梅开二度显示3.5秒
            excitement > 0.8f -> 3.5f  // 精彩进球显示3.5秒
            else -> 3f              // 普通进球显示3秒
        }
    }
}