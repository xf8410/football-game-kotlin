package com.football.game.model

import com.football.game.core.GameState

/**
 * 比赛数据模型
 */
data class Match(
    val id: String = "",
    val homeTeam: Team,
    val awayTeam: Team,
    val config: MatchConfig = MatchConfig(),

    // 比赛状态
    var phase: GameState.MatchPhase = GameState.MatchPhase.KICKOFF,
    var matchTime: Float = 0.0f,
    var currentHalf: Int = 1,
    var homeScore: Int = 0,
    var awayScore: Int = 0,

    // 进球记录
    val homeScorers: MutableList<GoalRecord> = mutableListOf(),
    val awayScorers: MutableList<GoalRecord> = mutableListOf(),

    // 统计
    val stats: MatchStats = MatchStats()
) {
    /**
     * 比赛配置
     */
    data class MatchConfig(
        val halfDuration: Float = 180.0f,  // 每半场秒数
        val difficulty: GameState.AIDifficulty = GameState.AIDifficulty.NORMAL,
        val isLANMatch: Boolean = false
    )

    /**
     * 进球记录
     */
    data class GoalRecord(
        val playerId: String = "",
        val playerName: String = "",
        val playerNumber: Int = 0,
        val minute: Int = 0,
        val type: String = "普通进球",  // 普通进球、远射、搓射、电梯球、帽子戏法、世界波、乌龙球
        val isOwnGoal: Boolean = false
    )

    /**
     * 比赛统计
     */
    data class MatchStats(
        var shotsHome: Int = 0,
        var shotsAway: Int = 0,
        var shotsOnTargetHome: Int = 0,
        var shotsOnTargetAway: Int = 0,
        var foulsHome: Int = 0,
        var foulsAway: Int = 0,
        var cornersHome: Int = 0,
        var cornersAway: Int = 0,
        var freeKicksHome: Int = 0,
        var freeKicksAway: Int = 0,
        var penaltiesHome: Int = 0,
        var penaltiesAway: Int = 0,
        var yellowCardsHome: Int = 0,
        var yellowCardsAway: Int = 0,
        var redCardsHome: Int = 0,
        var redCardsAway: Int = 0,
        var possessionHome: Float = 50.0f,  // 控球率百分比
        var possessionAway: Float = 50.0f
    )

    /**
     * 当前比赛时间（分钟）
     */
    fun getCurrentMinute(): Int {
        val halfDuration = config.halfDuration
        val minute = (matchTime / (halfDuration / 45.0f)).toInt()
        return if (currentHalf == 2) minute + 45 else minute
    }

    /**
     * 添加进球
     */
    fun addGoal(scoringTeam: GameState.TeamSide, goal: GoalRecord) {
        when (scoringTeam) {
            GameState.TeamSide.HOME -> {
                homeScore++
                homeScorers.add(goal)
            }
            GameState.TeamSide.AWAY -> {
                awayScore++
                awayScorers.add(goal)
            }
        }
    }

    /**
     * 记录射门
     */
    fun recordShot(team: GameState.TeamSide, onTarget: Boolean) {
        when (team) {
            GameState.TeamSide.HOME -> {
                stats.shotsHome++
                if (onTarget) stats.shotsOnTargetHome++
            }
            GameState.TeamSide.AWAY -> {
                stats.shotsAway++
                if (onTarget) stats.shotsOnTargetAway++
            }
        }
    }

    /**
     * 记录犯规
     */
    fun recordFoul(team: GameState.TeamSide) {
        when (team) {
            GameState.TeamSide.HOME -> stats.foulsHome++
            GameState.TeamSide.AWAY -> stats.foulsAway++
        }
    }

    /**
     * 记录角球
     */
    fun recordCorner(team: GameState.TeamSide) {
        when (team) {
            GameState.TeamSide.HOME -> stats.cornersHome++
            GameState.TeamSide.AWAY -> stats.cornersAway++
        }
    }

    /**
     * 判断比赛是否结束
     */
    fun isFinished(): Boolean {
        return phase == GameState.MatchPhase.FULLTIME
    }

    /**
     * 获取比赛结果描述
     */
    fun getResultDescription(): String {
        return when {
            homeScore > awayScore -> "${homeTeam.name} 获胜"
            homeScore < awayScore -> "${awayTeam.name} 获胜"
            else -> "平局"
        }
    }
}