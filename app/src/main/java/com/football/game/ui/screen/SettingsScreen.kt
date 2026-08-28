package com.football.game.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    var soundEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
    ) {
        // 顶部栏
        TopAppBar(
            title = {
                Text(
                    text = "设置",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2E7D32)
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 音效设置
            SettingsCard(
                title = "音效",
                items = listOf(
                    SettingsItem(
                        label = "音效",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    ),
                    SettingsItem(
                        label = "背景音乐",
                        checked = musicEnabled,
                        onCheckedChange = { musicEnabled = it }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 操作设置
            SettingsCard(
                title = "操作",
                items = listOf(
                    SettingsItem(
                        label = "震动反馈",
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 关于
            SettingsCard(
                title = "关于",
                items = emptyList()
            )

            Text(
                text = "足球游戏 Kotlin 版 v1.0.0",
                fontSize = 12.sp,
                color = Color(0xFFA5D6A7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * 设置卡片
 */
@Composable
fun SettingsCard(
    title: String,
    items: List<SettingsItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                SettingsRow(item = item)
            }

            if (items.isEmpty()) {
                Text(
                    text = "基于 Godot GDScript 版本重写",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 设置行
 */
@Composable
fun SettingsRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.label,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = item.checked,
            onCheckedChange = item.onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50)
            )
        )
    }
}

/**
 * 设置项数据类
 */
data class SettingsItem(
    val label: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)