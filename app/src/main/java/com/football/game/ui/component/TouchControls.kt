package com.football.game.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.core.GameEngine
import kotlin.math.sqrt

/**
 * 触屏控制器（最佳球会风格：半透明圆形按键 + 情境出现）
 *
 * 左侧：虚拟摇杆（移动方向）
 * 右侧：大动作按钮 + 情境按键，按键随局势自动出现/切换：
 * - 大按钮三态：加速（按住）⇄ 射门（点按：进入射门范围或刚接住传球）⇄ 铲球（点按：对方持球且贴身）
 * - 传球 / 直塞：己方持球时显示（两回事，各自独立按键）
 * - 解围：本方禁区附近己方持球时显示，大脚踢向前场
 */
@Composable
fun TouchControls(
    gameEngine: GameEngine,
    actionMode: GameEngine.ActionMode,
    hasBall: Boolean,
    showClearance: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 左侧虚拟摇杆
        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
                .size(150.dp),
            onOffsetChange = { offset ->
                // 转换为游戏输入
                val maxLength = 75f  // 摇杆最大半径
                val normalizedX = offset.x / maxLength
                val normalizedY = offset.y / maxLength
                gameEngine.inputVector = GameEngine.Vector2D(normalizedX, normalizedY)
            }
        )

        // 右侧：解围 + 大动作按钮 + 传球/直塞
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (showClearance) {
                ActionButton(
                    text = "解围",
                    color = Color(0xFF8D6E63),
                    onClick = { gameEngine.doClearance() }
                )
            }
            ContextActionButton(
                gameEngine = gameEngine,
                mode = actionMode
            )
            if (hasBall) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(
                        text = "传球",
                        color = Color(0xFF4CAF50),
                        onClick = { gameEngine.doPass() }
                    )
                    ActionButton(
                        text = "直塞",
                        color = Color(0xFF2196F3),
                        onClick = { gameEngine.doThroughBall() }
                    )
                }
            }
        }
    }
}

/**
 * 大动作按钮：加速 ⇄ 射门 ⇄ 铲球
 * 半透明深色圆键 + 模式色描边；按下即触发：射门/铲球点按生效，加速按住生效、松手停止
 */
@Composable
private fun ContextActionButton(
    gameEngine: GameEngine,
    mode: GameEngine.ActionMode
) {
    // pointerInput(Unit) 的手势回调是长驻协程，用 rememberUpdatedState 读取最新模式
    val currentMode by rememberUpdatedState(mode)
    val (label, color) = when (mode) {
        GameEngine.ActionMode.SPRINT -> "加速" to Color(0xFFFF9800)
        GameEngine.ActionMode.SHOOT -> "射门" to Color(0xFFF44336)
        GameEngine.ActionMode.TACKLE -> "铲球" to Color(0xFF3F51B5)
    }

    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(2.5.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        when (currentMode) {
                            GameEngine.ActionMode.SPRINT -> gameEngine.isSprinting = true
                            GameEngine.ActionMode.SHOOT -> gameEngine.doShoot()
                            GameEngine.ActionMode.TACKLE -> gameEngine.doTackle()
                        }
                        tryAwaitRelease()
                        gameEngine.isSprinting = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 虚拟摇杆
 */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onOffsetChange: (Offset) -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 60f

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        center = offset
                    },
                    onDrag = { change, _ ->
                        val dragOffset = change.position - center
                        val distance = sqrt(dragOffset.x * dragOffset.x + dragOffset.y * dragOffset.y)

                        if (distance <= maxRadius) {
                            knobOffset = dragOffset
                        } else {
                            // 限制在圆内
                            val ratio = maxRadius / distance
                            knobOffset = Offset(dragOffset.x * ratio, dragOffset.y * ratio)
                        }

                        onOffsetChange(knobOffset)
                        change.consume()
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onOffsetChange(Offset.Zero)
                    }
                )
            }
    ) {
        // 外圈
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = maxRadius + 20f,
            center = center
        )

        // 内圈
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = maxRadius,
            center = center
        )

        // 摇杆
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = 30f,
            center = center + knobOffset
        )
    }
}

/**
 * 小型情境动作按钮（传球/直塞/解围）：半透明圆形
 */
@Composable
fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(66.dp)
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.55f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * HUD 通用圆键（倍速/暂停/换人等）：半透明深色圆形 + 白字
 */
@Composable
fun HudCircleButton(
    label: String,
    size: Dp = 44.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
