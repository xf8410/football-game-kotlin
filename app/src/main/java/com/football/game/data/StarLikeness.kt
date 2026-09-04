package com.football.game.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.football.game.model.Team
import com.football.game.ui.component.AvatarFacialHair
import com.football.game.ui.component.AvatarHairStyle
import com.football.game.ui.component.HairStyle3D
import com.football.game.ui.component.KitPattern
import com.football.game.ui.component.PlayerLook
import com.football.game.ui.component.StarAvatarParams

/**
 * 球星特征库
 * 用标志性外貌特征（发型/胡子/肤色）让球员"像"真人球星：
 * - 2D 头像：LeagueScreen / EraSelectScreen 等界面
 * - 3D 模型：渲染器通过 PlayerLook 上色
 *
 * 角色卡系统：同一球星在不同球队 = 不同角色卡/职责（默认位置 + 可踢位置 +
 * 球队专属用法），例如阿什拉夫在皇马是右后卫，在大巴黎则按需客串边锋/中场/中卫。
 *
 * 队服系统：每支球队有独立的 球衣主色/袖色/球裤/球袜/花纹，
 * 同场比赛两队主色过于接近时自动换客场球衣，门将穿独立荧光色。
 *
 * 说明：全部为程序化卡通风格，不使用真实照片（肖像权），仅特征神似。
 */
object StarLikeness {

