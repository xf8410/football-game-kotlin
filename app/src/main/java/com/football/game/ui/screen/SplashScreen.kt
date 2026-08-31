package com.football.game.ui.screen

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 启动加载页（软件进入时显示，2.4 秒后自动进入主菜单，点击可跳过）
 *
 * 自定义皮肤：把图片上传到仓库 app/src/main/assets/splash.jpg（或 .png/.webp），
 * 下一次构建自动启用；右下角的"清言·AI生成"水印会被同色米色块无缝盖掉。
 * 没有 assets 图片时显示内置的米色程序化启动页。
 */
private val SplashBeige = Color(0xFFE9DFC1)

@Composable
fun SplashScreen(
    title: String = "足球游戏",
    subtitle: String = "最佳球会 × 实况风格",
    onDone: () -> Unit = {}
) {
    val context = LocalContext.current
    var splashBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var appeared by remember { mutableStateOf(false) }
    val fade by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "splashFade"
    )

    LaunchedEffect(Unit) {
        // 依次尝试 assets 里的自定义启动图
        for (name in listOf("splash.jpg", "splash.png", "splash.jpeg", "splash.webp")) {
            try {
                context.assets.open(name).use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        splashBitmap = bmp
                        break
                    }
                }
            } catch (_: Throwable) {
                // 没有该文件 → 继续尝试下一个
            }
        }
        appeared = true
        delay(2400L)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBeige)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDone() }
    ) {
        val bmp = splashBitmap
        if (bmp != null) {
            // ===== 自定义启动图（用户上传）=====
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fade)
            )
            // 盖掉右下角"清言·AI生成"水印（背景是纯米色，同色色块无缝覆盖）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.24f)
                    .fillMaxHeight(0.09f)
                    .background(SplashBeige)
            )
        } else {
            // ===== 内置程序化启动页 =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fade)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.42f))

                Text(text = "⚽", fontSize = 72.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3E2C1B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = Color(0xFF8A7A5C)
                )

                Spacer(modifier = Modifier.weight(0.30f))

                LinearProgressIndicator(
                    progress = { fade },
                    modifier = Modifier
                        .width(180.dp)
                        .height(4.dp),
                    color = Color(0xFF3E2C1B),
                    trackColor = Color(0xFFD9CBA6)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "正在载入绿茵场…",
                    fontSize = 12.sp,
                    color = Color(0xFF8A7A5C)
                )

                Spacer(modifier = Modifier.weight(0.16f))
            }

            Text(
                text = "点击任意位置进入",
                fontSize = 11.sp,
                color = Color(0xFFA5987A),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
        }
    }
}
