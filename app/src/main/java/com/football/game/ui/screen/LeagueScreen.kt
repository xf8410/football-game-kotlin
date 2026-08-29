package com.football.game.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.data.LeagueDatabase
import com.football.game.data.StarLikeness
import com.football.game.league.Fixture
import com.football.game.league.LeagueManager
import com.football.game.model.League
import com.football.game.model.Team
import com.football.game.ui.component.StarAvatarView

/**
 * 联赛模式屏幕
 * 视图：球队列表 / 积分榜 / 赛程
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueScreen(
    onTeamSelected: (League, Team) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var selectedLeague by remember { mutableStateOf(LeagueDatabase.ALL_LEAGUES.firstOrNull()) }
    var viewMode by remember { mutableStateOf(LeagueViewMode.TEAMS) }
    // 每次切换联赛时重建联赛管理器（新赛季）
    val manager = remember(selectedLeague) { selectedLeague?.let { LeagueManager(it) } }
    // 模拟完一轮后自增，驱动积分榜/赛程刷新
    var refreshTick by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Text(
                    text = "联赛模式",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2E7D32)
            )
        )

        // 联赛选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeagueDatabase.ALL_LEAGUES.forEach { league ->
                FilterChip(
                    selected = selectedLeague?.id == league.id,
                    onClick = { selectedLeague = league },
                    label = { Text(league.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50)
                    )
                )
            }
        }

        // 视图切换：球队 / 积分榜 / 赛程
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeagueViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = viewMode == mode,
                    onClick = { viewMode = mode },
                    label = { Text(mode.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // 联赛信息
        selectedLeague?.let { league ->
            LeagueInfoCard(league = league)
        }

        // 内容区
        when (viewMode) {
            LeagueViewMode.TEAMS -> {
                selectedLeague?.let { league ->
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val teams = manager?.teams ?: league.teams
                        itemsIndexed(teams) { index, team ->
                            TeamListItem(
                                rank = index + 1,
                                team = team,
                                onClick = { onTeamSelected(league, team) }
                            )
                        }
                    }
                }
            }

            LeagueViewMode.STANDINGS -> {
                manager?.let { mgr ->
                    StandingsTab(
                        manager = mgr,
                        refreshTick = refreshTick
                    )
                }
            }

            LeagueViewMode.FIXTURES -> {
                manager?.let { mgr ->
                    FixturesTab(
                        manager = mgr,
                        refreshTick = refreshTick,
                        onPlayRound = {
                            mgr.playRound()
                            refreshTick++
                        },
                        onReset = {
                            mgr.reset()
                            refreshTick++
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 联赛视图模式
 */
enum class LeagueViewMode(val label: String) {
    TEAMS("球队"),
    STANDINGS("积分榜"),
    FIXTURES("赛程")
}

/**
 * 积分榜页签
 */
@Composable
fun StandingsTab(
    manager: LeagueManager,
    refreshTick: Int
) {
    val standings = manager.standings()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 表头
                StandingsHeaderRow()

                Spacer(modifier = Modifier.height(4.dp))

                // 球队行
                standings.forEachIndexed { index, team ->
                    StandingsRow(
                        rank = index + 1,
                        team = team,
                        totalTeams = standings.size
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 图例
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StandingsLegendItem(Color(0xFFFFD700), "冠军")
            StandingsLegendItem(Color(0xFF4CAF50), "欧战区")
            StandingsLegendItem(Color(0xFFF44336), "降级区")
        }
    }
}

/**
 * 积分榜表头
 */
@Composable
private fun StandingsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StandingsCell("#", 24.dp, bold = true, light = true)
        Text(
            text = "球队",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        StandingsCell("赛", 26.dp, bold = true, light = true)
        StandingsCell("胜", 26.dp, bold = true, light = true)
        StandingsCell("平", 26.dp, bold = true, light = true)
        StandingsCell("负", 26.dp, bold = true, light = true)
        StandingsCell("净", 30.dp, bold = true, light = true)
        StandingsCell("分", 30.dp, bold = true, light = true)
    }
}

/**
 * 积分榜数据行
 */
