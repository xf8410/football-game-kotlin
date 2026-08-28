package com.football.game.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.model.Match
import com.football.game.model.Team

/**
 * 比赛结果屏幕
 */
@Composable
fun MatchResultScreen(
    match: Match,
    onBackToMenu: () -> Unit = {},
    onRematch: () -> Unit = {}
) {
    val resultText = when {
        match.homeScore > match.awayScore -> "胜利！"
        match.homeScore < match.awayScore -> "失败"
        else -> "平局"
    }

    val resultColor = when {
        match.homeScore > match.awayScore -> Color(0xFF4CAF50)
        match.homeScore < match.awayScore -> Color(0xFFF44336)
        else -> Color(0xFFFFC107)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 结果标题
        Text(
            text = resultText,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = resultColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 比分卡片
        ScoreCard(
            homeTeam = match.homeTeam,
            awayTeam = match.awayTeam,
            homeScore = match.homeScore,
            awayScore = match.awayScore
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 比赛统计
        MatchStatsCard(match = match)

        Spacer(modifier = Modifier.height(16.dp))

        // 进球列表
        if (match.homeScorers.isNotEmpty() || match.awayScorers.isNotEmpty()) {
            ScorersCard(match = match)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF607D8B)
                )
            ) {
                Text("返回菜单", fontSize = 16.sp)
            }

            Button(
                onClick = onRematch,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("再来一局", fontSize = 16.sp)
            }
        }
    }
}

/**
 * 比分卡片
 */
@Composable
fun ScoreCard(
    homeTeam: Team,
    awayTeam: Team,
    homeScore: Int,
    awayScore: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主队
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(homeTeam.primaryColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = homeTeam.shortName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // 比分
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$homeScore - $awayScore",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // 客队
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(awayTeam.primaryColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = awayTeam.shortName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * 比赛统计卡片
 */
@Composable
fun MatchStatsCard(match: Match) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "比赛统计",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 统计项
            StatRow(
                label = "射门",
                homeValue = "${match.stats.shotsHome}",
                awayValue = "${match.stats.shotsAway}"
            )
            StatRow(
                label = "射正",
                homeValue = "${match.stats.shotsOnTargetHome}",
                awayValue = "${match.stats.shotsOnTargetAway}"
            )
            StatRow(
                label = "犯规",
                homeValue = "${match.stats.foulsHome}",
                awayValue = "${match.stats.foulsAway}"
            )
            StatRow(
                label = "角球",
                homeValue = "${match.stats.cornersHome}",
                awayValue = "${match.stats.cornersAway}"
            )
            StatRow(
                label = "黄牌",
                homeValue = "${match.stats.yellowCardsHome}",
                awayValue = "${match.stats.yellowCardsAway}"
            )
        }
    }
}

/**
 * 统计行
 */
@Composable
fun StatRow(
    label: String,
    homeValue: String,
    awayValue: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = homeValue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )

        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            text = awayValue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Start
        )
    }
}

/**
 * 进球列表卡片
 */
@Composable
fun ScorersCard(match: Match) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "进球",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 主队进球
            match.homeScorers.forEach { goal ->
                ScorerItem(
                    teamName = match.homeTeam.shortName,
                    playerName = goal.playerName,
                    minute = goal.minute,
                    type = goal.type,
                    teamColor = Color(0xFFC62828)
                )
            }

            // 客队进球
            match.awayScorers.forEach { goal ->
                ScorerItem(
                    teamName = match.awayTeam.shortName,
                    playerName = goal.playerName,
                    minute = goal.minute,
                    type = goal.type,
                    teamColor = Color(0xFF1565C0)
                )
            }
        }
    }
}

/**
 * 进球项
 */
@Composable
fun ScorerItem(
    teamName: String,
    playerName: String,
    minute: Int,
    type: String,
    teamColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$minute'",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.width(40.dp)
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(teamColor)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = playerName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        if (type != "普通进球") {
            Text(
                text = type,
                fontSize = 12.sp,
                color = Color(0xFFFF9800),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}