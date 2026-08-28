package com.football.game.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.football.game.core.GameEngine
import kotlin.math.sqrt

/**
 * 触屏控制器组件
 * 左侧：虚拟摇杆
 * 右侧：动作按钮
 */
@Composable
fun TouchControls(
    gameEngine: GameEngine,
    hasBall: Boolean,
    modifier: Modifier = Modifier
) {
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }
    var isSprinting by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // 左侧虚拟摇杆
        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp)
                .size(150.dp),
            onOffsetChange = { offset ->
                joystickOffset = offset
                // 转换为游戏输入
                val maxLength = 75f  // 摇杆最大半径
                val normalizedX = offset.x / maxLength
                val normalizedY = offset.y / maxLength
                gameEngine.inputVector = GameEngine.Vector2D(normalizedX, normalizedY)
            }
        )

        // 右侧动作按钮
        ActionButtons(
            hasBall = hasBall,
            onPassClick = { gameEngine.doPass() },
            onShootClick = { gameEngine.doShoot() },
            onThroughBallClick = { gameEngine.doThroughBall() },
            onSprintClick = {
                isSprinting = !isSprinting
                gameEngine.isSprinting = isSprinting
            },
            onSwitchClick = { gameEngine.switchPlayer() },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp)
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
            color = Color.White.copy(alpha = 0.3f),
            radius = maxRadius + 20f,
            center = center
        )

        // 内圈
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = maxRadius,
            center = center
        )

        // 摇杆
        drawCircle(
            color = Color.White,
            radius = 30f,
            center = center + knobOffset
        )
    }
}

/**
 * 动作按钮
 */
@Composable
fun ActionButtons(
    hasBall: Boolean,
    onPassClick: () -> Unit,
    onShootClick: () -> Unit,
    onThroughBallClick: () -> Unit,
    onSprintClick: () -> Unit,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (hasBall) {
            // 有球时的按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "传球",
                    color = Color(0xFF4CAF50),
                    onClick = onPassClick
                )
                ActionButton(
                    text = "直塞",
                    color = Color(0xFF2196F3),
                    onClick = onThroughBallClick
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "射门",
                    color = Color(0xFFF44336),
                    onClick = onShootClick
                )
                ActionButton(
                    text = "冲刺",
                    color = Color(0xFFFF9800),
                    onClick = onSprintClick
                )
            }
        } else {
            // 无球时的按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton(
                    text = "切换",
                    color = Color(0xFF9C27B0),
                    onClick = onSwitchClick
                )
                ActionButton(
                    text = "冲刺",
                    color = Color(0xFFFF9800),
                    onClick = onSprintClick
                )
            }
        }
    }
}

/**
 * 单个动作按钮
 */
@Composable
fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(70.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}