@Composable
private fun StandingsRow(
    rank: Int,
    team: Team,
    totalTeams: Int
) {
    val rankColor = when {
        rank == 1 -> Color(0xFFFFD700)
        rank <= 4 -> Color(0xFF4CAF50)
        rank > totalTeams - 2 -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StandingsCell("$rank", 24.dp, color = rankColor, bold = true)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(team.primaryColor)
                    .border(1.dp, Color.LightGray, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = team.shortName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        StandingsCell("${team.gamesPlayed}", 26.dp)
        StandingsCell("${team.wins}", 26.dp)
        StandingsCell("${team.draws}", 26.dp)
        StandingsCell("${team.losses}", 26.dp)
        StandingsCell("${team.goalDifference}", 30.dp)
        StandingsCell("${team.points}", 30.dp, color = Color(0xFF2E7D32), bold = true)
    }
}

/**
 * 积分榜单元格
 */
@Composable
private fun StandingsCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color(0xFF424242),
    bold: Boolean = false,
    light: Boolean = false
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = if (light) Color.White else color,
        modifier = Modifier.width(width),
        textAlign = TextAlign.Center
    )
}

/**
 * 积分榜图例
 */
@Composable
private fun StandingsLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = Color(0xFFA5D6A7))
    }
}

/**
 * 赛程页签
 */
@Composable
fun FixturesTab(
    manager: LeagueManager,
    refreshTick: Int,
    onPlayRound: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 轮次信息 + 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (manager.isFinished) {
                    "赛季结束 ${manager.totalRounds}/${manager.totalRounds} 轮"
                } else {
                    "第 ${manager.completedRounds + 1} / ${manager.totalRounds} 轮"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlayRound,
                    enabled = !manager.isFinished,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (manager.isFinished) "已完赛" else "模拟下一轮",
                        fontSize = 13.sp
                    )
                }
                OutlinedButton(
                    onClick = onReset,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "重置", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 冠军横幅
        manager.champion()?.let { champion ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🏆", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "联赛冠军",
                            fontSize = 12.sp,
                            color = Color(0xFF5D4037)
                        )
                        Text(
                            text = champion.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3E2723)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 赛程列表
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 下一轮赛程
            if (!manager.isFinished) {
                item {
                    FixtureSectionHeader("第 ${manager.completedRounds + 1} 轮赛程")
                }
                itemsIndexed(manager.nextFixtures) { _, fixture ->
                    FixtureRow(fixture = fixture, played = false)
                }
            }

            // 最近完成的轮次赛果
            if (manager.completedRounds > 0) {
                item {
                    FixtureSectionHeader("第 ${manager.completedRounds} 轮赛果")
                }
                itemsIndexed(manager.schedule[manager.completedRounds - 1]) { _, fixture ->
                    FixtureRow(fixture = fixture, played = true)
                }
            }
        }
    }
}

/**
 * 赛程分组标题
 */
@Composable
private fun FixtureSectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFFC107),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/**
 * 单场比赛行
 */
@Composable
fun FixtureRow(
    fixture: Fixture,
    played: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (played) Color.White else Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主队
            Text(
                text = fixture.home.name,
                fontSize = 14.sp,
                fontWeight = if (played && fixture.homeGoals > fixture.awayGoals) FontWeight.Bold else FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1
            )

            // 比分
            Text(
                text = if (played) "${fixture.homeGoals} - ${fixture.awayGoals}" else "vs",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (played) Color(0xFF2E7D32) else Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // 客队
            Text(
                text = fixture.away.name,
                fontSize = 14.sp,
                fontWeight = if (played && fixture.awayGoals > fixture.homeGoals) FontWeight.Bold else FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
    }
}

/**
 * 联赛信息卡片
 */
@Composable
fun LeagueInfoCard(league: League) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = league.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LeagueInfoItem("国家", league.country)
                LeagueInfoItem("球队数", "${league.teams.size}")
                LeagueInfoItem("轮次", "${2 * (league.teams.size - 1)}")
            }
        }
    }
}

/**
 * 联赛信息项
 */
@Composable
fun LeagueInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFFA5D6A7)
        )
    }
}

/**
 * 球队列表项（带招牌球星头像）
 */
@Composable
fun TeamListItem(
    rank: Int,
    team: Team,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排名
            Text(
                text = "$rank",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center
            )

            // 招牌球星头像（球衣配色）
            StarAvatarView(
                params = StarLikeness.paramsForTeam(team.name, team.primaryColor, team.secondaryColor),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 球队信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = team.shortName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    StarLikeness.starNameForTeam(team.name)?.let { star ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⭐ $star",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA000)
                        )
                    }
                }
            }

            // 球队能力
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "OVR",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${team.overallRating}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        team.overallRating >= 85 -> Color(0xFF4CAF50)
                        team.overallRating >= 75 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
            }
        }
    }
}
