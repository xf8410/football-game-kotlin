package com.football.game.data

import com.football.game.model.Player

/**
 * 著名球员数据库
 * 包含五大联赛知名球员的详细数据
 */
object FamousPlayers {

    /**
     * 英超著名球员
     */
    private val premierLeaguePlayers = listOf(
        // 阿森纳
        Player(
            id = "saka_arsenal",
            name = "布卡约·萨卡",
            number = 7,
            role = "RW",
            teamId = "arsenal",
            teamName = "阿森纳",
            pace = 88, shooting = 82, passing = 83, dribbling = 87, defending = 42, physical = 65
        ),
        Player(
            id = "rice_arsenal",
            name = "德克兰·赖斯",
            number = 41,
            role = "CDM",
            teamId = "arsenal",
            teamName = "阿森纳",
            pace = 75, shooting = 72, passing = 78, dribbling = 79, defending = 86, physical = 82
        ),
        Player(
            id = "odegaard_arsenal",
            name = "马丁·厄德高",
            number = 8,
            role = "CAM",
            teamId = "arsenal",
            teamName = "阿森纳",
            pace = 76, shooting = 80, passing = 88, dribbling = 88, defending = 55, physical = 58
        ),
        Player(
            id = "saliba_arsenal",
            name = "威廉·萨利巴",
            number = 2,
            role = "CB",
            teamId = "arsenal",
            teamName = "阿森纳",
            pace = 82, shooting = 40, passing = 65, dribbling = 68, defending = 87, physical = 80
        ),

        // 曼城
        Player(
            id = "haaland_mancity",
            name = "埃尔林·哈兰德",
            number = 9,
            role = "ST",
            teamId = "manchester_city",
            teamName = "曼彻斯特城",
            pace = 89, shooting = 93, passing = 65, dribbling = 80, defending = 45, physical = 88
        ),
        Player(
            id = "foden_mancity",
            name = "菲尔·福登",
            number = 47,
            role = "CAM",
            teamId = "manchester_city",
            teamName = "曼彻斯特城",
            pace = 84, shooting = 82, passing = 84, dribbling = 90, defending = 48, physical = 58
        ),
        Player(
            id = "de_bruyne_mancity",
            name = "凯文·德布劳内",
            number = 17,
            role = "CM",
            teamId = "manchester_city",
            teamName = "曼彻斯特城",
            pace = 76, shooting = 86, passing = 93, dribbling = 88, defending = 60, physical = 72
        ),
        Player(
            id = "rodri_mancity",
            name = "罗德里",
            number = 16,
            role = "CDM",
            teamId = "manchester_city",
            teamName = "曼彻斯特城",
            pace = 68, shooting = 75, passing = 84, dribbling = 82, defending = 87, physical = 84
        ),

        // 利物浦
        Player(
            id = "salah_liverpool",
            name = "穆罕默德·萨拉赫",
            number = 11,
            role = "RW",
            teamId = "liverpool",
            teamName = "利物浦",
            pace = 89, shooting = 89, passing = 82, dribbling = 90, defending = 45, physical = 68
        ),
        Player(
            id = "nunez_liverpool",
            name = "达尔文·努涅斯",
            number = 9,
            role = "ST",
            teamId = "liverpool",
            teamName = "利物浦",
            pace = 93, shooting = 84, passing = 68, dribbling = 82, defending = 35, physical = 80
        ),
        Player(
            id = "mac_allister_liverpool",
            name = "亚历克西斯·麦卡利斯特",
            number = 10,
            role = "CM",
            teamId = "liverpool",
            teamName = "利物浦",
            pace = 72, shooting = 78, passing = 85, dribbling = 84, defending = 75, physical = 70
        ),

        // 曼联
        Player(
            id = "rashford_manu",
            name = "马库斯·拉什福德",
            number = 10,
            role = "LW",
            teamId = "manchester_united",
            teamName = "曼彻斯特联",
            pace = 93, shooting = 82, passing = 76, dribbling = 85, defending = 35, physical = 72
        ),
        Player(
            id = "fernandes_manu",
            name = "布鲁诺·费尔南德斯",
            number = 8,
            role = "CAM",
            teamId = "manchester_united",
            teamName = "曼彻斯特联",
            pace = 74, shooting = 84, passing = 89, dribbling = 83, defending = 58, physical = 68
        ),
        Player(
            id = "garnacho_manu",
            name = "亚历杭德罗·加纳乔",
            number = 17,
            role = "LW",
            teamId = "manchester_united",
            teamName = "曼彻斯特联",
            pace = 90, shooting = 78, passing = 72, dribbling = 86, defending = 30, physical = 62
        ),

        // 切尔西
        Player(
            id = "palmer_chelsea",
            name = "科尔·帕尔默",
            number = 20,
            role = "CAM",
            teamId = "chelsea",
            teamName = "切尔西",
            pace = 78, shooting = 84, passing = 84, dribbling = 86, defending = 42, physical = 60
        ),
        Player(
            id = "jackson_chelsea",
            name = "尼古拉斯·杰克逊",
            number = 15,
            role = "ST",
            teamId = "chelsea",
            teamName = "切尔西",
            pace = 90, shooting = 78, passing = 70, dribbling = 80, defending = 32, physical = 74
        ),

        // 热刺
        Player(
            id = "son_tottenham",
            name = "孙兴慜",
            number = 7,
            role = "ST",
            teamId = "tottenham",
            teamName = "托特纳姆热刺",
            pace = 88, shooting = 87, passing = 80, dribbling = 86, defending = 42, physical = 68
        ),
        Player(
            id = "maddison_tottenham",
            name = "詹姆斯·麦迪逊",
            number = 10,
            role = "CAM",
            teamId = "tottenham",
            teamName = "托特纳姆热刺",
            pace = 72, shooting = 80, passing = 86, dribbling = 84, defending = 52, physical = 60
        )
    )

