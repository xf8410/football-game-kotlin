package com.football.game.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

/**
 * 3D 模型发型枚举（低多边形程序化模型用）
 */
enum class HairStyle3D {
    NONE,       // 光头
    BUZZ,       // 寸头
    SHORT,      // 短发
    FLOW,       // 飘逸中分长发（哈兰德）
    AFRO,       // 爆炸头（古利特）
    CURLY,      // 卷发（萨拉赫/皮尔洛）
    PONYTAIL    // 马尾辫（巴乔）
}

/**
 * 队服花纹（3D 渲染器按此绘制球衣）
 */
enum class KitPattern {
    SOLID,      // 纯色（副色 = 袖子）
    STRIPES,    // 竖条纹（巴萨/国米/尤文/AC米兰/马竞）
    HOOPS,      // 横条纹
    HALF,       // 左右拼色
    SASH        // 斜杠（巴黎）
}

/**
 * 3D 球员外观描述
 * 颜色使用 ARGB Int，方便 OpenGL 渲染器直接使用
 */
data class PlayerLook(
    val kitColor1: Int,   // 球衣主色
    val kitColor2: Int,   // 球衣副色（袖子/花纹）
    val skinColor: Int,   // 肤色
    val hairColor: Int,   // 发色
    val hairStyle3D: HairStyle3D = HairStyle3D.SHORT,
    val shortsColor: Int = kitColor2,   // 球裤颜色
    val socksColor: Int = kitColor2,    // 球袜颜色
    val pattern: KitPattern = KitPattern.SOLID,
    val isGoalkeeper: Boolean = false
)

/** 发型（2D 头像用） */
enum class AvatarHairStyle {
    SHORT,     // 短发
    SLICK,     // 油头/背头
    BUZZ,      // 寸头
    FLOW,      // 中分飘逸（哈兰德）
    AFRO,      // 爆炸头（古利特）
    CURLY,     // 卷发（皮尔洛/萨拉赫）
    LONG,      // 长发（巴蒂斯图塔）
    PONYTAIL,  // 马尾辫（巴乔）
    MOHAWK,    // 莫西干（内马尔）
    BUN,       // 丸子头
    HEADBAND,  // 发带（贝克汉姆）
    BALD       // 光头（齐达内）
}

/** 胡须（2D 头像用） */
enum class AvatarFacialHair {
    NONE, STUBBLE, MOUSTACHE, GOATEE, FULL_BEARD
}

/**
 * 球星头像参数：程序化卡通肖像
 * 通过 肤色/发型/胡须/眉形/脸宽 组合出球星辨识度
 */
data class StarAvatarParams(
    val skinColor: Color = Color(0xFFE8B58B),
    val hairColor: Color = Color(0xFF2B1B12),
    val hairStyle: AvatarHairStyle = AvatarHairStyle.SHORT,
    val facialHair: AvatarFacialHair = AvatarFacialHair.NONE,
    val browThickness: Float = 1.0f,
    val faceWidth: Float = 1.0f,          // 0.9 ~ 1.1
    val kitColor1: Color = Color(0xFF2E7D32),
    val kitColor2: Color = Color.White,
    val backdropColor: Color = Color(0xFFE8F5E9),
    val headbandColor: Color = Color.White
)

/**
 * 程序化球星头像
 * Canvas 绘制：背景 → 肩部球衣 → 领口 → 脖子 → 头/耳 → 五官 → 胡须 → 头发 → 号码
 */
@Composable
fun StarAvatarView(
    params: StarAvatarParams,
    modifier: Modifier = Modifier,
    showNumber: Int? = null
) {
    Canvas(modifier = modifier) {
        drawStarAvatar(params, showNumber)
    }
}

