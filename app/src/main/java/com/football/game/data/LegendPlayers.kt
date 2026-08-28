package com.football.game.data

import com.football.game.model.Player

/**
 * 传奇球星数据库 - 不同时代的足球巨星
 */
object LegendPlayers {

    enum class Era(val displayName: String) {
        MODERN("现代 (2020s)"),
        RECENT("近年 (2010s)"),
        CLASSIC("经典 (2000s)"),
        GOLDEN("黄金年代 (80-90s)"),
        VINTAGE("复古 (60-70s)")
    }

    // 现代巨星
    private val modernEra = listOf(
        Player(id = "mbappe_legend", name = "姆巴佩", number = 10, role = "ST", teamId = "real_madrid", teamName = "皇家马德里", pace = 97, shooting = 95, passing = 83, dribbling = 94, defending = 36, physical = 78),
        Player(id = "haaland_legend", name = "哈兰德", number = 9, role = "ST", teamId = "manchester_city", teamName = "曼城", pace = 89, shooting = 94, passing = 65, dribbling = 80, defending = 45, physical = 90),
        Player(id = "vinicius_legend", name = "维尼修斯", number = 7, role = "LW", teamId = "real_madrid", teamName = "皇家马德里", pace = 96, shooting = 85, passing = 78, dribbling = 95, defending = 30, physical = 70),
        Player(id = "bellingham_legend", name = "贝林厄姆", number = 5, role = "CAM", teamId = "real_madrid", teamName = "皇家马德里", pace = 83, shooting = 87, passing = 84, dribbling = 88, defending = 76, physical = 80),
        Player(id = "yamal_legend", name = "亚马尔", number = 19, role = "RW", teamId = "barcelona", teamName = "巴塞罗那", pace = 93, shooting = 82, passing = 84, dribbling = 93, defending = 25, physical = 58)
    )

    // 近年巨星 (2010s)
    private val recentEra = listOf(
        Player(id = "messi_2012", name = "梅西 (2012)", number = 10, role = "RW", teamId = "barcelona", teamName = "巴塞罗那", pace = 88, shooting = 92, passing = 90, dribbling = 97, defending = 35, physical = 65),
        Player(id = "ronaldo_2014", name = "C罗 (2014)", number = 7, role = "ST", teamId = "real_madrid", teamName = "皇家马德里", pace = 92, shooting = 96, passing = 82, dribbling = 92, defending = 35, physical = 82),
        Player(id = "neymar_2017", name = "内马尔 (2017)", number = 11, role = "LW", teamId = "psg", teamName = "巴黎圣日耳曼", pace = 90, shooting = 88, passing = 86, dribbling = 96, defending = 30, physical = 62),
        Player(id = "modric_2018", name = "莫德里奇 (2018)", number = 10, role = "CM", teamId = "real_madrid", teamName = "皇家马德里", pace = 72, shooting = 80, passing = 90, dribbling = 90, defending = 72, physical = 65),
        Player(id = "iniesta_2012", name = "伊涅斯塔 (2012)", number = 8, role = "CM", teamId = "barcelona", teamName = "巴塞罗那", pace = 68, shooting = 78, passing = 92, dribbling = 94, defending = 62, physical = 58),
        Player(id = "xavi_2010", name = "哈维 (2010)", number = 6, role = "CM", teamId = "barcelona", teamName = "巴塞罗那", pace = 58, shooting = 75, passing = 96, dribbling = 90, defending = 68, physical = 55),
        Player(id = "suarez_2016", name = "苏亚雷斯 (2016)", number = 9, role = "ST", teamId = "barcelona", teamName = "巴塞罗那", pace = 82, shooting = 92, passing = 80, dribbling = 88, defending = 42, physical = 82)
    )

    // 经典巨星 (2000s)
    private val classicEra = listOf(
        Player(id = "ronaldinho_2005", name = "罗纳尔迪尼奥 (2005)", number = 10, role = "CAM", teamId = "barcelona", teamName = "巴塞罗那", pace = 86, shooting = 86, passing = 88, dribbling = 98, defending = 35, physical = 65),
        Player(id = "zidane_2006", name = "齐达内 (2006)", number = 5, role = "CAM", teamId = "real_madrid", teamName = "皇家马德里", pace = 72, shooting = 86, passing = 92, dribbling = 94, defending = 65, physical = 75),
        Player(id = "r9_2002", name = "大罗 (2002)", number = 9, role = "ST", teamId = "real_madrid", teamName = "皇家马德里", pace = 92, shooting = 96, passing = 78, dribbling = 94, defending = 30, physical = 80),
        Player(id = "henry_2004", name = "亨利 (2004)", number = 14, role = "ST", teamId = "arsenal", teamName = "阿森纳", pace = 93, shooting = 90, passing = 84, dribbling = 92, defending = 35, physical = 75),
        Player(id = "kaka_2007", name = "卡卡 (2007)", number = 22, role = "CAM", teamId = "ac_milan", teamName = "AC米兰", pace = 92, shooting = 88, passing = 86, dribbling = 92, defending = 40, physical = 68),
        Player(id = "pirlo_2007", name = "皮尔洛 (2007)", number = 21, role = "CM", teamId = "ac_milan", teamName = "AC米兰", pace = 55, shooting = 82, passing = 94, dribbling = 86, defending = 65, physical = 62),
        Player(id = "gerrard_2005", name = "杰拉德 (2005)", number = 8, role = "CM", teamId = "liverpool", teamName = "利物浦", pace = 78, shooting = 88, passing = 86, dribbling = 84, defending = 72, physical = 82),
        Player(id = "lampard_2005", name = "兰帕德 (2005)", number = 8, role = "CM", teamId = "chelsea", teamName = "切尔西", pace = 72, shooting = 88, passing = 84, dribbling = 80, defending = 65, physical = 80)
    )

