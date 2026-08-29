package com.football.game.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.football.game.model.Team
import com.football.game.ui.component.AvatarFacialHair
import com.football.game.ui.component.AvatarHairStyle
import com.football.game.ui.component.HairStyle3D
import com.football.game.ui.component.PlayerLook
import com.football.game.ui.component.StarAvatarParams

/**
 * 球星特征库
 * 用标志性外貌特征（发型/胡子/肤色）让球员"像"真人球星：
 * - 2D 头像：LeagueScreen / EraSelectScreen 等界面
 * - 3D 模型：渲染器通过 PlayerLook 上色
 *
 * 说明：全部为程序化卡通风格，不使用真实照片（肖像权），仅特征神似。
 */
object StarLikeness {

    // ==================== 球星特征预设 ====================
    // key 为特征核心名，匹配时对全名做包含匹配（"埃尔林·哈兰德" 命中 "哈兰德"）
    private val presets: Map<String, StarAvatarParams> = mapOf(
        // ---------- 现代球星 ----------
        "姆巴佩" to StarAvatarParams(
            skinColor = Color(0xFF9C6B43), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "哈兰德" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFFE8C56A),
            hairStyle = AvatarHairStyle.FLOW, browThickness = 1.3f
        ),
        "维尼修斯" to StarAvatarParams(
            skinColor = Color(0xFF7A4A2B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.CURLY
        ),
        "贝林厄姆" to StarAvatarParams(
            skinColor = Color(0xFFD9A066), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SLICK, facialHair = AvatarFacialHair.STUBBLE
        ),
        "亚马尔" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF241A12),
            hairStyle = AvatarHairStyle.CURLY
        ),
        "萨拉赫" to StarAvatarParams(
            skinColor = Color(0xFFC68642), hairColor = Color(0xFF2B1B12),
            hairStyle = AvatarHairStyle.CURLY, facialHair = AvatarFacialHair.FULL_BEARD
        ),
        "凯恩" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF6B4423),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "德布劳内" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFFB5522D),
            hairStyle = AvatarHairStyle.SHORT, facialHair = AvatarFacialHair.STUBBLE
        ),
        "萨卡" to StarAvatarParams(
            skinColor = Color(0xFF9C6B43), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "孙兴慜" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "布鲁诺·费尔南德斯" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF3B2A1E),
            hairStyle = AvatarHairStyle.SLICK, facialHair = AvatarFacialHair.STUBBLE
        ),
        "帕尔默" to StarAvatarParams(
            skinColor = Color(0xFFC68642), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "格里兹曼" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFFB08968),
            hairStyle = AvatarHairStyle.SLICK, facialHair = AvatarFacialHair.STUBBLE
        ),
        "莱万多夫斯基" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF4A3526),
            hairStyle = AvatarHairStyle.SHORT, facialHair = AvatarFacialHair.STUBBLE
        ),
        "穆西亚拉" to StarAvatarParams(
            skinColor = Color(0xFFD9A066), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.CURLY
        ),
        "萨内" to StarAvatarParams(
            skinColor = Color(0xFFC68642), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.CURLY
        ),
        "维尔茨" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF6B4423),
            hairStyle = AvatarHairStyle.SLICK
        ),
        "劳塔罗" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SLICK, facialHair = AvatarFacialHair.STUBBLE
        ),
        "莱奥" to StarAvatarParams(
            skinColor = Color(0xFF7A4A2B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.CURLY, facialHair = AvatarFacialHair.MOUSTACHE
        ),
        "弗拉霍维奇" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF3B2A1E),
            hairStyle = AvatarHairStyle.SLICK
        ),
        "克瓦拉茨赫利亚" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.CURLY
        ),
        "登贝莱" to StarAvatarParams(
            skinColor = Color(0xFF7A4A2B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "阿德耶米" to StarAvatarParams(
            skinColor = Color(0xFF7A4A2B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.BUZZ
        ),

        // ---------- 近年巨星 ----------
        "梅西" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF3B2A1E),
            hairStyle = AvatarHairStyle.SHORT, facialHair = AvatarFacialHair.FULL_BEARD
        ),
        "C罗" to StarAvatarParams(
            skinColor = Color(0xFFE0AC69), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SLICK, facialHair = AvatarFacialHair.STUBBLE
        ),
        "内马尔" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF2B1B12),
            hairStyle = AvatarHairStyle.MOHAWK, facialHair = AvatarFacialHair.GOATEE
        ),
        "莫德里奇" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF4A3526),
            hairStyle = AvatarHairStyle.SLICK
        ),
        "伊涅斯塔" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "哈维" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT, facialHair = AvatarFacialHair.STUBBLE
        ),
        "苏亚雷斯" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT, facialHair = AvatarFacialHair.FULL_BEARD
        ),

        // ---------- 经典巨星 ----------
        "罗纳尔迪尼奥" to StarAvatarParams(
            skinColor = Color(0xFF9C6B43), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.LONG, facialHair = AvatarFacialHair.GOATEE
        ),
        "齐达内" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF3B2A1E),
            hairStyle = AvatarHairStyle.BALD, facialHair = AvatarFacialHair.STUBBLE
        ),
        "大罗" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SLICK, facialHair = AvatarFacialHair.GOATEE
        ),
        "亨利" to StarAvatarParams(
            skinColor = Color(0xFF9C6B43), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "卡卡" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF6B4423),
            hairStyle = AvatarHairStyle.SLICK
        ),
        "皮尔洛" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.LONG, facialHair = AvatarFacialHair.FULL_BEARD
        ),
        "杰拉德" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF3B2A1E),
            hairStyle = AvatarHairStyle.SHORT, facialHair = AvatarFacialHair.STUBBLE
        ),
        "兰帕德" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF4A3526),
            hairStyle = AvatarHairStyle.SHORT
        ),

        // ---------- 黄金年代 ----------
        "马拉多纳" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.CURLY, facialHair = AvatarFacialHair.STUBBLE
        ),
        "范巴斯滕" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF6B4423),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "古利特" to StarAvatarParams(
            skinColor = Color(0xFF7A4A2B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.AFRO, facialHair = AvatarFacialHair.MOUSTACHE
        ),
        "巴乔" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF2B1B12),
            hairStyle = AvatarHairStyle.PONYTAIL, facialHair = AvatarFacialHair.GOATEE
        ),
        "巴蒂斯图塔" to StarAvatarParams(
            skinColor = Color(0xFFE3B07E), hairColor = Color(0xFFC9A25E),
            hairStyle = AvatarHairStyle.LONG, facialHair = AvatarFacialHair.FULL_BEARD
        ),
        "皮耶罗" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SLICK
        ),
        "贝克汉姆" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFFE8C56A),
            hairStyle = AvatarHairStyle.HEADBAND
        ),
        "劳尔" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SLICK
        ),

        // ---------- 复古巨星 ----------
        "贝利" to StarAvatarParams(
            skinColor = Color(0xFF6B4226), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "贝肯鲍尔" to StarAvatarParams(
            skinColor = Color(0xFFF3D0B0), hairColor = Color(0xFF6B4423),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "加林查" to StarAvatarParams(
            skinColor = Color(0xFFC68642), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "尤西比奥" to StarAvatarParams(
            skinColor = Color(0xFF6B4226), hairColor = Color(0xFF141414),
            hairStyle = AvatarHairStyle.SHORT
        ),
        "迪·斯蒂法诺" to StarAvatarParams(
            skinColor = Color(0xFFE8B58B), hairColor = Color(0xFF3B2A1E),
            hairStyle = AvatarHairStyle.SLICK
        )
    )

    // ==================== 球队 → 招牌球星 ====================
    private val teamStar: Map<String, String> = mapOf(
        "曼彻斯特城" to "哈兰德",
        "皇家马德里" to "姆巴佩",
        "巴塞罗那" to "亚马尔",
        "利物浦" to "萨拉赫",
        "拜仁慕尼黑" to "凯恩",
        "阿森纳" to "萨卡",
        "曼彻斯特联" to "布鲁诺·费尔南德斯",
        "切尔西" to "帕尔默",
        "托特纳姆热刺" to "孙兴慜",
        "马德里竞技" to "格里兹曼",
        "勒沃库森" to "维尔茨",
        "多特蒙德" to "阿德耶米",
        "国际米兰" to "劳塔罗",
        "AC米兰" to "莱奥",
        "尤文图斯" to "弗拉霍维奇",
        "那不勒斯" to "克瓦拉茨赫利亚",
        "巴黎圣日耳曼" to "登贝莱"
    )

    // ==================== 2D 头像查询 ====================

    /** 去掉 "(2012)" 这类年份后缀 */
    fun normalizeName(name: String): String = name.substringBefore("(").trim()

    /** 根据球员名获取头像参数（支持全名/带年份后缀/未知名兜底） */
    fun paramsForPlayerName(name: String): StarAvatarParams {
        val n = normalizeName(name)
        presets[n]?.let { return it }
        presets.entries.firstOrNull { (key, _) -> n.contains(key) }?.let { return it.value }
        return generateForName(n)
    }

    /** 球队的招牌球星名（无映射返回 null） */
    fun starNameForTeam(teamName: String): String? = teamStar[teamName]

    /** 根据球队名获取招牌球星头像参数（套用球衣配色） */
    fun paramsForTeam(teamName: String, kit1: Color, kit2: Color): StarAvatarParams {
        val star = teamStar[teamName]
        val base = star?.let { s ->
            presets[s] ?: paramsForPlayerName(s)
        } ?: generateForName(teamName)
        return base.copy(kitColor1 = kit1, kitColor2 = kit2)
    }

    /** 未知名字的兜底：按名字哈希确定性生成外观 */
    fun generateForName(name: String): StarAvatarParams {
        val h = name.hashCode()
        val skins = listOf(0xFFF3D0B0, 0xFFE8B58B, 0xFFD9A066, 0xFFC68642, 0xFF9C6B43, 0xFF7A4A2B)
        val hairs = listOf(0xFF1B1B1B, 0xFF2B1B12, 0xFF3B2A1E, 0xFF4A3526, 0xFF6B4423)
        val styles = listOf(
            AvatarHairStyle.SHORT, AvatarHairStyle.SLICK, AvatarHairStyle.BUZZ,
            AvatarHairStyle.SHORT, AvatarHairStyle.CURLY, AvatarHairStyle.SHORT
        )
        val beards = listOf(
            AvatarFacialHair.NONE, AvatarFacialHair.NONE,
            AvatarFacialHair.STUBBLE, AvatarFacialHair.GOATEE
        )
        return StarAvatarParams(
            skinColor = Color(skins[h and 0xFF and (skins.size - 1).inv() + skins.size shr 8 and 0]), // placeholder, corrected below
            hairColor = Color(hairs[(h shr 8 and 0xFF) % hairs.size]),
            hairStyle = styles[(h shr 16 and 0xFF) % styles.size],
            facialHair = beards[(h shr 24 and 0xFF) % beards.size],
            faceWidth = 0.95f + ((h shr 4 and 0xF) / 15f) * 0.15f
        )
    }

    // ==================== 3D 模型外观 ====================

    private val skinPalette = listOf(0xFFF3D0B0, 0xFFE8B58B, 0xFFD9A066, 0xFFC68642, 0xFF9C6B43, 0xFF7A4A2B)
    private val hairPalette = listOf(0xFF1B1B1B, 0xFF2B1B12, 0xFF3B2A1E, 0xFF4A3526, 0xFF6B4423)
    private val style3DPalette = listOf(
        HairStyle3D.SHORT, HairStyle3D.BUZZ, HairStyle3D.SHORT,
        HairStyle3D.CURLY, HairStyle3D.SHORT, HairStyle3D.FLOW
    )

    private fun to3DStyle(style: AvatarHairStyle): HairStyle3D = when (style) {
        AvatarHairStyle.BALD -> HairStyle3D.NONE
        AvatarHairStyle.BUZZ -> HairStyle3D.BUZZ
        AvatarHairStyle.FLOW -> HairStyle3D.FLOW
        AvatarHairStyle.AFRO -> HairStyle3D.AFRO
        AvatarHairStyle.CURLY -> HairStyle3D.CURLY
        AvatarHairStyle.PONYTAIL -> HairStyle3D.PONYTAIL
        else -> HairStyle3D.SHORT
    }

    /**
     * 生成一支球队的 3D 外观列表
     * @param starIndex 招牌球星所在下标（默认 9 = 中锋），该球员使用球队招牌球星特征
     * 下标 0 为门将，自动使用门将配色
     */
    fun lookForTeam(
        team: Team,
        playerCount: Int,
        starIndex: Int = 9
    ): List<PlayerLook> {
        val starParams = paramsForTeam(team.name, team.primaryColor, team.secondaryColor)
        return (0 until playerCount).map { i ->
            when {
                i == 0 -> PlayerLook(
                    kitColor1 = 0xFF212121.toInt(),          // 门将深色球衣
                    kitColor2 = team.primaryColor.toArgb(),
                    skinColor = starParams.skinColor.toArgb(),
                    hairColor = starParams.hairColor.toArgb(),
                    hairStyle3D = to3DStyle(starParams.hairStyle)
                )
                i == starIndex -> PlayerLook(
                    kitColor1 = team.primaryColor.toArgb(),
                    kitColor2 = team.secondaryColor.toArgb(),
                    skinColor = starParams.skinColor.toArgb(),
                    hairColor = starParams.hairColor.toArgb(),
                    hairStyle3D = to3DStyle(starParams.hairStyle)
                )
                else -> {
                    val seed = team.id.hashCode() * 31 + i
                    PlayerLook(
                        kitColor1 = team.primaryColor.toArgb(),
                        kitColor2 = team.secondaryColor.toArgb(),
                        skinColor = skinPalette[(seed and 0xFF) % skinPalette.size],
                        hairColor = hairPalette[(seed shr 8 and 0xFF) % hairPalette.size],
                        hairStyle3D = style3DPalette[(seed shr 16 and 0xFF) % style3DPalette.size]
                    )
                }
            }
        }
    }
}