private fun DrawScope.drawStarAvatar(params: StarAvatarParams, number: Int?) {
    val scale = size.minDimension / 100f
    val originX = (size.width - 100f * scale) / 2f
    val originY = (size.height - 100f * scale) / 2f

    fun X(v: Float) = originX + v * scale
    fun Y(v: Float) = originY + v * scale
    fun R(v: Float) = v * scale

    val headCx = 50f
    val headCy = 41f
    val headRx = 21f * params.faceWidth
    val headRy = 23f

    fun headRect(pad: Float = 0f): Rect = Rect(
        X(headCx - headRx - pad), Y(headCy - headRy - pad),
        X(headCx + headRx + pad), Y(headCy + headRy + pad)
    )

    // ==================== 背景 ====================
    drawCircle(color = params.backdropColor, radius = R(48f), center = Offset(X(50f), Y(52f)))

    // ==================== 脖子 ====================
    drawRect(
        color = params.skinColor,
        topLeft = Offset(X(43.5f), Y(54f)),
        size = Size(R(13f), R(20f))
    )

    // ==================== 球衣肩部 ====================
    val kitPath = Path().apply {
        moveTo(X(13f), Y(102f))
        cubicTo(X(16f), Y(74f), X(32f), Y(64f), X(50f), Y(64f))
        cubicTo(X(68f), Y(64f), X(84f), Y(74f), X(87f), Y(102f))
        close()
    }
    drawPath(kitPath, params.kitColor1)

    // V 字领口
    val collarPath = Path().apply {
        moveTo(X(42.5f), Y(65.5f))
        lineTo(X(50f), Y(74f))
        lineTo(X(57.5f), Y(65.5f))
        close()
    }
    drawPath(collarPath, params.kitColor2)

    // ==================== 后层头发 ====================
    val hair = params.hairColor
    when (params.hairStyle) {
        AvatarHairStyle.FLOW -> {
            val r = headRect(2.5f)
            drawOval(
                color = hair,
                topLeft = Offset(r.left, r.top - R(3f)),
                size = Size(r.width, r.height * 0.85f)
            )
        }
        AvatarHairStyle.AFRO ->
            drawCircle(hair, R(26f), Offset(X(50f), Y(26f)))
        AvatarHairStyle.CURLY ->
            listOf(Triple(30f, 26f, 9f), Triple(40f, 18f, 9f), Triple(50f, 15f, 10f),
                Triple(60f, 18f, 9f), Triple(70f, 26f, 9f)).forEach { (cx, cy, cr) ->
                drawCircle(hair, R(cr), Offset(X(cx), Y(cy)))
            }
        AvatarHairStyle.LONG ->
            drawOval(hair, topLeft = Offset(X(23f), Y(16f)), size = Size(R(54f), R(70f)))
        AvatarHairStyle.PONYTAIL -> {
            drawCircle(hair, R(8f), Offset(X(69f), Y(30f)))
            drawOval(hair, topLeft = Offset(X(66f), Y(32f)), size = Size(R(9f), R(20f)))
        }
        else -> {}
    }

    // ==================== 耳朵 + 头 ====================
    drawCircle(params.skinColor, R(3.6f), Offset(X(headCx - headRx), Y(headCy + 2f)))
    drawCircle(params.skinColor, R(3.6f), Offset(X(headCx + headRx), Y(headCy + 2f)))
    drawOval(params.skinColor, topLeft = headRect().topLeft, size = headRect().size)

    // ==================== 五官 ====================
    // 眼白 + 瞳孔
    drawCircle(Color.White, R(3.1f), Offset(X(41.5f), Y(39f)))
    drawCircle(Color.White, R(3.1f), Offset(X(58.5f), Y(39f)))
    drawCircle(Color(0xFF263238), R(1.6f), Offset(X(42.3f), Y(39.3f)))
    drawCircle(Color(0xFF263238), R(1.6f), Offset(X(57.7f), Y(39.3f)))

    // 眉毛
    val browColor = lerp(hair, Color.Black, 0.3f)
    drawRect(
        color = browColor,
        topLeft = Offset(X(37.8f), Y(33.4f)),
        size = Size(R(7.4f), R(1.5f * params.browThickness))
    )
    drawRect(
        color = browColor,
        topLeft = Offset(X(54.8f), Y(33.4f)),
        size = Size(R(7.4f), R(1.5f * params.browThickness))
    )

    // 鼻子
    val nosePath = Path().apply {
        moveTo(X(50f), Y(40.5f))
        lineTo(X(48.2f), Y(47.5f))
        lineTo(X(51.8f), Y(47.5f))
        close()
    }
    drawPath(nosePath, lerp(params.skinColor, Color.Black, 0.25f))

    // ==================== 胡须底层 ====================
    when (params.facialHair) {
        AvatarFacialHair.STUBBLE ->
            drawArc(
                color = lerp(hair, Color.Black, 0.2f).copy(alpha = 0.30f),
                startAngle = 12f, sweepAngle = 156f, useCenter = true,
                topLeft = headRect(0.5f).topLeft, size = headRect(0.5f).size
            )
        AvatarFacialHair.FULL_BEARD -> {
            drawArc(
                color = hair,
                startAngle = 15f, sweepAngle = 150f, useCenter = true,
                topLeft = headRect(1.5f).topLeft, size = headRect(1.5f).size
            )
            // 嘴周留出皮肤
            drawCircle(params.skinColor, R(4.4f), Offset(X(50f), Y(52.5f)))
        }
        AvatarFacialHair.GOATEE ->
            drawOval(hair, topLeft = Offset(X(45.5f), Y(53.5f)), size = Size(R(9f), R(8f)))
        else -> {}
    }

    // ==================== 嘴（微笑） ====================
    drawArc(
        color = Color(0xFF8D4A3A),
        startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(X(44.5f), Y(49.5f)), size = Size(R(11f), R(7f)),
        style = Stroke(width = R(1.4f))
    )

    // ==================== 胡须上层（胡子/山羊胡） ====================
    if (params.facialHair == AvatarFacialHair.MOUSTACHE ||
        params.facialHair == AvatarFacialHair.GOATEE ||
        params.facialHair == AvatarFacialHair.FULL_BEARD
    ) {
        drawRoundRect(
            color = hair,
            topLeft = Offset(X(44.8f), Y(48.2f)),
            size = Size(R(10.4f), R(2.4f)),
            cornerRadius = CornerRadius(R(1.2f), R(1.2f))
        )
    }

    // ==================== 前层头发 ====================
    when (params.hairStyle) {
        AvatarHairStyle.SHORT ->
            drawArc(hair, 180f, 180f, true, headRect(1.6f).topLeft, headRect(1.6f).size)
        AvatarHairStyle.SLICK -> {
            drawArc(hair, 185f, 175f, true, headRect(1.2f).topLeft, headRect(1.2f).size)
            drawLine(
                Color.White.copy(alpha = 0.30f),
                Offset(X(44f), Y(21.5f)), Offset(X(56f), Y(21.5f)),
                strokeWidth = R(1.2f)
            )
        }
        AvatarHairStyle.BUZZ ->
            drawArc(hair.copy(alpha = 0.85f), 190f, 160f, true, headRect(0.6f).topLeft, headRect(0.6f).size)
        AvatarHairStyle.FLOW -> {
            // 额前中分线 + 鬓角
            drawLine(
                lerp(hair, Color.Black, 0.4f),
                Offset(X(50f), Y(18.5f)), Offset(X(50f), Y(26f)),
                strokeWidth = R(1.1f)
            )
            drawRect(hair, Offset(X(headCx - headRx - 1.5f), Y(36f)), Size(R(3f), R(9f)))
            drawRect(hair, Offset(X(headCx + headRx - 1.5f), Y(36f)), Size(R(3f), R(9f)))
        }
        AvatarHairStyle.AFRO ->
            drawCircle(hair, R(10f), Offset(X(50f), Y(20f)))
        AvatarHairStyle.CURLY ->
            listOf(Pair(36f, 22f), Pair(46f, 19.5f), Pair(56f, 19.5f), Pair(64f, 22f)).forEach { (cx, cy) ->
                drawCircle(hair, R(5.5f), Offset(X(cx), Y(cy)))
            }
        AvatarHairStyle.LONG ->
            drawArc(hair, 185f, 170f, true, headRect(1.2f).topLeft, headRect(1.2f).size)
        AvatarHairStyle.PONYTAIL ->
            drawArc(hair, 185f, 170f, true, headRect(1.2f).topLeft, headRect(1.2f).size)
        AvatarHairStyle.MOHAWK -> {
            val crest = Path().apply {
                moveTo(X(31f), Y(29f))
                quadraticBezierTo(X(50f), Y(2f), X(69f), Y(29f))
                close()
            }
            drawPath(crest, hair)
        }
        AvatarHairStyle.BUN -> {
            drawArc(hair, 180f, 180f, true, headRect(1.6f).topLeft, headRect(1.6f).size)
            drawCircle(hair, R(6.5f), Offset(X(50f), Y(15f)))
        }
        AvatarHairStyle.HEADBAND -> {
            drawArc(hair, 185f, 170f, true, headRect(1.4f).topLeft, headRect(1.4f).size)
            drawRoundRect(
                color = params.headbandColor,
                topLeft = Offset(X(28f), Y(25.5f)),
                size = Size(R(44f), R(5f)),
                cornerRadius = CornerRadius(R(2.5f), R(2.5f))
            )
        }
        AvatarHairStyle.BALD ->
            drawLine(
                Color.White.copy(alpha = 0.35f),
                Offset(X(42f), Y(23f)), Offset(X(50f), Y(20.5f)),
                strokeWidth = R(1.6f)
            )
    }

    // ==================== 球衣号码 ====================
    if (number != null) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 17f * scale
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            canvas.nativeCanvas.drawText(number.toString(), X(50f), Y(95f), paint)
        }
    }
}
