package com.football.game.data

import com.football.game.model.Player
import com.football.game.model.Team

/**
 * 球员数据库
 * 提供预设的球员和球队数据
 */
object PlayerDatabase {

    /**
     * 获取所有球员
     */
    fun getAllPlayers(): List<Player> {
        return players
    }

    /**
     * 根据ID获取球员
     */
    fun getPlayer(playerId: String): Player? {
        return players.find { it.id == playerId }
    }

    /**
     * 根据ID获取球员姓名
     */
    fun getPlayerName(playerId: String): String {
        return getPlayer(playerId)?.name ?: "未知球员"
    }

    /**
     * 获取球员短姓名（姓）
     */
    fun getPlayerShortName(playerId: String): String {
        val name = getPlayerName(playerId)
        return name.split(" ").lastOrNull() ?: name
    }

    /**
     * 获取球队所有球员
     */
    fun getTeamPlayers(teamId: String): List<Player> {
        return players.filter { it.teamId == teamId }
    }

    /**
     * 获取球队信息
     */
    fun getTeam(teamId: String): Team? {
        return Team.PRESET_TEAMS.find { it.id == teamId }
    }

    /**
     * 获取所有球队
     */
    fun getAllTeams(): List<Team> {
        return Team.PRESET_TEAMS
    }

    // ==================== 预设球员数据 ====================

    private val players = listOf(
        // 皇家马德里
        Player(
            id = "courtois",
            name = "库尔图瓦",
            number = 1,
            role = "GK",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            isGoalkeeper = true,
            pace = 50,
            shooting = 15,
            passing = 30,
            dribbling = 25,
            defending = 20,
            physical = 80
        ),
        Player(
            id = "carvajal",
            name = "卡瓦哈尔",
            number = 2,
            role = "RB",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 82,
            shooting = 65,
            passing = 78,
            dribbling = 80,
            defending = 80,
            physical = 72
        ),
        Player(
            id = "militao",
            name = "米利唐",
            number = 3,
            role = "CB",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 82,
            shooting = 40,
            passing = 55,
            dribbling = 60,
            defending = 85,
            physical = 82
        ),
        Player(
            id = "alaba",
            name = "阿拉巴",
            number = 4,
            role = "CB",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 76,
            shooting = 68,
            passing = 80,
            dribbling = 78,
            defending = 82,
            physical = 72
        ),
        Player(
            id = "mendy",
            name = "门迪",
            number = 23,
            role = "LB",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 90,
            shooting = 55,
            passing = 72,
            dribbling = 78,
            defending = 82,
            physical = 80
        ),
        Player(
            id = "modric",
            name = "莫德里奇",
            number = 10,
            role = "CM",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 72,
            shooting = 78,
            passing = 90,
            dribbling = 90,
            defending = 72,
            physical = 65
        ),
        Player(
            id = "toni_kroos",
            name = "克罗斯",
            number = 8,
            role = "CM",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 60,
            shooting = 82,
            passing = 92,
            dribbling = 85,
            defending = 70,
            physical = 68
        ),
        Player(
            id = "valverde",
            name = "巴尔韦德",
            number = 15,
            role = "CM",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 86,
            shooting = 80,
            passing = 80,
            dribbling = 82,
            defending = 78,
            physical = 82
        ),
        Player(
            id = "vinicius",
            name = "维尼修斯",
            number = 7,
            role = "LW",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 95,
            shooting = 82,
            passing = 75,
            dribbling = 92,
            defending = 30,
            physical = 68
        ),
        Player(
            id = "bellingham",
            name = "贝林厄姆",
            number = 5,
            role = "CAM",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 82,
            shooting = 85,
            passing = 82,
            dribbling = 85,
            defending = 75,
            physical = 78
        ),
        Player(
            id = "rodrygo",
            name = "罗德里戈",
            number = 11,
            role = "RW",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 88,
            shooting = 82,
            passing = 78,
            dribbling = 88,
            defending = 35,
            physical = 65
        ),
        Player(
            id = "mbappe",
            name = "姆巴佩",
            number = 9,
            role = "ST",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 97,
            shooting = 92,
            passing = 80,
            dribbling = 92,
            defending = 35,
            physical = 78
        ),

        // 巴塞罗那
        Player(
            id = "ter_stegen",
            name = "特尔施特根",
            number = 1,
            role = "GK",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            isGoalkeeper = true,
            pace = 55,
            shooting = 15,
            passing = 35,
            dribbling = 30,
            defending = 20,
            physical = 78
        ),
        Player(
            id = "kounde",
            name = "孔德",
            number = 23,
            role = "RB",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 82,
            shooting = 55,
            passing = 75,
            dribbling = 78,
            defending = 85,
            physical = 75
        ),
        Player(
            id = "araujo",
            name = "阿劳霍",
            number = 4,
            role = "CB",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 80,
            shooting = 45,
            passing = 60,
            dribbling = 55,
            defending = 88,
            physical = 85
        ),
        Player(
            id = "christensen",
            name = "克里斯滕森",
            number = 15,
            role = "CB",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 68,
            shooting = 40,
            passing = 65,
            dribbling = 60,
            defending = 84,
            physical = 78
        ),
        Player(
            id = "balde",
            name = "巴尔德",
            number = 28,
            role = "LB",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 92,
            shooting = 55,
            passing = 72,
            dribbling = 82,
            defending = 72,
            physical = 65
        ),
        Player(
            id = "pedri",
            name = "佩德里",
            number = 8,
            role = "CM",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 78,
            shooting = 75,
            passing = 88,
            dribbling = 90,
            defending = 72,
            physical = 65
        ),
        Player(
            id = "gavi",
            name = "加维",
            number = 6,
            role = "CM",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 80,
            shooting = 72,
            passing = 82,
            dribbling = 85,
            defending = 75,
            physical = 72
        ),
        Player(
            id = "de_jong",
            name = "德容",
            number = 21,
            role = "CM",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 82,
            shooting = 72,
            passing = 85,
            dribbling = 88,
            defending = 75,
            physical = 78
        ),
        Player(
            id = "yamal",
            name = "亚马尔",
            number = 19,
            role = "RW",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 90,
            shooting = 78,
            passing = 80,
            dribbling = 90,
            defending = 25,
            physical = 60
        ),
        Player(
            id = "raphinha",
            name = "拉菲尼亚",
            number = 11,
            role = "LW",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 85,
            shooting = 82,
            passing = 80,
            dribbling = 85,
            defending = 45,
            physical = 70
        ),
        Player(
            id = "lewandowski",
            name = "莱万多夫斯基",
            number = 9,
            role = "ST",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 72,
            shooting = 92,
            passing = 75,
            dribbling = 82,
            defending = 40,
            physical = 82
        ),
        Player(
            id = "ferran",
            name = "费兰·托雷斯",
            number = 7,
            role = "ST",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 88,
            shooting = 82,
            passing = 72,
            dribbling = 82,
            defending = 30,
            physical = 68
        )
    )
}