    // 黄金年代 (80-90s)
    private val goldenEra = listOf(
        Player(id = "maradona_1986", name = "马拉多纳 (1986)", number = 10, role = "CAM", teamId = "napoli", teamName = "那不勒斯", pace = 88, shooting = 88, passing = 90, dribbling = 98, defending = 35, physical = 68),
        Player(id = "van_basten_1988", name = "范巴斯滕 (1988)", number = 9, role = "ST", teamId = "ac_milan", teamName = "AC米兰", pace = 86, shooting = 94, passing = 80, dribbling = 90, defending = 35, physical = 78),
        Player(id = "gullit_1988", name = "古利特 (1988)", number = 10, role = "CAM", teamId = "ac_milan", teamName = "AC米兰", pace = 82, shooting = 86, passing = 86, dribbling = 90, defending = 72, physical = 85),
        Player(id = "baggio_1994", name = "巴乔 (1994)", number = 10, role = "CF", teamId = "juventus", teamName = "尤文图斯", pace = 82, shooting = 88, passing = 86, dribbling = 94, defending = 35, physical = 65),
        Player(id = "batistuta_1998", name = "巴蒂斯图塔 (1998)", number = 9, role = "ST", teamId = "roma", teamName = "罗马", pace = 82, shooting = 94, passing = 72, dribbling = 82, defending = 35, physical = 85),
        Player(id = "del_piero_1996", name = "皮耶罗 (1996)", number = 10, role = "CF", teamId = "juventus", teamName = "尤文图斯", pace = 82, shooting = 90, passing = 86, dribbling = 92, defending = 35, physical = 68),
        Player(id = "beckham_1999", name = "贝克汉姆 (1999)", number = 7, role = "RW", teamId = "manchester_united", teamName = "曼联", pace = 78, shooting = 88, passing = 94, dribbling = 84, defending = 42, physical = 68),
        Player(id = "raul_2000", name = "劳尔 (2000)", number = 7, role = "CF", teamId = "real_madrid", teamName = "皇家马德里", pace = 84, shooting = 90, passing = 84, dribbling = 90, defending = 35, physical = 70)
    )

    // 复古巨星 (60-70s)
    private val vintageEra = listOf(
        Player(id = "pele_1970", name = "贝利 (1970)", number = 10, role = "CF", teamId = "santos", teamName = "桑托斯", pace = 88, shooting = 94, passing = 88, dribbling = 96, defending = 40, physical = 78),
        Player(id = "beckenbauer_1974", name = "贝肯鲍尔 (1974)", number = 5, role = "CB", teamId = "bayern_munich", teamName = "拜仁慕尼黑", pace = 78, shooting = 75, passing = 90, dribbling = 88, defending = 96, physical = 80),
        Player(id = "garrincha_1962", name = "加林查 (1962)", number = 7, role = "RW", teamId = "boca_juniors", teamName = "博卡青年", pace = 92, shooting = 82, passing = 80, dribbling = 98, defending = 30, physical = 72),
        Player(id = "eusebio_1966", name = "尤西比奥 (1966)", number = 9, role = "ST", teamId = "benfica", teamName = "本菲卡", pace = 90, shooting = 94, passing = 80, dribbling = 92, defending = 35, physical = 78),
        Player(id = "di_stefano_1960", name = "迪·斯蒂法诺 (1960)", number = 9, role = "CF", teamId = "real_madrid", teamName = "皇家马德里", pace = 82, shooting = 92, passing = 88, dribbling = 90, defending = 72, physical = 78)
    )

    val ALL_LEGEND_PLAYERS = modernEra + recentEra + classicEra + goldenEra + vintageEra

    fun getPlayersByEra(era: Era): List<Player> = when (era) {
        Era.MODERN -> modernEra
        Era.RECENT -> recentEra
        Era.CLASSIC -> classicEra
        Era.GOLDEN -> goldenEra
        Era.VINTAGE -> vintageEra
    }

    fun getPlayer(playerId: String): Player? = ALL_LEGEND_PLAYERS.find { it.id == playerId }

    fun getAllEras(): List<Era> = Era.values().toList()
}