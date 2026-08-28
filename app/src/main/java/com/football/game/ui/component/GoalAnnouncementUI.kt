package com.football.game.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.core.GoalAnnouncement
import com.football.game.core.GoalTypes
import kotlinx.coroutines.delay

/**
 * 进球播报UI组件
 * 显示在左上角
 */
@Composable
fun GoalAnnouncementUI(
    announcement: GoalAnnouncement?,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var currentAnnouncement by remember { mutableStateOf<GoalAnnouncement?>(null) }
    
    LaunchedEffect(announcement) {
        if (announcement != null) {
            currentAnnouncement = announcement
            isVisible = true
            delay((announcement.getDisplayDuration() * 1000).toLong())
            isVisible = false
        } else {
            isVisible = false
        }
    }
    
    AnimatedVisibility(
        visible = isVisible && currentAnnouncement != null,
        modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        currentAnnouncement?.let { ann ->
            GoalAnnouncementCard(announcement = ann)
        }
    }
}

/**
 * 进球播报卡片
 */
@Composable
fun GoalAnnouncementCard(
    announcement: GoalAnnouncement,
    modifier: Modifier = Modifier
) {
    val isSpecial = announcement.goalCount >= 2
    
    Box(
        modifier = modifier
            .fillMaxWidth(0.4f)
            .clip(RoundedCornerShape(bottomEnd = 12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
            .border(
                width = if (isSpecial) 2.dp else 0.dp,
                color = if (isSpecial) {
                    when (announcement.goalCount) {
                        4, 5, 6, 7 -> Color(0xFFFFD700)  // 金色
                        3 -> Color(0xFFFF6B6B)            // 红色
                        else -> Color(0xFF4ECDC4)         // 青色
                    }
                } else Color.Transparent,
                shape = RoundedCornerShape(bottomEnd = 12.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            // 主文本（进球者 + 时间）
            Text(
                text = announcement.mainText,
                fontSize = if (isSpecial) 20.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 子文本（进球方式）
            Text(
                text = announcement.subText,
                fontSize = 14.sp,
                color = Color(0xFFA5D6A7),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 比分
            Text(
                text = announcement.scoreText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Yellow
            )
        }
    }
}

/**
 * 帽子戏法/大四喜 专用播报
 */
@Composable
fun SpecialGoalAnnouncement(
    announcement: GoalAnnouncement,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(announcement) {
        isVisible = true
        delay(4000)
        isVisible = false
    }
    
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        val gradientColors = when (announcement.goalCount) {
            4, 5, 6, 7 -> listOf(
                Color(0xFFFFD700),
                Color(0xFFFFA000),
                Color(0xFFFF6F00)
            )
            3 -> listOf(
                Color(0xFFFF6B6B),
                Color(0xFFE53935),
                Color(0xFFC62828)
            )
            else -> listOf(
                Color(0xFF4ECDC4),
                Color(0xFF26A69A),
                Color(0xFF00897B)
            )
        }
        
        Box(
            modifier = modifier
                .fillMaxWidth(0.35f)
                .clip(RoundedCornerShape(bottomEnd = 16.dp))
                .background(
                    Brush.verticalGradient(colors = gradientColors)
                )
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 特殊图标
                Text(
                    text = when (announcement.goalCount) {
                        3 -> "🎩"
                        4 -> "🌟"
                        5 -> "👑"
                        else -> "⭐"
                    },
                    fontSize = 36.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 术语
                Text(
                    text = announcement.countTerm.term,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 球员名
                Text(
                    text = announcement.mainText.split("\n").first().removePrefix("⚽ 进球！"),
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                // 描述
                Text(
                    text = announcement.countTerm.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 进球方式图标
 */
@Composable
fun GoalMethodIcon(
    method: GoalTypes.GoalMethod,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (method) {
        // 头球类
        GoalTypes.GoalMethod.HEADER_NORMAL,
        GoalTypes.GoalMethod.HEADER_POWERFUL,
        GoalTypes.GoalMethod.HEADER_DIVING,
        GoalTypes.GoalMethod.HEADER_REDIRECT,
        GoalTypes.GoalMethod.HEADER_GLANCING,
        GoalTypes.GoalMethod.CORNER_HEADER -> "⚽" to Color(0xFF8BC34A)
        
        // 特殊射门
        GoalTypes.GoalMethod.SHOT_CURLED -> "🌀" to Color(0xFF9C27B0)
        GoalTypes.GoalMethod.SHOT_KNUCKLE -> "💨" to Color(0xFF2196F3)
        GoalTypes.GoalMethod.SHOT_CHIP -> "⬆️" to Color(0xFF00BCD4)
        GoalTypes.GoalMethod.SHOT_BICYCLE,
        GoalTypes.GoalMethod.SHOT_OVERHEAD -> "🤸" to Color(0xFFFF9800)
        GoalTypes.GoalMethod.SHOT_VOLLEY,
        GoalTypes.GoalMethod.SHOT_HALF_VOLLEY -> "🚀" to Color(0xFFF44336)
        GoalTypes.GoalMethod.SHOT_BACKHEEL -> "🦶" to Color(0xFFE91E63)
        
        // 位置
        GoalTypes.GoalMethod.SHOT_FAR_POST,
        GoalTypes.GoalMethod.LONG_SHOT -> "🎯" to Color(0xFFFF5722)
        
        // 配合
        GoalTypes.GoalMethod.SHOT_CROSS,
        GoalTypes.GoalMethod.SHOT_CUTBACK,
        GoalTypes.GoalMethod.SHOT_LAYOFF -> "🤝" to Color(0xFF4CAF50)
        
        // 特殊
        GoalTypes.GoalMethod.PANENKA -> "🥄" to Color(0xFFFFD700)
        GoalTypes.GoalMethod.SOLO_GOAL -> "🏃" to Color(0xFFE91E63)
        
        else -> "⚽" to Color.White
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
    }
}

/**
 * 进球统计条
 */
@Composable
fun GoalStatsBar(
    goalMethod: GoalTypes.GoalMethod,
    excitement: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GoalMethodIcon(method = goalMethod)
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goalMethod.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = goalMethod.description,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
        
        // 精彩程度
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "精彩度",
                fontSize = 10.sp,
                color = Color.Gray
            )
            Text(
                text = "${(excitement * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    excitement > 0.8f -> Color(0xFFFF6B6B)
                    excitement > 0.5f -> Color(0xFFFFC107)
                    else -> Color(0xFF4CAF50)
                }
            )
        }
    }
}