    // ==================== 球星特征预设 ====================
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
        "阿什拉夫·哈基米" to StarAvatarParams(
            skinColor = Color(0xFF9C6B43), hairColor = Color(0xFF141414),
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

    // ==================== 角色卡：同一球星不同球队不同职责 ====================

    /**
     * 球星角色卡
     * @param roles       可胜任的位置（role 代码）
     * @param duty        职责描述（踢法定位）
     * @param defaultRole 默认位置
     * @param byTeam      球队专属用法：某些球星在特定球队会被赋予不同职责
     *                    （特殊情况：如阿什拉夫在大巴黎按需客串边锋/中场/中卫）
     */
    data class StarRoleCard(
        val roles: List<String>,
        val duty: String,
        val defaultRole: String,
        val byTeam: Map<String, String> = emptyMap()
    )

    private val roleCards: Map<String, StarRoleCard> = mapOf(
        "哈兰德" to StarRoleCard(listOf("ST"), "禁区终结者：背身支点 + 抢点爆射", "ST"),
        "姆巴佩" to StarRoleCard(listOf("ST", "LW"), "反击箭头：斜插身后 + 高速爆趟", "ST"),
        "萨拉赫" to StarRoleCard(listOf("RW", "ST"), "右路内切爆点：肋部斜插 + 兜射远角", "RW"),
        "亚马尔" to StarRoleCard(listOf("RW", "LW"), "边路持球爆点：内切组织 + 倒三角", "RW"),
        "凯恩" to StarRoleCard(listOf("ST", "CM"), "支点中锋：回撤做球 + 禁区抢点", "ST"),
        "贝林厄姆" to StarRoleCard(listOf("CM", "ST"), "全能中场：后插上抢点 + 逼抢发动机", "CM"),
        "萨卡" to StarRoleCard(listOf("RW", "LW", "CM"), "边路走廊：下底传中 + 内切射门", "RW"),
        "孙兴慜" to StarRoleCard(listOf("LW", "ST", "RW"), "内切射手：左路右脚兜射远角 + 反击箭头", "LW"),
        "布鲁诺·费尔南德斯" to StarRoleCard(listOf("CM", "RW"), "组织核心：直塞调度 + 远射", "CM"),
        "帕尔默" to StarRoleCard(listOf("CM", "RW"), "前场自由人：肋部直塞 + 定位球", "CM"),
        "格里兹曼" to StarRoleCard(listOf("CM", "ST", "LW"), "影锋串联：穿插衔接 + 二点进攻", "CM"),
        "德布劳内" to StarRoleCard(listOf("CM", "RW"), "进攻发动机：贴地直塞 + 弧线传中", "CM"),
        "莱万多夫斯基" to StarRoleCard(listOf("ST"), "禁区之王：抢点头槌 + 背身做球", "ST"),
        "穆西亚拉" to StarRoleCard(listOf("CM", "LW", "RW"), "盘带推进器：中路持球突破", "CM"),
        "维尔茨" to StarRoleCard(listOf("CM", "LW"), "创造力中场：穿透直塞 + 推进", "CM"),
        "劳塔罗" to StarRoleCard(listOf("ST"), "拼抢型中锋：压迫逼抢 + 抢点", "ST"),
        "莱奥" to StarRoleCard(listOf("LW", "ST"), "左路爆点：高速爆趟 + 强突底线", "LW"),
        "弗拉霍维奇" to StarRoleCard(listOf("ST"), "抢点中锋：禁区抢射 + 支点", "ST"),
        "克瓦拉茨赫利亚" to StarRoleCard(listOf("LW", "RW"), "内切型边锋：右脚兜射远角", "LW"),
        "登贝莱" to StarRoleCard(listOf("RW", "LW", "ST"), "双足边锋：左右开弓 + 反击箭头", "RW"),
        "阿德耶米" to StarRoleCard(listOf("LW", "ST"), "速度型边锋：身后冲刺", "LW"),
        "阿什拉夫·哈基米" to StarRoleCard(
            listOf("RB", "CB", "CM", "RW", "LW"),
            "边路万金油：攻守兼备，助攻型边卫；在大巴黎属特殊情况——按需客串边锋/中场/中卫",
            "RB",
            byTeam = mapOf("paris_saint_germain" to "RW")
        ),
        "梅西" to StarRoleCard(listOf("RW", "CM", "ST"), "自由人：右路内切 + 中路组织", "RW"),
        "C罗" to StarRoleCard(listOf("ST", "LW"), "全能攻击手：抢点 + 内切爆射", "ST"),
        "内马尔" to StarRoleCard(listOf("LW", "CM"), "边路魔术师：持球突破 + 直塞", "LW")
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
        "巴黎圣日耳曼" to "阿什拉夫·哈基米"
    )

    // ==================== 角色卡查询 ====================

    /** role 代码 → 4-3-3 槽位下标 */
    private fun slotForRole(role: String): Int = when (role) {
        "GK" -> 0
        "LB" -> 1
        "CB" -> 2
        "RB" -> 4
        "CM" -> 6
        "LW" -> 8
        "ST" -> 9
        "RW" -> 10
        else -> 9
    }

    /**
     * 球星在 4-3-3 中的位置槽（按角色卡：同一球星在不同球队不同职责 → 不同槽位）
     * 例：阿什拉夫默认右后卫（槽 4），在大巴黎按角色卡踢边锋（槽 10）
     */
    fun starSlotForTeam(team: Team): Int {
        val star = teamStar[team.name] ?: return 9
        val card = roleCards[star] ?: return 9
        val role = card.byTeam[team.id] ?: card.defaultRole
        return slotForRole(role)
    }

    /** 球队招牌球星的角色卡（名字 + 卡），无招牌球星/无卡返回 null */
    fun roleCardForTeam(team: Team): Pair<String, StarRoleCard>? {
        val star = teamStar[team.name] ?: return null
        val card = roleCards[star] ?: return null
        return star to card
    }

    /** 球星在该球队实际担任的位置 role 代码 */
    fun starRoleForTeam(team: Team): String? {
        val star = teamStar[team.name] ?: return null
        val card = roleCards[star] ?: return null
        return card.byTeam[team.id] ?: card.defaultRole
    }

    // ==================== 球队队服库 ====================

    /**
     * 队服规格：球衣主色 + 袖/花纹副色 + 球裤 + 球袜 + 花纹
     */
    data class KitSpec(
        val shirt: Color,      // 球衣主色
        val shirt2: Color,     // 袖子/条纹副色
        val shorts: Color,     // 球裤
        val socks: Color,      // 球袜
        val pattern: KitPattern = KitPattern.SOLID
    )

    /** 各队经典配色（按 team.id 匹配；纯色队服 shirt2 = 袖子颜色） */
    private val teamKits: Map<String, KitSpec> = mapOf(
        "real_madrid" to KitSpec(Color.White, Color.White, Color.White, Color(0xFFF2F2F2)),
        "barcelona" to KitSpec(Color(0xFFA50044), Color(0xFF004D98), Color(0xFF004D98), Color(0xFF004D98), KitPattern.STRIPES),
        "manchester_city" to KitSpec(Color(0xFF6CABDD), Color.White, Color.White, Color(0xFF6CABDD)),
        "liverpool" to KitSpec(Color(0xFFC8102E), Color(0xFFC8102E), Color(0xFFC8102E), Color(0xFFC8102E)),
        "bayern_munich" to KitSpec(Color(0xFFDC052D), Color.White, Color(0xFFDC052D), Color(0xFFDC052D)),
        "arsenal" to KitSpec(Color(0xFFEF0107), Color.White, Color.White, Color(0xFFEF0107)),
        "manchester_united" to KitSpec(Color(0xFFDA291C), Color(0xFFDA291C), Color.White, Color(0xFF1B1B1B)),
        "chelsea" to KitSpec(Color(0xFF034694), Color.White, Color(0xFF034694), Color(0xFF034694)),
        "tottenham_hotspur" to KitSpec(Color.White, Color(0xFF132257), Color.White, Color.White),
        "atletico_madrid" to KitSpec(Color(0xFFCB3524), Color.White, Color(0xFF272E61), Color(0xFFCB3524), KitPattern.STRIPES),
        "bayer_leverkusen" to KitSpec(Color(0xFFE32636), Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFFE32636)),
        "borussia_dortmund" to KitSpec(Color(0xFFFDE100), Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFFFDE100)),
        "inter_milan" to KitSpec(Color(0xFF0B1F8F), Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFF1B1B1B), KitPattern.STRIPES),
        "ac_milan" to KitSpec(Color(0xFF1B1B1B), Color(0xFFD2232A), Color.White, Color(0xFF1B1B1B), KitPattern.STRIPES),
        "juventus" to KitSpec(Color.White, Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFF1B1B1B), KitPattern.STRIPES),
        "napoli" to KitSpec(Color(0xFF12A0D7), Color.White, Color.White, Color(0xFF12A0D7)),
        "paris_saint_germain" to KitSpec(Color(0xFF004170), Color(0xFFD2042C), Color(0xFF004170), Color(0xFF004170), KitPattern.SASH)
    )

    /** 门将独立配色（与场上球员和对方门将都区分开） */
    private val gkKitHome = KitSpec(Color(0xFF2FE86B), Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFF2FE86B))
    private val gkKitAway = KitSpec(Color(0xFFFF8A1E), Color(0xFF1B1B1B), Color(0xFF1B1B1B), Color(0xFFFF8A1E))

    /** 未收录球队的兜底队服：主色衣身 + 副色袖/裤 + 主色袜 */
    private fun kitForTeam(team: Team): KitSpec {
        teamKits[team.id]?.let { return it }
        return KitSpec(team.primaryColor, team.secondaryColor, team.secondaryColor, team.primaryColor)
    }

    /** 两队球衣主色是否"撞衫"（RGB 距离过近，0~441 量程） */
    private fun kitClash(a: KitSpec, b: KitSpec): Boolean {
        val ca = a.shirt.toArgb()
        val cb = b.shirt.toArgb()
        val dr = ((ca shr 16) and 0xFF) - ((cb shr 16) and 0xFF)
        val dg = ((ca shr 8) and 0xFF) - ((cb shr 8) and 0xFF)
        val db = (ca and 0xFF) - (cb and 0xFF)
        val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
        return dist < 66f
    }

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
            skinColor = Color(skins[(h and 0xFF) % skins.size]),
            hairColor = Color(hairs[(h shr 8 and 0xFF) % hairs.size]),
            hairStyle = styles[(h shr 16 and 0xFF) % styles.size],
            facialHair = beards[(h shr 24 and 0xFF) % beards.size],
            faceWidth = 0.95f + ((h shr 4 and 0xF) / 15f) * 0.15f
        )
    }

    // ==================== 3D 模型外观 ====================

    private val skinPalette = listOf(
        0xFFF3D0B0.toInt(), 0xFFE8B58B.toInt(), 0xFFD9A066.toInt(),
        0xFFC68642.toInt(), 0xFF9C6B43.toInt(), 0xFF7A4A2B.toInt()
    )
    private val hairPalette = listOf(
        0xFF1B1B1B.toInt(), 0xFF2B1B12.toInt(), 0xFF3B2A1E.toInt(),
        0xFF4A3526.toInt(), 0xFF6B4423.toInt()
    )
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
     * 生成一支球队的 3D 外观列表（独立使用，无撞衫处理）
     * 招牌球星槽位按角色卡解析（starSlotForTeam），下标 0 为门将
     */
    fun lookForTeam(
        team: Team,
        playerCount: Int,
        starIndex: Int = starSlotForTeam(team)
    ): List<PlayerLook> {
        return buildLooks(team, kitForTeam(team), gkKitHome, playerCount, starIndex)
    }

    /**
     * 一场比赛的主客队外观（含撞衫处理 + 双方门将独立配色）：
     * 两队球衣主色过于接近时，客队自动改穿客场（副色衣身），仍撞则强制黑衫。
     * 例：利物浦 vs 拜仁（红对红）→ 客队换客场；皇马 vs 热刺（白对白）→ 客队换藏青。
     */
    fun looksForMatch(
        home: Team,
        away: Team,
        playerCount: Int
    ): Pair<List<PlayerLook>, List<PlayerLook>> {
        val homeKit = kitForTeam(home)
        var awayKit = kitForTeam(away)
        if (kitClash(homeKit, awayKit)) {
            // 客队改穿客场：副色为衣身，主色作袖/花纹
            val swapped = KitSpec(awayKit.shirt2, awayKit.shirt, awayKit.shirt, awayKit.socks, awayKit.pattern)
            awayKit = if (kitClash(homeKit, swapped)) {
                KitSpec(Color(0xFF1B1B1B), Color.White, Color(0xFF1B1B1B), Color(0xFF1B1B1B))
            } else {
                swapped
            }
        }
        return buildLooks(home, homeKit, gkKitHome, playerCount, starSlotForTeam(home)) to
            buildLooks(away, awayKit, gkKitAway, playerCount, starSlotForTeam(away))
    }

    /** 按队服规格生成 11 人外观（下标 0 门将，starIndex 招牌球星槽位） */
    private fun buildLooks(
        team: Team,
        kit: KitSpec,
        gk: KitSpec,
        playerCount: Int,
        starIndex: Int
    ): List<PlayerLook> {
        val starParams = paramsForTeam(team.name, kit.shirt, kit.shirt2)
        return (0 until playerCount).map { i ->
            val seed = team.id.hashCode() * 31 + i
            val skin = skinPalette[(seed and 0xFF) % skinPalette.size]
            val hair = hairPalette[(seed shr 8 and 0xFF) % hairPalette.size]
            val style = style3DPalette[(seed shr 16 and 0xFF) % style3DPalette.size]
            when {
                i == 0 -> PlayerLook(
                    kitColor1 = gk.shirt.toArgb(),
                    kitColor2 = gk.shirt2.toArgb(),
                    skinColor = skin,
                    hairColor = hair,
                    hairStyle3D = style,
                    shortsColor = gk.shorts.toArgb(),
                    socksColor = gk.socks.toArgb(),
                    pattern = gk.pattern,
                    isGoalkeeper = true
                )
                i == starIndex -> PlayerLook(
                    kitColor1 = kit.shirt.toArgb(),
                    kitColor2 = kit.shirt2.toArgb(),
                    skinColor = starParams.skinColor.toArgb(),
                    hairColor = starParams.hairColor.toArgb(),
                    hairStyle3D = to3DStyle(starParams.hairStyle),
                    shortsColor = kit.shorts.toArgb(),
                    socksColor = kit.socks.toArgb(),
                    pattern = kit.pattern
                )
                else -> PlayerLook(
                    kitColor1 = kit.shirt.toArgb(),
                    kitColor2 = kit.shirt2.toArgb(),
                    skinColor = skin,
                    hairColor = hair,
                    hairStyle3D = style,
                    shortsColor = kit.shorts.toArgb(),
                    socksColor = kit.socks.toArgb(),
                    pattern = kit.pattern
                )
            }
        }
    }
}
