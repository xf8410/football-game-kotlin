package com.football.game.data

import com.football.game.model.League
import com.football.game.model.Team

/**
 * 联赛数据库
 * 包含五大联赛和主要球队的完整数据
 */
object LeagueDatabase {

    /**
     * 英超联赛球队
     */
    private val premierLeagueTeams = listOf(
        Team(
            id = "arsenal",
            name = "阿森纳",
            shortName = "ARS",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFEF0107),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 87, midfield = 86, defense = 84, goalkeeper = 84
        ),
        Team(
            id = "manchester_city",
            name = "曼彻斯特城",
            shortName = "MCI",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF6CABDD),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 89, midfield = 88, defense = 86, goalkeeper = 85
        ),
        Team(
            id = "liverpool",
            name = "利物浦",
            shortName = "LIV",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFC8102E),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 86, midfield = 85, defense = 85, goalkeeper = 84
        ),
        Team(
            id = "manchester_united",
            name = "曼彻斯特联",
            shortName = "MUN",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFDA291C),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 84, midfield = 83, defense = 82, goalkeeper = 83
        ),
        Team(
            id = "chelsea",
            name = "切尔西",
            shortName = "CHE",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF034694),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 83, midfield = 84, defense = 83, goalkeeper = 82
        ),
        Team(
            id = "tottenham",
            name = "托特纳姆热刺",
            shortName = "TOT",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF132257),
            attack = 83, midfield = 82, defense = 81, goalkeeper = 82
        ),
        Team(
            id = "newcastle",
            name = "纽卡斯尔联",
            shortName = "NEW",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF241F20),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 82, midfield = 81, defense = 82, goalkeeper = 80
        ),
        Team(
            id = "aston_villa",
            name = "阿斯顿维拉",
            shortName = "AVL",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF670E36),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF95BFE5),
            attack = 81, midfield = 80, defense = 79, goalkeeper = 78
        ),
        Team(
            id = "brighton",
            name = "布莱顿",
            shortName = "BHA",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF0057B8),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 79, midfield = 80, defense = 78, goalkeeper = 77
        ),
        Team(
            id = "west_ham",
            name = "西汉姆联",
            shortName = "WHU",
            league = "英超",
            country = "英格兰",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF7A263A),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF1BB1E7),
            attack = 78, midfield = 79, defense = 78, goalkeeper = 77
        )
    )

    /**
     * 西甲联赛球队
     */
    private val laLigaTeams = listOf(
        Team(
            id = "real_madrid",
            name = "皇家马德里",
            shortName = "RMA",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color.White,
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF1F2D5F),
            attack = 91, midfield = 89, defense = 85, goalkeeper = 86
        ),
        Team(
            id = "barcelona",
            name = "巴塞罗那",
            shortName = "BAR",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFA50044),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF004D98),
            attack = 88, midfield = 87, defense = 83, goalkeeper = 84
        ),
        Team(
            id = "atletico_madrid",
            name = "马德里竞技",
            shortName = "ATM",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFCB3524),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF272E61),
            attack = 84, midfield = 83, defense = 85, goalkeeper = 83
        ),
        Team(
            id = "real_sociedad",
            name = "皇家社会",
            shortName = "RSO",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF003DA5),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 81, midfield = 82, defense = 80, goalkeeper = 79
        ),
        Team(
            id = "villarreal",
            name = "比利亚雷亚尔",
            shortName = "VIL",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFFFDE00),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF005DAA),
            attack = 80, midfield = 81, defense = 79, goalkeeper = 78
        ),
        Team(
            id = "real_betis",
            name = "皇家贝蒂斯",
            shortName = "BET",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF00954C),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 79, midfield = 80, defense = 78, goalkeeper = 77
        ),
        Team(
            id = "athletic_bilbao",
            name = "毕尔巴鄂竞技",
            shortName = "ATH",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFEE2523),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 79, midfield = 79, defense = 80, goalkeeper = 78
        ),
        Team(
            id = "girona",
            name = "赫罗纳",
            shortName = "GIR",
            league = "西甲",
            country = "西班牙",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFCD2534),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 78, midfield = 79, defense = 77, goalkeeper = 76
        )
    )

    /**
     * 德甲联赛球队
     */
    private val bundesligaTeams = listOf(
        Team(
            id = "bayern_munich",
            name = "拜仁慕尼黑",
            shortName = "BAY",
            league = "德甲",
            country = "德国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFDC052D),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 88, midfield = 87, defense = 85, goalkeeper = 86
        ),
        Team(
            id = "dortmund",
            name = "多特蒙德",
            shortName = "BVB",
            league = "德甲",
            country = "德国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFFDE100),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFF000000),
            attack = 85, midfield = 84, defense = 82, goalkeeper = 82
        ),
        Team(
            id = "leverkusen",
            name = "勒沃库森",
            shortName = "LEV",
            league = "德甲",
            country = "德国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFE32221),
            secondaryColor = androidx.compose.ui.graphics.Color.Black,
            attack = 86, midfield = 86, defense = 84, goalkeeper = 83
        ),
        Team(
            id = "leipzig",
            name = "莱比锡",
            shortName = "RBL",
            league = "德甲",
            country = "德国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFDD0741),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 83, midfield = 84, defense = 82, goalkeeper = 81
        ),
        Team(
            id = "frankfurt",
            name = "法兰克福",
            shortName = "SGE",
            league = "德甲",
            country = "德国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFE1000F),
            secondaryColor = androidx.compose.ui.graphics.Color.Black,
            attack = 81, midfield = 80, defense = 79, goalkeeper = 78
        ),
        Team(
            id = "stuttgart",
            name = "斯图加特",
            shortName = "VFB",
            league = "德甲",
            country = "德国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFE32219),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 80, midfield = 79, defense = 78, goalkeeper = 77
        )
    )

    /**
     * 意甲联赛球队
     */
    private val serieATeams = listOf(
        Team(
            id = "inter_milan",
            name = "国际米兰",
            shortName = "INT",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF010E80),
            secondaryColor = androidx.compose.ui.graphics.Color.Black,
            attack = 86, midfield = 85, defense = 86, goalkeeper = 85
        ),
        Team(
            id = "ac_milan",
            name = "AC米兰",
            shortName = "MIL",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFFB090B),
            secondaryColor = androidx.compose.ui.graphics.Color.Black,
            attack = 84, midfield = 83, defense = 83, goalkeeper = 82
        ),
        Team(
            id = "juventus",
            name = "尤文图斯",
            shortName = "JUV",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color.Black,
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 83, midfield = 82, defense = 84, goalkeeper = 83
        ),
        Team(
            id = "napoli",
            name = "那不勒斯",
            shortName = "NAP",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF12A0D7),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 84, midfield = 83, defense = 82, goalkeeper = 81
        ),
        Team(
            id = "roma",
            name = "罗马",
            shortName = "ROM",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF8E1F2F),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFFF0BC42),
            attack = 82, midfield = 81, defense = 80, goalkeeper = 79
        ),
        Team(
            id = "lazio",
            name = "拉齐奥",
            shortName = "LAZ",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF87D8F7),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 81, midfield = 80, defense = 79, goalkeeper = 78
        ),
        Team(
            id = "atalanta",
            name = "亚特兰大",
            shortName = "ATA",
            league = "意甲",
            country = "意大利",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF1E71B8),
            secondaryColor = androidx.compose.ui.graphics.Color.Black,
            attack = 84, midfield = 83, defense = 81, goalkeeper = 80
        )
    )

    /**
     * 法甲联赛球队
     */
    private val ligue1Teams = listOf(
        Team(
            id = "paris_sg",
            name = "巴黎圣日耳曼",
            shortName = "PSG",
            league = "法甲",
            country = "法国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF004170),
            secondaryColor = androidx.compose.ui.graphics.Color(0xFFDA291C),
            attack = 89, midfield = 86, defense = 83, goalkeeper = 84
        ),
        Team(
            id = "marseille",
            name = "马赛",
            shortName = "OM",
            league = "法甲",
            country = "法国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF2FAEE0),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 80, midfield = 79, defense = 78, goalkeeper = 77
        ),
        Team(
            id = "lyon",
            name = "里昂",
            shortName = "OL",
            league = "法甲",
            country = "法国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFF0F4C8A),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 79, midfield = 80, defense = 78, goalkeeper = 77
        ),
        Team(
            id = "monaco",
            name = "摩纳哥",
            shortName = "MON",
            league = "法甲",
            country = "法国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFE7192D),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 81, midfield = 80, defense = 79, goalkeeper = 78
        ),
        Team(
            id = "nice",
            name = "尼斯",
            shortName = "OGC",
            league = "法甲",
            country = "法国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFE8112D),
            secondaryColor = androidx.compose.ui.graphics.Color.Black,
            attack = 78, midfield = 79, defense = 78, goalkeeper = 76
        ),
        Team(
            id = "lille",
            name = "里尔",
            shortName = "LIL",
            league = "法甲",
            country = "法国",
            primaryColor = androidx.compose.ui.graphics.Color(0xFFE4002B),
            secondaryColor = androidx.compose.ui.graphics.Color.White,
            attack = 79, midfield = 80, defense = 79, goalkeeper = 77
        )
    )

    /**
     * 所有联赛
     */
    val ALL_LEAGUES = listOf(
        League(
            id = "premier_league",
            name = "英超",
            country = "英格兰",
            teams = premierLeagueTeams,
            totalRounds = 38
        ),
        League(
            id = "la_liga",
            name = "西甲",
            country = "西班牙",
            teams = laLigaTeams,
            totalRounds = 38
        ),
        League(
            id = "bundesliga",
            name = "德甲",
            country = "德国",
            teams = bundesligaTeams,
            totalRounds = 34
        ),
        League(
            id = "serie_a",
            name = "意甲",
            country = "意大利",
            teams = serieATeams,
            totalRounds = 38
        ),
        League(
            id = "ligue_1",
            name = "法甲",
            country = "法国",
            teams = ligue1Teams,
            totalRounds = 34
        )
    )

    /**
     * 获取所有球队
     */
    fun getAllTeams(): List<Team> {
        return ALL_LEAGUES.flatMap { it.teams }
    }

    /**
     * 根据ID获取球队
     */
    fun getTeam(teamId: String): Team? {
        return getAllTeams().find { it.id == teamId }
    }

    /**
     * 根据联赛名称获取球队
     */
    fun getTeamsByLeague(leagueName: String): List<Team> {
        return ALL_LEAGUES.find { it.name == leagueName }?.teams ?: emptyList()
    }

    /**
     * 获取联赛
     */
    fun getLeague(leagueId: String): League? {
        return ALL_LEAGUES.find { it.id == leagueId }
    }

    /**
     * 搜索球队
     */
    fun searchTeams(query: String): List<Team> {
        val lowerQuery = query.lowercase()
        return getAllTeams().filter {
            it.name.lowercase().contains(lowerQuery) ||
                    it.shortName.lowercase().contains(lowerQuery) ||
                    it.id.lowercase().contains(lowerQuery)
        }
    }
}