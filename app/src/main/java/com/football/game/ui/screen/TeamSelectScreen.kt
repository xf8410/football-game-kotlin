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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.football.game.model.Team

/**
 * 球队选择屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            .background(Color(0xFF1B5E20))
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2E7D32)
            )
        )

        // 联赛筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedLeague == null,
                onClick = { selectedLeague = null },
                label = { Text("全部") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50)
                )
            )

            leagues.forEach { league ->
                FilterChip(
                    selected = selectedLeague == league.name,
                    onClick = { selectedLeague = league.name },
                    label = { Text(league.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50)
                    )
                )
            }
        }

        // 球队网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(teams) { team ->
                TeamCard(
                    team = team,
                    isSelected = selectedTeam?.id == team.id,
                    onClick = { selectedTeam = team }
                )
            }
        }

        // 已选择的球队和确认按钮
        selectedTeam?.let { team ->
            SelectedTeamBar(
                team = team,
                onConfirm = { onTeamSelected(team) }
            )
        }
    }
}

/**
 * 球队卡片
 */
@Composable
fun TeamCard(
    team: Team,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 球队颜色圆圈
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(team.primaryColor)
                    .border(2.dp, Color.Gray, CircleShape)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 球队名称
            Text(
                text = team.shortName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = team.name.take(4),
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * 已选择球队栏
 */
@Composable
fun SelectedTeamBar(
    team: Team,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2E7D32))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 球队信息
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(team.primaryColor)
                .border(2.dp, Color.White, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = team.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${team.league} · ${team.country}",
                fontSize = 12.sp,
                color = Color(0xFFA5D6A7)
            )
        }

        // 确认按钮
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text("确认选择")
        }
    }
}

/**
 * 球队属性预览
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
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
 * 属性条
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
                .background(Color.Gray, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = value / 100f)
                    .height(8.dp)
                    .background(
                        when {
                            value >= 85 -> Color(0xFF4CAF50)
                            value >= 75 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
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