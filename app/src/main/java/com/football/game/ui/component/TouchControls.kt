package com.football.game.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.core.GameEngine
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * 触屏控制器（最佳球会 × 实况足球 风格：半透明圆形按键 + 情境出现）
 *
 * 左侧：虚拟摇杆（移动方向）
 * 右侧按键随局势自动出现/切换：
 * - 大按钮三态：加速（按住）⇄ 射门（实况式蓄力：按住蓄力松手出脚）⇄ 铲球（点按：对方持球且贴身）
 * - 传球 / 直塞：己方持球时显示（两回事，各自独立按键）
 * - 解围：本方禁区附近己方持球时显示，大脚踢向前场
 * - 呼叫压位：防守时显示，按住呼叫第二名队友上前逼抢
 * - 切换：防守时显示，手动切到离球最近的队友
 */
@Composable
fun TouchControls(
    gameEngine: GameEngine,
    actionMode: GameEngine.ActionMode,
    hasBall: Boolean,
    showClearance: Boolean,
    defending: Boolean,
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

        // 右侧：解围 + 大动作按钮 + 传球/直塞 或 呼叫压位/切换
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
            } else if (defending) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HoldActionButton(
                        text = "呼叫压位",
                        color = Color(0xFF607D8B),
                        onHold = { gameEngine.callPressing = it }
                    )
                    ActionButton(
                        text = "切换",
                        color = Color(0xFF9C27B0),
                        onClick = { gameEngine.switchToNearest() }
                    )
                }
            }
        }
    }
}

/**
 * 大动作按钮：加速 ⇄ 射门 ⇄ 铲球
 * - 加速：按住生效，松手停止
 * - 射门：实况式蓄力（按住蓄力 1.2s 蓄满，黄色进度弧显示，松手出脚；快点=半力推射）
 * - 铲球：点按即出脚
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

    var chargeActive by remember { mutableStateOf(false) }
    var chargeStart by remember { mutableStateOf(0L) }
    var chargeLevel by remember { mutableStateOf(0f) }

    // 蓄力进度动画（0 → 1，1.2 秒蓄满）
    LaunchedEffect(chargeActive) {
        while (chargeActive) {
            chargeLevel = ((System.currentTimeMillis() - chargeStart) / 1200f).coerceIn(0f, 1f)
            delay(30L)
        }
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
                            GameEngine.ActionMode.SPRINT -> {
                                gameEngine.isSprinting = true
                                tryAwaitRelease()
                                gameEngine.isSprinting = false
                            }
                            GameEngine.ActionMode.SHOOT -> {
                                // 蓄力射门：按住蓄力，松手出脚
                                chargeStart = System.currentTimeMillis()
                                chargeActive = true
                                tryAwaitRelease()
                                chargeActive = false
                                chargeLevel = 0f
                                val heldMs = (System.currentTimeMillis() - chargeStart).coerceAtLeast(0L)
                                val power = (0.5f + 0.5f * (heldMs / 1200f)).coerceIn(0.5f, 1f)
                                gameEngine.doShoot(power)
                            }
                            GameEngine.ActionMode.TACKLE -> {
                                gameEngine.doTackle()
                                tryAwaitRelease()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 蓄力弧（射门按住时显示）
        if (chargeLevel > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawArc(
                    color = Color(0xFFFFEB3B),
                    startAngle = -90f,
                    sweepAngle = 360f * chargeLevel,
                    useCenter = false,
                    style = Stroke(width = 5.dp.toPx())
                )
            }
        }
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
 * 小型情境动作按钮（传球/直塞/解围/切换）：半透明圆形
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
 * 按住生效的小按钮（呼叫压位）：按住 onHold(true)，松手 onHold(false)
 */
@Composable
private fun HoldActionButton(
    text: String,
    color: Color,
    onHold: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHold(true)
                        tryAwaitRelease()
                        onHold(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * HUD 通用圆键（跳过/暂停/换人等）：半透明深色圆形 + 白字
 * enabled=false 时半透明置灰且不可点
 */
@Composable
fun HudCircleButton(
    label: String,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
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