    /**
     * 西甲著名球员
     */
    private val laLigaPlayers = listOf(
        // 皇家马德里
        Player(
            id = "mbappe_real",
            name = "基利安·姆巴佩",
            number = 9,
            role = "ST",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 97, shooting = 94, passing = 82, dribbling = 93, defending = 36, physical = 78
        ),
        Player(
            id = "vinicius_real",
            name = "维尼修斯·儒尼奥尔",
            number = 7,
            role = "LW",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 95, shooting = 84, passing = 78, dribbling = 94, defending = 30, physical = 70
        ),
        Player(
            id = "bellingham_real",
            name = "朱德·贝林厄姆",
            number = 5,
            role = "CAM",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 83, shooting = 86, passing = 83, dribbling = 87, defending = 76, physical = 80
        ),
        Player(
            id = "rodrygo_real",
            name = "罗德里戈",
            number = 11,
            role = "RW",
            teamId = "real_madrid",
            teamName = "皇家马德里",
            pace = 88, shooting = 83, passing = 79, dribbling = 90, defending = 32, physical = 62
        ),

        // 巴塞罗那
        Player(
            id = "yamal_barca",
            name = "拉明·亚马尔",
            number = 19,
            role = "RW",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 92, shooting = 80, passing = 82, dribbling = 92, defending = 25, physical = 58
        ),
        Player(
            id = "lewandowski_barca",
            name = "罗伯特·莱万多夫斯基",
            number = 9,
            role = "ST",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 72, shooting = 92, passing = 76, dribbling = 84, defending = 42, physical = 82
        ),
        Player(
            id = "pedri_barca",
            name = "佩德里",
            number = 8,
            role = "CM",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 78, shooting = 76, passing = 88, dribbling = 90, defending = 72, physical = 65
        ),
        Player(
            id = "gavi_barca",
            name = "加维",
            number = 6,
            role = "CM",
            teamId = "barcelona",
            teamName = "巴塞罗那",
            pace = 82, shooting = 74, passing = 82, dribbling = 86, defending = 76, physical = 74
        ),

        // 马德里竞技
        Player(
            id = "griezmann_atm",
            name = "安托万·格里兹曼",
            number = 7,
            role = "CF",
            teamId = "atletico_madrid",
            teamName = "马德里竞技",
            pace = 80, shooting = 84, passing = 82, dribbling = 86, defending = 52, physical = 68
        ),
        Player(
            id = "alvarez_atm",
            name = "胡利安·阿尔瓦雷斯",
            number = 19,
            role = "ST",
            teamId = "atletico_madrid",
            teamName = "马德里竞技",
            pace = 84, shooting = 84, passing = 78, dribbling = 84, defending = 48, physical = 72
        )
    )

