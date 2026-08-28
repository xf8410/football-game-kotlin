package com.football.game.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 主菜单屏幕
 */
@Composable
fun MainMenuScreen(
    onQuickMatch: () -> Unit = {},
    onLeagueMode: () -> Unit = {},
    onCupMode: () -> Unit = {},
    onSettings: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))  // 深绿色背景
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 标题
            Text(
                text = "⚽",
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "足球游戏",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Kotlin Android 版",
                fontSize = 18.sp,
                color = Color(0xFFA5D6A7),
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // 菜单按钮
            MenuButton(
                text = "快速比赛",
                color = Color(0xFF4CAF50),
                onClick = onQuickMatch
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuButton(
                text = "联赛模式",
                color = Color(0xFF2196F3),
                onClick = onLeagueMode
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuButton(
                text = "杯赛模式",
                color = Color(0xFFFF9800),
                onClick = onCupMode
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuButton(
                text = "设置",
                color = Color(0xFF607D8B),
                onClick = onSettings
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MenuButton(
                text = "退出",
                color = Color(0xFFF44336),
                onClick = onExit
            )
        }
    }
}

/**
 * 菜单按钮
 */
@Composable
fun MenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}