package com.football.game.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.football.game.core.GameState
import com.football.game.core.Vector3
import com.football.game.model.Player
import com.football.game.ui.component.TouchControls

/**
 * 比赛屏幕
 */
@Composable
fun MatchScreen(
    homeTeamName: String = "红队",
    awayTeamName: String = "蓝队",
    onMatchEnd: () -> Unit = {}
) {
    // 比赛状态
    var matchTime by remember { mutableFloatStateOf(0f) }
    var homeScore by remember { mutableIntStateOf(0) }
    var awayScore by remember { mutableIntStateOf(0) }
    var currentHalf by remember { mutableIntStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    
    // 球员位置（简化）
    var ballPosition by remember { mutableStateOf(Vector3.ZERO) }
    var ballHeight by remember { mutableFloatStateOf(0f) }
    var hasBall by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))  // 球场绿色
    ) {
        // 3D 球场渲染区域
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // 绘制球场
            drawField(canvasWidth, canvasHeight)
            
            // 绘制球员（简化表示）
            drawPlayers(canvasWidth, canvasHeight)
            
            // 绘制球
            drawBall(canvasWidth, canvasHeight, ballPosition, ballHeight)
        }
        
        // 计分板
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
        
        // 触屏控制器
        TouchControls(
            gameEngine = remember { com.football.game.core.GameEngine(
                match = com.football.game.model.Match(
                    homeTeam = com.football.game.model.Team(id = "home", name = homeTeamName),
                    awayTeam = com.football.game.model.Team(id = "away", name = awayTeamName)
                ),
                homePlayers = emptyList(),
                awayPlayers = emptyList()
            )},
            hasBall = hasBall,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 绘制球场
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawField(
    width: Float,
    height: Float
) {
    // 球场边线
    val fieldLeft = width * 0.05f
    val fieldRight = width * 0.95f
    val fieldTop = height * 0.1f
    val fieldBottom = height * 0.9f
    
    // 边线
    drawLine(
        color = Color.White,
        start = Offset(fieldLeft, fieldTop),
        end = Offset(fieldRight, fieldTop),
        strokeWidth = 3f
    )
    drawLine(
        color = Color.White,
        start = Offset(fieldLeft, fieldBottom),
        end = Offset(fieldRight, fieldBottom),
        strokeWidth = 3f
    )
    drawLine(
        color = Color.White,
        start = Offset(fieldLeft, fieldTop),
        end = Offset(fieldLeft, fieldBottom),
        strokeWidth = 3f
    )
    drawLine(
        color = Color.White,
        start = Offset(fieldRight, fieldTop),
        end = Offset(fieldRight, fieldBottom),
        strokeWidth = 3f
    )
    
    // 中线
    drawLine(
        color = Color.White,
        start = Offset(width / 2, fieldTop),
        end = Offset(width / 2, fieldBottom),
        strokeWidth = 3f
    )
    
    // 中圈
    drawCircle(
        color = Color.White,
        radius = height * 0.1f,
        center = Offset(width / 2, height / 2),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
    
    // 禁区（简化）
    val penaltyWidth = width * 0.2f
    val penaltyHeight = height * 0.15f
    
    // 主队禁区
    drawRect(
        color = Color.White,
        topLeft = Offset(width / 2 - penaltyWidth / 2, fieldTop),
        size = androidx.compose.ui.geometry.Size(penaltyWidth, penaltyHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
    
    // 客队禁区
    drawRect(
        color = Color.White,
        topLeft = Offset(width / 2 - penaltyWidth / 2, fieldBottom - penaltyHeight),
        size = androidx.compose.ui.geometry.Size(penaltyWidth, penaltyHeight),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
    )
}

/**
 * 绘制球员（简化）
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlayers(
    width: Float,
    height: Float
) {
    // 主队球员（红色）
    val homePositions = listOf(
        Offset(width * 0.5f, height * 0.15f),   // GK
        Offset(width * 0.2f, height * 0.25f),   // LB
        Offset(width * 0.4f, height * 0.25f),   // CB
        Offset(width * 0.6f, height * 0.25f),   // CB
        Offset(width * 0.8f, height * 0.25f),   // RB
        Offset(width * 0.2f, height * 0.4f),    // LM
        Offset(width * 0.4f, height * 0.4f),    // CM
        Offset(width * 0.6f, height * 0.4f),    // CM
        Offset(width * 0.8f, height * 0.4f),    // RM
        Offset(width * 0.4f, height * 0.55f),   // ST
        Offset(width * 0.6f, height * 0.55f)    // ST
    )
    
    homePositions.forEach { pos ->
        drawCircle(
            color = Color(0xFFC62828),
            radius = 12f,
            center = pos
        )
    }
    
    // 客队球员（蓝色）
    val awayPositions = listOf(
        Offset(width * 0.5f, height * 0.85f),   // GK
        Offset(width * 0.2f, height * 0.75f),   // LB
        Offset(width * 0.4f, height * 0.75f),   // CB
        Offset(width * 0.6f, height * 0.75f),   // CB
        Offset(width * 0.8f, height * 0.75f),   // RB
        Offset(width * 0.2f, height * 0.6f),    // LM
        Offset(width * 0.4f, height * 0.6f),    // CM
        Offset(width * 0.6f, height * 0.6f),    // CM
        Offset(width * 0.8f, height * 0.6f),    // RM
        Offset(width * 0.4f, height * 0.45f),   // ST
        Offset(width * 0.6f, height * 0.45f)    // ST
    )
    
    awayPositions.forEach { pos ->
        drawCircle(
            color = Color(0xFF1565C0),
            radius = 12f,
            center = pos
        )
    }
}

/**
 * 绘制球
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBall(
    width: Float,
    height: Float,
    position: Vector3,
    ballHeight: Float
) {
    // 简化：球在2D平面上的位置
    val ballX = width / 2 + position.x * (width / GameState.FIELD_WIDTH)
    val ballY = height / 2 - position.z * (height / GameState.FIELD_LENGTH) - ballHeight * 2
    
    // 球的阴影
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = 8f,
        center = Offset(ballX, height / 2 - position.z * (height / GameState.FIELD_LENGTH))
    )
    
    // 球
    drawCircle(
        color = Color.White,
        radius = 10f,
        center = Offset(ballX, ballY)
    )
}

/**
 * 计分板
 */
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
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 队名
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = homeTeamName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = awayTeamName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 比分
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$homeScore",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "-",
                color = Color.Gray,
                fontSize = 24.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "$awayScore",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        
        // 时间
        val minutes = (matchTime / 60).toInt()
        val seconds = (matchTime % 60).toInt()
        val halfText = if (currentHalf == 1) "上半场" else "下半场"
        
        Text(
            text = "$halfText ${String.format("%02d:%02d", minutes, seconds)}",
            color = Color.Yellow,
            fontSize = 14.sp
        )
    }
}