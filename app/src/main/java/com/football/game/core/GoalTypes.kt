package com.football.game.core

/**
 * 进球方式和术语系统
 */
object GoalTypes {

    enum class GoalMethod(
        val displayName: String,
        val description: String,
        val difficultyBonus: Float,
        val excitementBonus: Float
    ) {
        SHOT_NORMAL("普通射门", "常规射门得分", 1.0f, 0.5f),
        SHOT_POWERFUL("大力抽射", "力量十足的射门", 1.1f, 0.7f),
        SHOT_PLACEMENT("巧射", "角度刁钻的射门", 1.2f, 0.6f),
        SHOT_FAR_POST("远射", "禁区外远射得分", 1.5f, 0.9f),
        SHOT_ONE_TOUCH("一脚射门", "不停球直接射门", 1.3f, 0.7f),
        SHOT_CURLED("搓射", "弧线球射门", 1.4f, 0.9f),
        SHOT_KNUCKLE("电梯球", "不旋转的飘忽射门", 1.6f, 1.0f),
        SHOT_CHIP("吊射", "挑过门将的射门", 1.3f, 0.8f),
        SHOT_BICYCLE("倒钩射门", "腾空倒钩", 2.0f, 1.0f),
        SHOT_HALF_VOLLEY("凌空抽射", "球落地前射门", 1.8f, 1.0f),
        SHOT_VOLLEY("凌空", "球在空中时射门", 1.7f, 0.9f),
        SHOT_BACKHEEL("脚后跟射门", "脚后跟磕球射门", 1.8f, 0.9f),
        HEADER_NORMAL("头球", "常规头球攻门", 1.2f, 0.6f),
        HEADER_POWERFUL("头球冲顶", "力量十足的头球", 1.4f, 0.8f),
        HEADER_DIVING("鱼跃头球", "腾空头球攻门", 1.6f, 0.9f),
        HEADER_REDIRECT("头球蹭射", "改变球方向的头球", 1.3f, 0.7f),
        SHOT_CROSS("传中射门", "接传中球射门得分", 1.2f, 0.7f),
        HEADER_CROSS("传中头球", "接传中球头球攻门", 1.3f, 0.8f),
        SHOT_CUTBACK("倒三角射门", "接倒三角传球射门", 1.2f, 0.7f),
        SHOT_ONE_TWO("二过一射门", "传球后跑位接回传射门", 1.3f, 0.8f),
        REBOUND("补射", "门将扑出后补射得分", 1.0f, 0.6f),
        SCRAMBLE("混战进球", "禁区内混战中进球", 0.8f, 0.5f),
        FREE_KICK_DIRECT("直接任意球", "任意球直接破门", 1.5f, 0.9f),
        CORNER_HEADER("角球头球", "角球传中头球攻门", 1.3f, 0.8f),
        PENALTY_NORMAL("点球", "点球罚进", 1.0f, 0.5f),
        PANENKA("勺子点球", "勺子点球罚进", 1.8f, 0.9f),
        OWN_GOAL("乌龙球", "对方球员自摆乌龙", 0.5f, 0.3f),
        SOLO_GOAL("个人突破进球", "带球突破多人后射门", 1.8f, 1.0f),
        COUNTER_ATTACK("快速反击进球", "反击中快速进攻得分", 1.2f, 0.7f),
    }

    enum class GoalCountTerm(
        val term: String,
        val description: String
    ) {
        FIRST("首开纪录", "打进本场比赛第一球"),
        DOUBLE("梅开二度", "同一球员打进两球"),
        HAT_TRICK("帽子戏法", "同一球员打进三球"),
        FOUR_GOALS("大四喜", "同一球员打进四球"),
        FIVE_GOALS("五子登科", "同一球员打进五球"),
        SIX_GOALS("独中六元", "同一球员打进六球"),
    }
    
    enum class GoalPosition(val term: String) {
        OUTSIDE_BOX("远射破门"),
        INSIDE_BOX("禁区内破门"),
        TOP_CORNER("世界波"),
        BOTTOM_CORNER("死角破门"),
    }
    
    enum class GoalContext(val term: String) {
        EQUALIZER("将比分扳平！"),
        TAKE_LEAD("取得领先！"),
        EXTEND_LEAD("扩大领先优势！"),
        WINNER("比赛制胜球！"),
    }

    fun generateGoalAnnouncement(
        goalMethod: GoalMethod,
        scorerName: String,
        minute: Int,
        goalCount: Int,
        teamName: String,
        opponentName: String,
        currentScore: Pair<Int, Int>,
        goalPosition: GoalPosition? = null,
        goalContext: GoalContext? = null
    ): GoalAnnouncement {
        val countTerm = getGoalCountTerm(goalCount)
        
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
            excitement = goalMethod.excitementBonus,
            goalCount = goalCount
        )
    }
    
    fun getGoalCountTerm(count: Int): GoalCountTerm {
        return when (count) {
            1 -> GoalCountTerm.FIRST
            2 -> GoalCountTerm.DOUBLE
            3 -> GoalCountTerm.HAT_TRICK
            4 -> GoalCountTerm.FOUR_GOALS
            5 -> GoalCountTerm.FIVE_GOALS
            else -> GoalCountTerm.SIX_GOALS
        }
    }
    
    fun detectGoalPosition(ballX: Float, ballZ: Float, goalWidth: Float): GoalPosition {
        val absX = kotlin.math.abs(ballX)
        return when {
            absX > goalWidth * 0.4f -> GoalPosition.TOP_CORNER
            absX > goalWidth * 0.2f -> GoalPosition.BOTTOM_CORNER
            else -> GoalPosition.INSIDE_BOX
        }
    }
    
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
            minute >= 90 && goalDiff == 0 -> GoalContext.EQUALIZER
            teamScore == 0 && opponentScore == 0 -> GoalContext.TAKE_LEAD
            goalDiff > 0 -> GoalContext.EXTEND_LEAD
            goalDiff == 0 -> GoalContext.EQUALIZER
            else -> GoalContext.TAKE_LEAD
        }
    }
}

data class GoalAnnouncement(
    val mainText: String,
    val subText: String,
    val scoreText: String,
    val goalMethod: GoalTypes.GoalMethod,
    val countTerm: GoalTypes.GoalCountTerm,
    val excitement: Float,
    val goalCount: Int
) {
    fun getDisplayDuration(): Float {
        return when {
            goalCount >= 4 -> 5f
            goalCount == 3 -> 4f
            goalCount == 2 -> 3.5f
            excitement > 0.8f -> 3.5f
            else -> 3f
        }
    }
}