    /**
     * 德甲著名球员
     */
    private val bundesligaPlayers = listOf(
        // 拜仁慕尼黑
        Player(
            id = "kane_bayern",
            name = "哈里·凯恩",
            number = 9,
            role = "ST",
            teamId = "bayern_munich",
            teamName = "拜仁慕尼黑",
            pace = 72, shooting = 93, passing = 82, dribbling = 84, defending = 45, physical = 82
        ),
        Player(
            id = "musiala_bayern",
            name = "贾马尔·穆西亚拉",
            number = 42,
            role = "CAM",
            teamId = "bayern_munich",
            teamName = "拜仁慕尼黑",
            pace = 82, shooting = 78, passing = 84, dribbling = 92, defending = 45, physical = 58
        ),
        Player(
            id = "sane_bayern",
            name = "勒罗伊·萨内",
            number = 10,
            role = "RW",
            teamId = "bayern_munich",
            teamName = "拜仁慕尼黑",
            pace = 93, shooting = 82, passing = 80, dribbling = 88, defending = 35, physical = 62
        ),
        Player(
            id = "olise_bayern",
            name = "迈克尔·奥利塞",
            number = 7,
            role = "RW",
            teamId = "bayern_munich",
            teamName = "拜仁慕尼黑",
            pace = 86, shooting = 80, passing = 82, dribbling = 86, defending = 38, physical = 65
        ),

        // 多特蒙德
        Player(
            id = "adeyemi_bvb",
            name = "卡里姆·阿德耶米",
            number = 27,
            role = "RW",
            teamId = "dortmund",
            teamName = "多特蒙德",
            pace = 96, shooting = 78, passing = 72, dribbling = 84, defending = 28, physical = 65
        ),
        Player(
            id = "brandt_bvb",
            name = "朱利安·布兰特",
            number = 10,
            role = "CAM",
            teamId = "dortmund",
            teamName = "多特蒙德",
            pace = 76, shooting = 78, passing = 84, dribbling = 84, defending = 52, physical = 62
        ),

        // 勒沃库森
        Player(
            id = "wirtz_lev",
            name = "弗洛里安·维尔茨",
            number = 10,
            role = "CAM",
            teamId = "leverkusen",
            teamName = "勒沃库森",
            pace = 82, shooting = 82, passing = 86, dribbling = 90, defending = 42, physical = 58
        ),
        Player(
            id = "boniface_lev",
            name = "维克托·博尼法斯",
            number = 22,
            role = "ST",
            teamId = "leverkusen",
            teamName = "勒沃库森",
            pace = 86, shooting = 84, passing = 72, dribbling = 82, defending = 35, physical = 80
        )
    )

    /**
     * 意甲著名球员
     */
    private val serieAPlayers = listOf(
        // 国际米兰
        Player(
            id = "lautaro_inter",
            name = "劳塔罗·马丁内斯",
            number = 10,
            role = "ST",
            teamId = "inter_milan",
            teamName = "国际米兰",
            pace = 85, shooting = 87, passing = 78, dribbling = 86, defending = 42, physical = 76
        ),
        Player(
            id = "thuram_inter",
            name = "马库斯·图拉姆",
            number = 9,
            role = "ST",
            teamId = "inter_milan",
            teamName = "国际米兰",
            pace = 88, shooting = 82, passing = 74, dribbling = 82, defending = 38, physical = 78
        ),
        Player(
            id = "barella_inter",
            name = "尼科洛·巴雷拉",
            number = 23,
            role = "CM",
            teamId = "inter_milan",
            teamName = "国际米兰",
            pace = 82, shooting = 76, passing = 82, dribbling = 84, defending = 78, physical = 74
        ),

        // AC米兰
        Player(
            id = "leao_milan",
            name = "拉斐尔·莱奥",
            number = 10,
            role = "LW",
            teamId = "ac_milan",
            teamName = "AC米兰",
            pace = 93, shooting = 82, passing = 76, dribbling = 90, defending = 28, physical = 70
        ),
        Player(
            id = "pulisic_milan",
            name = "克里斯蒂安·普利西奇",
            number = 11,
            role = "RW",
            teamId = "ac_milan",
            teamName = "AC米兰",
            pace = 86, shooting = 80, passing = 78, dribbling = 86, defending = 42, physical = 62
        ),

        // 尤文图斯
        Player(
            id = "vlahovic_juve",
            name = "杜尚·弗拉霍维奇",
            number = 9,
            role = "ST",
            teamId = "juventus",
            teamName = "尤文图斯",
            pace = 84, shooting = 87, passing = 70, dribbling = 82, defending = 38, physical = 78
        ),
        Player(
            id = "chiesa_juve",
            name = "费德里科·基耶萨",
            number = 7,
            role = "RW",
            teamId = "juventus",
            teamName = "尤文图斯",
            pace = 90, shooting = 82, passing = 76, dribbling = 86, defending = 35, physical = 68
        ),

        // 那不勒斯
        Player(
            id = "kvara_napoli",
            name = "赫维查·克瓦拉茨赫利亚",
            number = 77,
            role = "LW",
            teamId = "napoli",
            teamName = "那不勒斯",
            pace = 88, shooting = 82, passing = 80, dribbling = 90, defending = 32, physical = 65
        ),
        Player(
            id = "osimhen_napoli",
            name = "维克托·奥西梅恩",
            number = 9,
            role = "ST",
            teamId = "napoli",
            teamName = "那不勒斯",
            pace = 93, shooting = 87, passing = 65, dribbling = 82, defending = 35, physical = 82
        )
    )

