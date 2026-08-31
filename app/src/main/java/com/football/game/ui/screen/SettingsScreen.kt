package com.football.game.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.football.game.data.GitHubUploader
import kotlinx.coroutines.launch

/**
 * 设置屏幕
 * 新增：GitHub 直传面板 —— 在 App 内选图 → 上传启动图 → 触发构建新版，
 * 全程不需要电脑和网页操作（需一次性粘贴自己的 GitHub Token）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("github_prefs", Context.MODE_PRIVATE) }

    var soundEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }

    // ===== GitHub 直传状态 =====
    var token by remember { mutableStateOf(prefs.getString("pat", "") ?: "") }
    var preparedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var ghStatus by remember { mutableStateOf("未上传过启动图（当前使用内置海报启动页）") }
    var ghBusy by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = GitHubUploader.prepareImageBytes(context, uri)
            preparedBytes = bytes
            previewBitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            ghStatus = if (bytes != null) {
                "已选择图片（${bytes.size / 1024} KB）→ 点\"上传启动图\"提交到仓库"
            } else {
                "图片读取失败，请换一张试试"
            }
        }
    }

    fun saveToken() {
        prefs.edit().putString("pat", token.trim()).apply()
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
                .verticalScroll(rememberScrollState())
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

            // ===== GitHub 直传面板 =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2C17)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🎨 自定义启动图（App 内直传）",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "把 AI 生成的球星海报设为软件启动页：选图 → 上传 → 构建新版，全程在 App 里完成。",
                        fontSize = 12.sp,
                        color = Color(0xFF9BB8A4)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 第 1 步：Token
                    Text(
                        text = "第 1 步（一次性）· 粘贴 GitHub Token",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点这里打开 Token 生成页（已自动勾选 repo + workflow 权限）→ 生成后复制 → 粘贴到下面。Token 只存在你手机里。",
                        fontSize = 11.sp,
                        color = Color(0xFF9BB8A4),
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, GitHubUploader.TOKEN_URL.toUri())
                                )
                            } catch (_: Throwable) {
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("粘贴 GitHub Token（ghp_ 开头）", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    ghBusy = true
                                    saveToken()
                                    ghStatus = GitHubUploader.verifyToken(token.trim()).message
                                    ghBusy = false
                                }
                            },
                            enabled = token.isNotBlank() && !ghBusy
                        ) {
                            Text("校验 Token", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 第 2 步：选图
                    Text(
                        text = "第 2 步 · 选择启动图",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { pickImage.launch("image/*") },
                            enabled = !ghBusy
                        ) {
                            Text("从相册选图", fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 第 3 步：上传 + 构建
                    Text(
                        text = "第 3 步 · 上传并出新版",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    ghBusy = true
                                    saveToken()
                                    val bytes = preparedBytes
                                    ghStatus = if (bytes == null) {
                                        "请先选择图片"
                                    } else {
                                        GitHubUploader.uploadSplashImage(token.trim(), bytes).message
                                    }
                                    ghBusy = false
                                }
                            },
                            enabled = preparedBytes != null && token.isNotBlank() && !ghBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Text("上传启动图", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    ghBusy = true
                                    saveToken()
                                    ghStatus = GitHubUploader.triggerBuild(token.trim()).message
                                    ghBusy = false
                                }
                            },
                            enabled = token.isNotBlank() && !ghBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC107),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("构建新版 APK", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (ghBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = ghStatus,
                        fontSize = 12.sp,
                        color = Color(0xFFFFD54F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 关于
            SettingsCard(
                title = "关于",
                items = emptyList()
            )

            Text(
                text = "足球游戏 Kotlin 版 v1.0.10",
                fontSize = 12.sp,
                color = Color(0xFFA5D6A7),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center
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
