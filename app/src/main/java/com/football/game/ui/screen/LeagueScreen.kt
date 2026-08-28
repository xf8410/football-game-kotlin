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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.football.game.model.League
import com.football.game.model.Team

/**
 * 联赛模式屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueScreen(
    onTeamSelected: (League, Team) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var selectedLeague by remember { mutableStateOf(LeagueDatabase.ALL_LEAGUES.firstOrNull()) }

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

        // 联赛信息
        selectedLeague?.let { league ->
            LeagueInfoCard(league = league)
        }

        // 球队列表
        selectedLeague?.let { league ->
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(league.teams) { index, team ->
                    TeamListItem(
                        rank = index + 1,
                        team = team,
                        onClick = { onTeamSelected(league, team) }
                    )
                }
            }
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
                LeagueInfoItem("轮次", "${league.totalRounds}")
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
 * 球队列表项
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

            // 球队颜色
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(team.primaryColor)
                    .border(2.dp, Color.LightGray, CircleShape)
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
                Text(
                    text = team.shortName,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
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

/**
 * 联赛积分榜
 */
@Composable
fun LeagueStandings(
    league: League,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "球队",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "场",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "胜",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "平",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "负",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "分",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 球队行（示例）
            league.teams.take(5).forEachIndexed { index, team ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = team.shortName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "0",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "0",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "0",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "0",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}