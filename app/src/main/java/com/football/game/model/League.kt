package com.football.game.model

/**
 * 联赛数据模型
 */
data class League(
    val id: String,
    val name: String,
    val country: String,
    val teams: List<Team>,
    val currentRound: Int = 0,
    val totalRounds: Int = 0
) {
    /**
     * 积分榜
     */
    data class StandingsEntry(
        val team: Team,
        val played: Int = 0,
        val won: Int = 0,
        val drawn: Int = 0,
        val lost: Int = 0,
        val goalsFor: Int = 0,
        val goalsAgainst: Int = 0,
        val points: Int = won * 3 + drawn,
        val goalDifference: Int = goalsFor - goalsAgainst
    ) : Comparable<StandingsEntry> {
        override fun compareTo(other: StandingsEntry): Int {
            // 先按积分排序
            if (points != other.points) return other.points - points
            // 积分相同按净胜球
            if (goalDifference != other.goalDifference) return other.goalDifference - goalDifference
            // 净胜球相同按进球数
            if (goalsFor != other.goalsFor) return other.goalsFor - goalsFor
            // 都相同按球队名
            return team.name.compareTo(other.team.name)
        }
    }

    /**
     * 赛程表
     */
    data class Schedule(
        val round: Int,
        val matches: List<ScheduledMatch>
    )

    data class ScheduledMatch(
        val homeTeam: Team,
        val awayTeam: Team,
        val isPlayed: Boolean = false,
        val result: MatchResult? = null
    )

    data class MatchResult(
        val homeGoals: Int,
        val awayGoals: Int
    )

    companion object {
        /**
         * 预设联赛
         */
        val PRESET_LEAGUES = listOf(
            League(
                id = "premier_league",
                name = "英超",
                country = "英格兰",
                teams = Team.PRESET_TEAMS.filter { it.league == "英超" },
                totalRounds = 38
            ),
            League(
                id = "la_liga",
                name = "西甲",
                country = "西班牙",
                teams = Team.PRESET_TEAMS.filter { it.league == "西甲" },
                totalRounds = 38
            ),
            League(
                id = "bundesliga",
                name = "德甲",
                country = "德国",
                teams = Team.PRESET_TEAMS.filter { it.league == "德甲" },
                totalRounds = 34
            )
        )
    }
}