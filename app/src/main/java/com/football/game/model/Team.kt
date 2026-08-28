package com.football.game.model

import androidx.compose.ui.graphics.Color

/**
 * 球队数据模型
 */
data class Team(
    val id: String,
    val name: String,
    val shortName: String = name.take(3).uppercase(),
    val league: String = "",
    val country: String = "",
    val formation: String = "4-4-2",

    // 球队颜色
    val primaryColor: Color = Color.Red,
    val secondaryColor: Color = Color.White,

    // 球员列表
    val playerIds: List<String> = emptyList(),

    // 球队属性
    val attack: Int = 70,
    val midfield: Int = 70,
    val defense: Int = 70,
    val goalkeeper: Int = 70,

    // 统计
    var gamesPlayed: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0
) {
    /**
     * 积分
     */
    val points: Int get() = wins * 3 + draws

    /**
     * 净胜球
     */
    val goalDifference: Int get() = goalsFor - goalsAgainst

    /**
     * 球队总评分
     */
    val overallRating: Int get() = (attack + midfield + defense + goalkeeper) / 4

    /**
     * 记录比赛结果
     */
    fun recordResult(goalsFor: Int, goalsAgainst: Int) {
        gamesPlayed++
        this.goalsFor += goalsFor
        this.goalsAgainst += goalsAgainst
        when {
            goalsFor > goalsAgainst -> wins++
            goalsFor == goalsAgainst -> draws++
            else -> losses++
        }
    }

    companion object {
        /**
         * 预设球队
         */
        val PRESET_TEAMS = listOf(
            Team(
                id = "real_madrid",
                name = "皇家马德里",
                shortName = "RMA",
                league = "西甲",
                country = "西班牙",
                primaryColor = Color.White,
                secondaryColor = Color(0xFF1F2D5F), // 深蓝
                attack = 88,
                midfield = 87,
                defense = 85,
                goalkeeper = 86
            ),
            Team(
                id = "barcelona",
                name = "巴塞罗那",
                shortName = "BAR",
                league = "西甲",
                country = "西班牙",
                primaryColor = Color(0xFFA50044), // 红蓝
                secondaryColor = Color(0xFF004D98),
                attack = 86,
                midfield = 88,
                defense = 82,
                goalkeeper = 84
            ),
            Team(
                id = "manchester_city",
                name = "曼彻斯特城",
                shortName = "MCI",
                league = "英超",
                country = "英格兰",
                primaryColor = Color(0xFF6CABDD), // 天蓝
                secondaryColor = Color.White,
                attack = 89,
                midfield = 88,
                defense = 86,
                goalkeeper = 85
            ),
            Team(
                id = "liverpool",
                name = "利物浦",
                shortName = "LIV",
                league = "英超",
                country = "英格兰",
                primaryColor = Color(0xFFC8102E), // 红
                secondaryColor = Color.White,
                attack = 85,
                midfield = 84,
                defense = 85,
                goalkeeper = 84
            ),
            Team(
                id = "bayern_munich",
                name = "拜仁慕尼黑",
                shortName = "BAY",
                league = "德甲",
                country = "德国",
                primaryColor = Color(0xFFDC052D), // 红
                secondaryColor = Color.White,
                attack = 87,
                midfield = 86,
                defense = 84,
                goalkeeper = 85
            )
        )
    }
}