    /**
     * 法甲著名球员
     */
    private val ligue1Players = listOf(
        // 巴黎圣日耳曼
        Player(
            id = "dembele_psg",
            name = "奥斯曼·登贝莱",
            number = 10,
            role = "RW",
            teamId = "paris_sg",
            teamName = "巴黎圣日耳曼",
            pace = 93, shooting = 78, passing = 80, dribbling = 92, defending = 30, physical = 60
        ),
        Player(
            id = "barcola_psg",
            name = "布拉德利·巴尔科拉",
            number = 29,
            role = "LW",
            teamId = "paris_sg",
            teamName = "巴黎圣日耳曼",
            pace = 94, shooting = 78, passing = 74, dribbling = 88, defending = 28, physical = 62
        ),
        Player(
            id = "asensio_psg",
            name = "马尔科·阿森西奥",
            number = 11,
            role = "CAM",
            teamId = "paris_sg",
            teamName = "巴黎圣日耳曼",
            pace = 78, shooting = 82, passing = 82, dribbling = 84, defending = 42, physical = 60
        ),
        Player(
            id = "zair_emery_psg",
            name = "扎伊尔·埃梅里",
            number = 33,
            role = "CM",
            teamId = "paris_sg",
            teamName = "巴黎圣日耳曼",
            pace = 78, shooting = 72, passing = 80, dribbling = 82, defending = 72, physical = 68
        ),

        // 摩纳哥
        Player(
            id = "ben_yedder_monaco",
            name = "维萨姆·本耶德尔",
            number = 9,
            role = "ST",
            teamId = "monaco",
            teamName = "摩纳哥",
            pace = 82, shooting = 86, passing = 74, dribbling = 84, defending = 38, physical = 70
        ),
        Player(
            id = "golovin_monaco",
            name = "亚历山大·戈洛温",
            number = 17,
            role = "CAM",
            teamId = "monaco",
            teamName = "摩纳哥",
            pace = 78, shooting = 78, passing = 80, dribbling = 82, defending = 58, physical = 65
        )
    )

    /**
     * 所有著名球员
     */
    val ALL_FAMOUS_PLAYERS = premierLeaguePlayers + laLigaPlayers + bundesligaPlayers + serieAPlayers + ligue1Players

    /**
     * 根据ID获取球员
     */
    fun getPlayer(playerId: String): Player? {
        return ALL_FAMOUS_PLAYERS.find { it.id == playerId }
    }

    /**
     * 根据球队ID获取球员
     */
    fun getPlayersByTeam(teamId: String): List<Player> {
        return ALL_FAMOUS_PLAYERS.filter { it.teamId == teamId }
    }

    /**
     * 搜索球员
     */
    fun searchPlayers(query: String): List<Player> {
        val lowerQuery = query.lowercase()
        return ALL_FAMOUS_PLAYERS.filter {
            it.name.lowercase().contains(lowerQuery) ||
                    it.id.lowercase().contains(lowerQuery)
        }
    }
}