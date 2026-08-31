package com.football.game.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.data.LeagueDatabase
import com.football.game.data.StarLikeness
import com.football.game.model.Team
import kotlin.math.roundToInt

// ==================== 主题色 ====================

private val BgDeep = Color(0xFF0A1F12)
private val BgMid = Color(0xFF12301C)
private val PanelDark = Color(0xFF0F2C17)
private val AccentGold = Color(0xFFFFD54F)
private val TextSubtle = Color(0xFF9BB8A4)

/** 按联赛名配色（英超紫 / 西甲橙 / 意甲蓝 / 德甲红 / 法甲藏青），识别不到按索引取调色板 */
private fun leagueColor(name: String, index: Int): Color = when {
    name.contains("英") -> Color(0xFF38003C)
    name.contains("西") -> Color(0xFFE8590C)
    name.contains("意") -> Color(0xFF1B2C8F)
    name.contains("德") -> Color(0xFFD20515)
    name.contains("法") -> Color(0xFF12315E)
    else -> leagueFallback[index % leagueFallback.size]
}

private val leagueFallback = listOf(
    Color(0xFF2E7D32), Color(0xFFE8590C), Color(0xFF1B2C8F),
    Color(0xFFD20515), Color(0xFF12315E)
)

/**
 * 球队选择屏幕（选择联赛/球队）—— 夜场高级风：
 * 深绿渐变底 + 联赛色胶囊页签 + 双列球队卡（队徽/星级/OVR/招牌球星）+ 底部确认栏（属性条 + 确认按钮）
 */
@Composable
fun TeamSelectScreen(
    title: String = "选择球队",
    onTeamSelected: (Team) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedLeague by remember { mutableStateOf<String?>(null) }
    var selectedTeam by remember { mutableStateOf<Team?>(null) }

    val leagues = LeagueDatabase.ALL_LEAGUES
    val teams = if (selectedLeague != null) {
        LeagueDatabase.getTeamsByLeague(selectedLeague!!)
    } else {
        LeagueDatabase.getAllTeams()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgDeep, BgMid, BgDeep)))
    ) {
        // ===== 顶部：标题 + 返回 =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(
                    text = "挑选你的球队，踏上绿茵征程",
                    fontSize = 11.sp,
                    color = TextSubtle
                )
            }
        }

        // ===== 联赛页签（横向滚动胶囊，联赛主色）=====
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                LeagueChip(
                    label = "全部联赛",
                    teamCount = LeagueDatabase.getAllTeams().size,
                    color = AccentGold,
                    isSelected = selectedLeague == null,
                    onClick = { selectedLeague = null }
                )
            }
            items(leagues.size) { index ->
                val league = leagues[index]
                LeagueChip(
                    label = league.name,
                    teamCount = league.teams.size,
                    color = leagueColor(league.name, index),
                    isSelected = selectedLeague == league.name,
                    onClick = { selectedLeague = league.name }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== 球队网格（双列大卡）=====
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(teams, key = { it.id }) { team ->
                TeamCard(
                    team = team,
                    isSelected = selectedTeam?.id == team.id,
                    onClick = { selectedTeam = team }
                )
            }
        }

        // ===== 底部确认栏 =====
        selectedTeam?.let { team ->
            SelectedTeamBar(
                team = team,
                onConfirm = { onTeamSelected(team) }
            )
        }
    }
}

/**
 * 联赛胶囊页签
 */
@Composable
private fun LeagueChip(
    label: String,
    teamCount: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) color else Color.White.copy(alpha = 0.07f),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSubtle,
        label = "chipFg"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = fg
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$teamCount",
            fontSize = 10.sp,
            color = fg.copy(alpha = 0.65f)
        )
    }
}

/**
 * 球队卡片：顶部主色带 + 队徽圆 + 球队名 + 星级 + OVR + 招牌球星
 */
@Composable
fun TeamCard(
    team: Team,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentGold else Color.Transparent,
        label = "cardBorder"
    )
    val overall = team.overallRating
    val starName = StarLikeness.starNameForTeam(team.name)
    val filledStars = (overall / 20f).roundToInt().coerceIn(1, 5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .scale(scale)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF163822) else Color(0xFF112A19)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // 顶部主色带（主色→副色渐变）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(team.primaryColor, team.secondaryColor)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 队徽（主色圆 + 副色描边 + 简称首字）
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(team.primaryColor)
                            .border(2.dp, team.secondaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = team.shortName.take(2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = team.shortName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = team.league,
                            fontSize = 10.sp,
                            color = TextSubtle,
                            maxLines = 1
                        )
                    }
                    // OVR
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "OVR", fontSize = 9.sp, color = TextSubtle)
                        Text(
                            text = "$overall",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                overall >= 85 -> Color(0xFF7CFC9B)
                                overall >= 75 -> AccentGold
                                else -> Color(0xFFFF8A80)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 星级
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★".repeat(filledStars),
                        fontSize = 11.sp,
                        color = AccentGold
                    )
                    Text(
                        text = "★".repeat(5 - filledStars),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.18f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    starName?.let { star ->
                        Text(
                            text = "⭐$star",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 已选择球队栏（底部确认栏）：球队信息 + 四维属性迷你条 + 确认按钮（主色渐变）
 */
@Composable
fun SelectedTeamBar(
    team: Team,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelDark)
            .border(1.dp, Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(team.primaryColor)
                    .border(2.dp, team.secondaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = team.shortName.take(2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${team.league} · ${team.country}",
                    fontSize = 11.sp,
                    color = TextSubtle
                )
            }

            // 确认按钮（主色→副色渐变胶囊）
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .width(120.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = team.primaryColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "确认选择 ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 四维属性迷你条
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStat("进攻", team.attack, Modifier.weight(1f))
            MiniStat("中场", team.midfield, Modifier.weight(1f))
            MiniStat("防守", team.defense, Modifier.weight(1f))
            MiniStat("门将", team.goalkeeper, Modifier.weight(1f))
        }
    }
}

/**
 * 迷你属性条：标签 + 数值 + 渐变条
 */
@Composable
private fun MiniStat(label: String, value: Int, modifier: Modifier = Modifier) {
    val barColor = when {
        value >= 85 -> Color(0xFF7CFC9B)
        value >= 75 -> AccentGold
        else -> Color(0xFFFF8A80)
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 10.sp, color = TextSubtle)
            Text(
                text = "$value",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(barColor, RoundedCornerShape(3.dp))
            )
        }
    }
}

/**
 * 球队属性预览（保留旧接口：深色重绘版）
 */
@Composable
fun TeamStatsPreview(
    team: Team,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(PanelDark, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "球队能力",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatBar("进攻", team.attack)
        StatBar("中场", team.midfield)
        StatBar("防守", team.defense)
        StatBar("门将", team.goalkeeper)
    }
}

/**
 * 属性条（保留旧接口）
 */
@Composable
fun StatBar(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.width(40.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = value / 100f)
                    .height(8.dp)
                    .background(
                        when {
                            value >= 85 -> Color(0xFF7CFC9B)
                            value >= 75 -> AccentGold
                            else -> Color(0xFFFF8A80)
                        },
                        RoundedCornerShape(4.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$value",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
