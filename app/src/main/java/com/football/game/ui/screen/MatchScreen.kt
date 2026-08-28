package com.football.game.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.core.GameEngine
import com.football.game.core.GameState
import com.football.game.core.GoalAnnouncement
import com.football.game.core.GoalTypes
import com.football.game.core.Vector3
import com.football.game.model.Team
import com.football.game.ui.component.GoalAnnouncementUI
import com.football.game.ui.component.TouchControls
import kotlinx.coroutines.delay

@Composable
fun MatchScreen(
    homeTeamName: String = "红队",
    awayTeamName: String = "蓝队",
    homeTeam: Team? = null,
    awayTeam: Team? = null,
    onMatchEnd: () -> Unit = {}
) {
    var matchTime by remember { mutableFloatStateOf(0f) }
    var homeScore by remember { mutableIntStateOf(0) }
    var awayScore by remember { mutableIntStateOf(0) }
    var currentHalf by remember { mutableIntStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var currentAnnouncement by remember { mutableStateOf<GoalAnnouncement?>(null) }
    var ballPosition by remember { mutableStateOf(Vector3.ZERO) }
    var ballHeight by remember { mutableFloatStateOf(0f) }
    var hasBall by remember { mutableStateOf(false) }
    
    val match = remember {
        com.football.game.model.Match(
            homeTeam = homeTeam ?: Team(id = "home", name = homeTeamName, shortName = "HOM"),
            awayTeam = awayTeam ?: Team(id = "away", name = awayTeamName, shortName = "AWY")
        )
    }
    
    val gameEngine = remember {
        GameEngine(match = match, homePlayers = emptyList(), awayPlayers = emptyList())
    }
    
    fun simulateGoal(isHome: Boolean) {
        val methods = listOf(
            GoalTypes.GoalMethod.SHOT_NORMAL,
            GoalTypes.GoalMethod.SHOT_POWERFUL,
            GoalTypes.GoalMethod.SHOT_CURLED,
            GoalTypes.GoalMethod.HEADER_NORMAL,
            GoalTypes.GoalMethod.SHOT_FAR_POST,
            GoalTypes.GoalMethod.SHOT_CROSS,
            GoalTypes.GoalMethod.REBOUND
        )
        val method = methods.random()
        
        if (isHome) homeScore++ else awayScore++
        
        val announcement = GoalTypes.generateGoalAnnouncement(
            goalMethod = method,
            scorerName = if (isHome) "主队球员" else "客队球员",
            minute = matchTime.toInt() / 60 + if (currentHalf == 2) 45 else 0,
            goalCount = 1,
            teamName = if (isHome) homeTeamName else awayTeamName,
            opponentName = if (isHome) awayTeamName else homeTeamName,
            currentScore = Pair(homeScore, awayScore),
            goalPosition = GoalTypes.detectGoalPosition(0f, 0f, GameState.GOAL_WIDTH),
            goalContext = GoalTypes.detectGoalContext(
                teamScore = if (isHome) homeScore else awayScore,
                opponentScore = if (isHome) awayScore else homeScore,
                minute = matchTime.toInt() / 60,
                isExtraTime = false,
                isSecondHalf = currentHalf == 2
            )
        )
        
        currentAnnouncement = announcement
    }
    
    LaunchedEffect(isPaused, isFinished) {
        if (!isPaused && !isFinished) {
            while (matchTime < 2700f) {
                delay(100L)
                if (!isPaused && !isFinished) {
                    matchTime += 10f
                    if (matchTime.toInt() % 600 == 0 && matchTime > 0) {
                        if (Math.random() > 0.6) {
                            simulateGoal(Math.random() > 0.5)
                        }
                    }
                    if (matchTime >= 2700f && currentHalf == 1) {
                        currentHalf = 2
                        matchTime = 0f
                    } else if (matchTime >= 2700f && currentHalf == 2) {
                        isFinished = true
                    }
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawField(size.width, size.height)
            drawPlayers(size.width, size.height)
            drawBall(size.width, size.height, ballPosition, ballHeight)
        }
        
        GoalAnnouncementUI(
            announcement = currentAnnouncement,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
        Scoreboard(
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            homeScore = homeScore,
            awayScore = awayScore,
            matchTime = matchTime,
            currentHalf = currentHalf,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
        
        TouchControls(
            gameEngine = gameEngine,
            hasBall = hasBall,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawField(width: Float, height: Float) {
    val fieldLeft = width * 0.05f
    val fieldRight = width * 0.95f
    val fieldTop = height * 0.1f
    val fieldBottom = height * 0.9f
    
    drawLine(Color.White, Offset(fieldLeft, fieldTop), Offset(fieldRight, fieldTop), 3f)
    drawLine(Color.White, Offset(fieldLeft, fieldBottom), Offset(fieldRight, fieldBottom), 3f)
    drawLine(Color.White, Offset(fieldLeft, fieldTop), Offset(fieldLeft, fieldBottom), 3f)
    drawLine(Color.White, Offset(fieldRight, fieldTop), Offset(fieldRight, fieldBottom), 3f)
    drawLine(Color.White, Offset(width / 2, fieldTop), Offset(width / 2, fieldBottom), 3f)
    drawCircle(Color.White, height * 0.1f, Offset(width / 2, height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
    
    val penaltyWidth = width * 0.2f
    val penaltyHeight = height * 0.15f
    drawRect(Color.White, Offset(width / 2 - penaltyWidth / 2, fieldTop), androidx.compose.ui.geometry.Size(penaltyWidth, penaltyHeight), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
    drawRect(Color.White, Offset(width / 2 - penaltyWidth / 2, fieldBottom - penaltyHeight), androidx.compose.ui.geometry.Size(penaltyWidth, penaltyHeight), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlayers(width: Float, height: Float) {
    val homePositions = listOf(
        Offset(width * 0.5f, height * 0.15f), Offset(width * 0.2f, height * 0.25f),
        Offset(width * 0.4f, height * 0.25f), Offset(width * 0.6f, height * 0.25f),
        Offset(width * 0.8f, height * 0.25f), Offset(width * 0.2f, height * 0.4f),
        Offset(width * 0.4f, height * 0.4f), Offset(width * 0.6f, height * 0.4f),
        Offset(width * 0.8f, height * 0.4f), Offset(width * 0.4f, height * 0.55f),
        Offset(width * 0.6f, height * 0.55f)
    )
    homePositions.forEach { drawCircle(Color(0xFFC62828), 12f, it) }
    
    val awayPositions = listOf(
        Offset(width * 0.5f, height * 0.85f), Offset(width * 0.2f, height * 0.75f),
        Offset(width * 0.4f, height * 0.75f), Offset(width * 0.6f, height * 0.75f),
        Offset(width * 0.8f, height * 0.75f), Offset(width * 0.2f, height * 0.6f),
        Offset(width * 0.4f, height * 0.6f), Offset(width * 0.6f, height * 0.6f),
        Offset(width * 0.8f, height * 0.6f), Offset(width * 0.4f, height * 0.45f),
        Offset(width * 0.6f, height * 0.45f)
    )
    awayPositions.forEach { drawCircle(Color(0xFF1565C0), 12f, it) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBall(width: Float, height: Float, position: Vector3, ballHeight: Float) {
    val ballX = width / 2 + position.x * (width / GameState.FIELD_WIDTH)
    val ballY = height / 2 - position.z * (height / GameState.FIELD_LENGTH) - ballHeight * 2
    drawCircle(Color.Black.copy(alpha = 0.3f), 8f, Offset(ballX, height / 2 - position.z * (height / GameState.FIELD_LENGTH)))
    drawCircle(Color.White, 10f, Offset(ballX, ballY))
}

@Composable
fun Scoreboard(
    homeTeamName: String,
    awayTeamName: String,
    homeScore: Int,
    awayScore: Int,
    matchTime: Float,
    currentHalf: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(homeTeamName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(awayTeamName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$homeScore", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Text("-", color = Color.Gray, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Text("$awayScore", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        }
        val minutes = (matchTime / 60).toInt()
        val halfText = if (currentHalf == 1) "上半场" else "下半场"
        Text(text = "$halfText ${String.format("%02d:%02d", minutes, 0)}", color = Color.Yellow, fontSize = 14.sp)
    }
}