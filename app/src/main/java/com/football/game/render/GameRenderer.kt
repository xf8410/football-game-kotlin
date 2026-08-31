package com.football.game.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.football.game.core.GameState
import com.football.game.core.RefereeState
import com.football.game.core.TackleRules
import com.football.game.core.Vector3
import com.football.game.model.Player
import com.football.game.ui.component.HairStyle3D
import com.football.game.ui.component.KitPattern
import com.football.game.ui.component.PlayerLook
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * 游戏渲染器
 * OpenGL ES 2.0：人形球员模型（胶囊四肢 + 旋转体躯干/球裤 + 颈部 + 肩部 + 球形头 + 发型），
 * 队服系统（竖条纹/横条纹/拼色/斜杠 + 独立球裤球袜配色），
 * 完整球场环境（LED广告板 + 四面阶梯看台 + 上万观众 + 顶棚 + 球网 + 角旗 + 泛光灯塔），
 * 裁判模型（吹哨 + 红黄牌），滑铲/倒地姿态，细条纹草皮 + 白线球场
 *
 * 模型升级：方块人 → 人形（真人人身比例，圆锥过渡四肢，椭圆截面旋转体躯干）
 * 场景升级：悬浮草皮 → 完整体育场（顶点色单绘制调用，性能无压力）
 * 抽搐修复（保留）：onDrawFrame 先用真实帧间隔调用 frameCallback（MatchScreen 在此推进模拟），
 * 随后立即绘制 → 模拟与画面同帧同相；相机/动画帧率无关平滑
 */
class GameRenderer : GLSurfaceView.Renderer {

    // ==================== 着色器 ====================

    private val vertexShader = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;
        uniform vec3 uColor;
        uniform float uAlpha;
        uniform float uUseVColor;
        varying vec3 vNormal;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vNormal = (uModelMatrix * vec4(aNormal, 0.0)).xyz;
            vColor = mix(vec4(uColor, uAlpha), aColor, uUseVColor);
        }
    """

    private val fragmentShader = """
        precision mediump float;
        uniform vec3 uLightDir;
        varying vec3 vNormal;
        varying vec4 vColor;
        void main() {
            vec3 n = normalize(vNormal);
            float diff = max(dot(n, normalize(uLightDir)), 0.0);
            float lambert = 0.62 + 0.38 * diff;
            gl_FragColor = vec4(vColor.rgb * lambert, vColor.a);
        }
    """

    // ==================== 矩阵 ====================

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // 相机（跟随球，平滑移动）
    private var camTargetX = 0f
    private var camTargetZ = 0f
    private var camPosX = 0f
    private var camPosY = 36f
    private var camPosZ = -30f

    // ==================== 资源 ====================

    private var program = GLProgram()
    private lateinit var sphere: GLShape
    private lateinit var headSphere: GLShape
    private lateinit var cube: GLShape
    private lateinit var quad: GLShape
    private lateinit var centerCircle: GLShape
    private lateinit var markerRing: GLShape

    // 人形部件（旋转体/胶囊）
    private lateinit var legShape: GLShape
    private lateinit var sockShape: GLShape
    private lateinit var armShape: GLShape
    private lateinit var neckShape: GLShape
    private lateinit var shortsShape: GLShape
    private lateinit var torsoSolid: GLShape
    private lateinit var torsoStripes: List<GLShape>
    private lateinit var torsoHoops: List<GLShape>
    private lateinit var torsoHalf0: GLShape
    private lateinit var torsoHalf1: GLShape
    private lateinit var torsoSashBase: GLShape
    private lateinit var torsoSashWedge: GLShape

    // 体育场环境（顶点色）
    private lateinit var stadiumShape: GLShape
    private lateinit var netShape: GLShape

    private var ready = false
    private var frameTime = 0f

    /**
     * 每帧模拟回调：绘制前调用，参数 = 真实帧间隔（秒）。
     * MatchScreen 借此推进 gameEngine.update(dt)（渲染线程驱动模拟）。
     */
    var frameCallback: ((Float) -> Unit)? = null
    private var lastFrameNanos = 0L

    // ==================== 游戏数据 ====================

    private var homePlayers: List<Player> = emptyList()
    private var awayPlayers: List<Player> = emptyList()
    private var homeLooks: List<PlayerLook> = emptyList()
    private var awayLooks: List<PlayerLook> = emptyList()
    private var refereeState: RefereeState? = null
    private var ballPosition = Vector3.ZERO
    private var ballHeight = 0f
    private var activePlayerIndex = -1

    // ==================== 人形比例（米） ====================
    // 腿：髋部枢轴 0.90，腿部胶囊局部 y ∈ [-0.42, +0.42]（站姿世界 0.06 ~ 0.90）
    // 躯干：旋转体 1.10 ~ 1.67；球裤 0.82 ~ 1.15；手臂枢轴 1.57；头心 1.84；总高 ~2.0

    private val torsoProfile = listOf(
        Triple(1.10f, 0.185f, 0.125f),
        Triple(1.22f, 0.168f, 0.115f),
        Triple(1.38f, 0.175f, 0.120f),
        Triple(1.54f, 0.200f, 0.132f),
        Triple(1.63f, 0.210f, 0.138f),
        Triple(1.67f, 0.140f, 0.105f)
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.05f, 0.07f, 0.09f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        program = buildProgram()
        if (program.id == 0) return

        sphere = GLShape(buildSphere(latBands = 8, lonBands = 12))
        headSphere = GLShape(buildSphere(latBands = 12, lonBands = 18))
        cube = GLShape(buildCube())
        quad = GLShape(buildQuad())
        centerCircle = GLShape(buildRing(innerR = 8.9f, outerR = 9.15f))
        markerRing = GLShape(buildRing(innerR = 0.34f, outerR = 0.42f, segments = 28))

        // ---- 人形部件 ----
        // 腿：底部圆头 → 踝 → 小腿肚 → 大腿 → 髋圆头
        legShape = GLShape(
            buildRevolution(
                listOf(
                    Triple(-0.42f, 0.044f, 0.044f),
                    Triple(-0.38f, 0.052f, 0.052f),
                    Triple(-0.24f, 0.056f, 0.056f),
                    Triple(-0.06f, 0.068f, 0.068f),
                    Triple(0.12f, 0.078f, 0.078f),
                    Triple(0.30f, 0.086f, 0.086f),
                    Triple(0.40f, 0.088f, 0.088f),
                    Triple(0.42f, 0.064f, 0.064f)
                )
            )
        )
        // 球袜：踝 → 小腿肚（略大于腿，包在外面）
        sockShape = GLShape(
            buildRevolution(
                listOf(
                    Triple(-0.36f, 0.062f, 0.062f),
                    Triple(-0.20f, 0.064f, 0.064f),
                    Triple(-0.08f, 0.074f, 0.074f),
                    Triple(0.00f, 0.082f, 0.082f)
                ),
                capTop = false
            )
        )
        // 手臂：腕 → 小臂 → 大臂 → 肩圆头
        armShape = GLShape(
            buildRevolution(
                listOf(
                    Triple(-0.28f, 0.034f, 0.034f),
                    Triple(-0.24f, 0.040f, 0.040f),
                    Triple(-0.10f, 0.045f, 0.045f),
                    Triple(0.06f, 0.050f, 0.050f),
                    Triple(0.20f, 0.056f, 0.056f),
                    Triple(0.28f, 0.046f, 0.046f)
                )
            )
        )
        // 脖子
        neckShape = GLShape(
            buildRevolution(
                listOf(
                    Triple(0.00f, 0.052f, 0.050f),
                    Triple(0.10f, 0.056f, 0.054f)
                ),
                capTop = false,
                capBottom = false
            )
        )
        // 球裤（髋部旋转体，绝对世界坐标）
        shortsShape = GLShape(
            buildRevolution(
                listOf(
                    Triple(0.82f, 0.160f, 0.118f),
                    Triple(0.88f, 0.190f, 0.135f),
                    Triple(1.00f, 0.200f, 0.140f),
                    Triple(1.12f, 0.190f, 0.135f),
                    Triple(1.15f, 0.175f, 0.125f)
                )
            )
        )
        // 躯干：纯色
        torsoSolid = GLShape(buildRevolution(torsoProfile))
        // 躯干：竖条纹（5 个 72° 扇区，交替染色）
        torsoStripes = (0 until 5).map { i ->
            GLShape(buildRevolution(torsoProfile, 3, i * 72f, (i + 1) * 72f))
        }
        // 躯干：横条纹（4 段堆叠圆环，交替染色，边界共用环无缝）
        torsoHoops = listOf(
            GLShape(
                buildRevolution(
                    listOf(
                        Triple(1.10f, 0.185f, 0.125f),
                        Triple(1.16f, 0.174f, 0.119f),
                        Triple(1.22f, 0.168f, 0.115f)
                    ),
                    capTop = false
                )
            ),
            GLShape(
                buildRevolution(
                    listOf(
                        Triple(1.22f, 0.168f, 0.115f),
                        Triple(1.30f, 0.171f, 0.117f),
                        Triple(1.38f, 0.175f, 0.120f)
                    ),
                    capTop = false,
                    capBottom = false
                )
            ),
            GLShape(
                buildRevolution(
                    listOf(
                        Triple(1.38f, 0.175f, 0.120f),
                        Triple(1.46f, 0.187f, 0.126f),
                        Triple(1.54f, 0.200f, 0.132f)
                    ),
                    capTop = false,
                    capBottom = false
                )
            ),
            GLShape(
                buildRevolution(
                    listOf(
                        Triple(1.54f, 0.200f, 0.132f),
                        Triple(1.59f, 0.207f, 0.136f),
                        Triple(1.63f, 0.210f, 0.138f),
                        Triple(1.67f, 0.140f, 0.105f)
                    ),
                    capBottom = false
                )
            )
        )
        // 躯干：左右拼色（两个 180° 半边）
        torsoHalf0 = GLShape(buildRevolution(torsoProfile, 7, 0f, 180f))
        torsoHalf1 = GLShape(buildRevolution(torsoProfile, 7, 180f, 360f))
        // 躯干：斜杠（底色 + 一条斜置窄带）
        torsoSashBase = GLShape(buildRevolution(torsoProfile, 14))
        torsoSashWedge = GLShape(buildRevolution(torsoProfile, 2, 40f, 66f))

        // ---- 体育场环境（顶点色网格，单绘制调用）----
        val stadium = buildStadiumMesh()
        stadiumShape = GLShape(stadium.first, stadium.second)
        val net = buildNetMesh()
        netShape = GLShape(net.first, net.second)

        GLES20.glEnableVertexAttribArray(program.aPosition)
        GLES20.glEnableVertexAttribArray(program.aNormal)
        ready = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = if (height > 0) width.toFloat() / height.toFloat() else 1f
        Matrix.perspectiveM(projectionMatrix, 0, 50f, ratio, 1f, 300f)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (!ready) return

        // 真实帧间隔（秒）
        val nowNanos = System.nanoTime()
        val frameDt = if (lastFrameNanos == 0L) 0.016f
                      else ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNanos = nowNanos
        frameTime += frameDt

        // 先推进模拟（渲染线程驱动，与绘制同帧同相），再画本帧 → 数据零延迟
        frameCallback?.invoke(frameDt)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program.id)
        GLES20.glUniform3f(program.uLight, 0.4f, -0.85f, -0.35f)

        // 相机跟随球（帧率无关平滑：1 - e^(-k·dt)）
        val camLerp = 1f - kotlin.math.exp(-6f * frameDt)
        camTargetX = lerp(camTargetX, ballPosition.x, camLerp)
        camTargetZ = lerp(camTargetZ, ballPosition.z, camLerp)
        camPosX = lerp(camPosX, camTargetX * 0.85f, camLerp)
        camPosY = lerp(camPosY, 36f, camLerp)
        camPosZ = lerp(camPosZ, camTargetZ - 30f, camLerp)
        Matrix.setLookAtM(
            viewMatrix, 0,
            camPosX, camPosY, camPosZ,
            camTargetX, 0f, camTargetZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        drawStadium()
        drawField()
        drawNets()
        drawAllPlayers()
        refereeState?.let { drawReferee(it) }
        drawBall()
    }

    // ==================== 体育场 ====================

    /** 广告板 + 看台 + 上万观众 + 顶棚 + 后墙 + 角旗 + 泛光灯塔 + 外围地面（全部顶点色，一次绘制） */
    private fun drawStadium() {
        if (!::stadiumShape.isInitialized) return
        drawShapeAt(stadiumShape, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 1f, 1f, 1f)
    }

    /** 球门网（半透明白网，两端各一张，+z 端直接摆，-z 端旋转 180°） */
    private fun drawNets() {
        if (!::netShape.isInitialized) return
        val halfL = GameState.FIELD_LENGTH / 2
        drawShapeAt(netShape, 0f, 0f, halfL + 0.1f, 1f, 1f, 1f, 0f, 1f, 1f, 1f)
        drawShapeAt(netShape, 0f, 0f, -halfL - 0.1f, 1f, 1f, 1f, 180f, 1f, 1f, 1f)
    }

    /**
     * 生成体育场网格（位置+法线 / 顶点色 两个缓冲）。
     * 布局：草皮边缘 → LED 广告板（3m 一段紫白相间）→ 12 排阶梯看台（每排 ~140 观众，
     * 随机球衣色 + 微后仰）→ 顶棚 + 后墙；四角角旗 + 泛光灯塔；最外围深色地面。
     */
    private fun buildStadiumMesh(): Pair<FloatArray, FloatArray> {
        val pos = ArrayList<Float>(1 shl 20)
        val col = ArrayList<Float>(1 shl 20)
        val rnd = Random(20260831L)

        fun face(
            v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray,
            n: FloatArray, r: Float, g: Float, b: Float, a: Float = 1f
        ) {
            for (v in listOf(v0, v1, v2, v0, v2, v3)) {
                pos.addAll(listOf(v[0], v[1], v[2], n[0], n[1], n[2]))
                col.addAll(listOf(r, g, b, a))
            }
        }

        fun box(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
            val hx = sx / 2f
            val hy = sy / 2f
            val hz = sz / 2f
            face(floatArrayOf(cx - hx, cy - hy, cz + hz), floatArrayOf(cx + hx, cy - hy, cz + hz), floatArrayOf(cx + hx, cy + hy, cz + hz), floatArrayOf(cx - hx, cy + hy, cz + hz), floatArrayOf(0f, 0f, 1f), r, g, b)
            face(floatArrayOf(cx + hx, cy - hy, cz - hz), floatArrayOf(cx - hx, cy - hy, cz - hz), floatArrayOf(cx - hx, cy + hy, cz - hz), floatArrayOf(cx + hx, cy + hy, cz - hz), floatArrayOf(0f, 0f, -1f), r, g, b)
            face(floatArrayOf(cx + hx, cy - hy, cz + hz), floatArrayOf(cx + hx, cy - hy, cz - hz), floatArrayOf(cx + hx, cy + hy, cz - hz), floatArrayOf(cx + hx, cy + hy, cz + hz), floatArrayOf(1f, 0f, 0f), r, g, b)
            face(floatArrayOf(cx - hx, cy - hy, cz - hz), floatArrayOf(cx - hx, cy - hy, cz + hz), floatArrayOf(cx - hx, cy + hy, cz + hz), floatArrayOf(cx - hx, cy + hy, cz - hz), floatArrayOf(-1f, 0f, 0f), r, g, b)
            face(floatArrayOf(cx - hx, cy + hy, cz + hz), floatArrayOf(cx + hx, cy + hy, cz + hz), floatArrayOf(cx + hx, cy + hy, cz - hz), floatArrayOf(cx - hx, cy + hy, cz - hz), floatArrayOf(0f, 1f, 0f), r, g, b)
            face(floatArrayOf(cx - hx, cy - hy, cz - hz), floatArrayOf(cx + hx, cy - hy, cz - hz), floatArrayOf(cx + hx, cy - hy, cz + hz), floatArrayOf(cx - hx, cy - hy, cz + hz), floatArrayOf(0f, -1f, 0f), r, g, b)
        }

        // ---- 外围地面（深色混凝土）----
        box(0f, -0.07f, 0f, 190f, 0.14f, 230f, 0.10f, 0.10f, 0.11f)

        // ---- LED 广告板（绕场一圈，3m 一段交替色）----
        val boardCols = arrayOf(
            floatArrayOf(0.60f, 0.18f, 0.82f),
            floatArrayOf(0.94f, 0.94f, 0.97f),
            floatArrayOf(0.13f, 0.14f, 0.50f),
            floatArrayOf(0.94f, 0.94f, 0.97f)
        )
        var bi = 0
        var z = -54f
        while (z < 54f) {
            val c = boardCols[bi % boardCols.size]
            val zEnd = (z + 3f).coerceAtMost(54f)
            val w = zEnd - z
            val zc = z + w / 2f
            face(floatArrayOf(36f, 0f, zc - w / 2), floatArrayOf(36f, 0f, zc + w / 2), floatArrayOf(36f, 0.95f, zc + w / 2), floatArrayOf(36f, 0.95f, zc - w / 2), floatArrayOf(-1f, 0f, 0f), c[0], c[1], c[2])
            face(floatArrayOf(-36f, 0f, zc + w / 2), floatArrayOf(-36f, 0f, zc - w / 2), floatArrayOf(-36f, 0.95f, zc - w / 2), floatArrayOf(-36f, 0.95f, zc + w / 2), floatArrayOf(1f, 0f, 0f), c[0], c[1], c[2])
            z = zEnd
            bi++
        }
        var x = -33f
        while (x < 33f) {
            val c = boardCols[bi % boardCols.size]
            val xEnd = (x + 3f).coerceAtMost(33f)
            val w = xEnd - x
            val xc = x + w / 2f
            face(floatArrayOf(xc + w / 2, 0f, 55f), floatArrayOf(xc - w / 2, 0f, 55f), floatArrayOf(xc - w / 2, 0.95f, 55f), floatArrayOf(xc + w / 2, 0.95f, 55f), floatArrayOf(0f, 0f, -1f), c[0], c[1], c[2])
            face(floatArrayOf(xc - w / 2, 0f, -55f), floatArrayOf(xc + w / 2, 0f, -55f), floatArrayOf(xc + w / 2, 0.95f, -55f), floatArrayOf(xc - w / 2, 0.95f, -55f), floatArrayOf(0f, 0f, 1f), c[0], c[1], c[2])
            x = xEnd
            bi++
        }

        // ---- 看台（四面：12 排阶梯 + 观众 + 顶棚 + 后墙）----
        val rows = 12
        val stepD = 1.15f
        val stepH = 0.55f
        val crowdPalette = arrayOf(
            floatArrayOf(0.90f, 0.90f, 0.92f), floatArrayOf(0.85f, 0.25f, 0.25f),
            floatArrayOf(0.25f, 0.35f, 0.80f), floatArrayOf(0.95f, 0.75f, 0.20f),
            floatArrayOf(0.20f, 0.20f, 0.22f), floatArrayOf(0.55f, 0.30f, 0.15f),
            floatArrayOf(0.90f, 0.60f, 0.70f), floatArrayOf(0.30f, 0.60f, 0.35f),
            floatArrayOf(0.80f, 0.55f, 0.30f), floatArrayOf(0.45f, 0.45f, 0.50f)
        )

        fun sideStand(sign: Float) {
            for (i in 0 until rows) {
                val frontX = sign * (38f + i * stepD)
                val baseY = 1.2f + i * stepH
                val cx = frontX + sign * stepD / 2f
                box(cx, baseY + 0.35f, 0f, stepD, 0.7f, 112f, 0.26f, 0.27f, 0.30f)
                var p = -54f
                while (p < 54f) {
                    val c = crowdPalette[rnd.nextInt(crowdPalette.size)]
                    val px = frontX + sign * 0.30f
                    val lean = sign * 0.12f
                    face(
                        floatArrayOf(px, baseY + 0.70f, p), floatArrayOf(px, baseY + 0.70f, p + 0.45f),
                        floatArrayOf(px + lean, baseY + 1.28f, p + 0.45f), floatArrayOf(px + lean, baseY + 1.28f, p),
                        floatArrayOf(-sign, 0f, 0f), c[0], c[1], c[2]
                    )
                    p += 0.78f
                }
            }
            box(sign * (38f + rows * stepD + 1.4f), 1.2f + rows * stepH + 3.0f, 0f, 8f, 0.35f, 116f, 0.55f, 0.56f, 0.60f)
            box(sign * (38f + rows * stepD + 0.55f), (1.2f + rows * stepH) / 2f + 0.6f, 0f, 1.1f, 1.2f + rows * stepH + 1.2f, 116f, 0.18f, 0.18f, 0.20f)
        }

        fun endStand(sign: Float) {
            for (i in 0 until rows) {
                val frontZ = sign * (57f + i * stepD)
                val baseY = 1.2f + i * stepH
                val cz = frontZ + sign * stepD / 2f
                box(0f, baseY + 0.35f, cz, 80f, 0.7f, stepD, 0.26f, 0.27f, 0.30f)
                var p = -39f
                while (p < 39f) {
                    val c = crowdPalette[rnd.nextInt(crowdPalette.size)]
                    val pz = frontZ + sign * 0.30f
                    val lean = sign * 0.12f
                    face(
                        floatArrayOf(p, baseY + 0.70f, pz), floatArrayOf(p + 0.45f, baseY + 0.70f, pz),
                        floatArrayOf(p + 0.45f, baseY + 1.28f, pz + lean), floatArrayOf(p, baseY + 1.28f, pz + lean),
                        floatArrayOf(0f, 0f, -sign), c[0], c[1], c[2]
                    )
                    p += 0.78f
                }
            }
            box(0f, 1.2f + rows * stepH + 3.0f, sign * (57f + rows * stepD + 1.4f), 84f, 0.35f, 8f, 0.55f, 0.56f, 0.60f)
            box(0f, (1.2f + rows * stepH) / 2f + 0.6f, sign * (57f + rows * stepD + 0.55f), 84f, 1.2f + rows * stepH + 1.2f, 1.1f, 0.18f, 0.18f, 0.20f)
        }

        sideStand(1f)
        sideStand(-1f)
        endStand(1f)
        endStand(-1f)

        // ---- 角旗（四角：白杆 + 黄旗）----
        for (sx in intArrayOf(-1, 1)) {
            for (sz in intArrayOf(-1, 1)) {
                val fx = sx * 34.2f
                val fz = sz * 52.7f
                box(fx, 0.75f, fz, 0.05f, 1.5f, 0.05f, 0.85f, 0.87f, 0.90f)
                face(
                    floatArrayOf(fx, 1.5f, fz), floatArrayOf(fx, 1.5f, fz + sz * 0.45f),
                    floatArrayOf(fx, 1.2f, fz + sz * 0.45f), floatArrayOf(fx, 1.2f, fz),
                    floatArrayOf(-sx.toFloat(), 0f, 0f), 1f, 0.75f, 0.10f
                )
            }
        }

        // ---- 泛光灯塔（四角，白色灯板）----
        for (sx in intArrayOf(-1, 1)) {
            for (sz in intArrayOf(-1, 1)) {
                val lx = sx * 64f
                val lz = sz * 78f
                box(lx, 11f, lz, 0.8f, 22f, 0.8f, 0.35f, 0.36f, 0.40f)
                box(lx - sx * 1.2f, 22.6f, lz - sz * 1.2f, 5.5f, 3.2f, 5.5f, 0.92f, 0.93f, 0.85f)
            }
        }

        return Pair(pos.toFloatArray(), col.toFloatArray())
    }

    /** 单个球门网（背网 + 顶网 + 两侧网，半透明白；建在 +z 方向，另一端由绘制端旋转 180°） */
    private fun buildNetMesh(): Pair<FloatArray, FloatArray> {
        val pos = ArrayList<Float>()
        val col = ArrayList<Float>()

        fun face(v0: FloatArray, v1: FloatArray, v2: FloatArray, v3: FloatArray, n: FloatArray, a: Float) {
            for (v in listOf(v0, v1, v2, v0, v2, v3)) {
                pos.addAll(listOf(v[0], v[1], v[2], n[0], n[1], n[2]))
                col.addAll(listOf(0.96f, 0.96f, 0.98f, a))
            }
        }

        val gw = GameState.GOAL_WIDTH / 2
        val gh = 2.44f
        val depth = 1.9f
        val a = 0.38f

        // 背网（竖条 + 横条）
        var x = -gw
        while (x <= gw + 0.01f) {
            face(floatArrayOf(x, 0f, depth), floatArrayOf(x, 0f, depth - 0.05f), floatArrayOf(x, gh, depth - 0.05f), floatArrayOf(x, gh, depth), floatArrayOf(0f, 0f, -1f), a)
            x += 0.42f
        }
        var y = 0f
        while (y <= gh + 0.01f) {
            face(floatArrayOf(-gw, y, depth), floatArrayOf(gw, y, depth), floatArrayOf(gw, y, depth - 0.05f), floatArrayOf(-gw, y, depth - 0.05f), floatArrayOf(0f, 0f, -1f), a)
            y += 0.42f
        }
        // 顶网（横梁 → 后上方的斜面网）
        var x2 = -gw
        while (x2 <= gw + 0.01f) {
            face(floatArrayOf(x2, gh, 0f), floatArrayOf(x2, gh, depth - 0.05f), floatArrayOf(x2, gh - 0.25f, depth - 0.05f), floatArrayOf(x2, gh - 0.25f, depth), floatArrayOf(0f, 1f, 0f), a)
            x2 += 0.42f
        }
        // 两侧网
        for (sx in floatArrayOf(-gw, gw)) {
            var zz = 0f
            while (zz <= depth + 0.01f) {
                face(floatArrayOf(sx, 0f, zz), floatArrayOf(sx + 0.05f, 0f, zz), floatArrayOf(sx + 0.05f, gh, zz), floatArrayOf(sx, gh, zz), floatArrayOf(-1f, 0f, 0f), a)
                zz += 0.42f
            }
        }

        return Pair(pos.toFloatArray(), col.toFloatArray())
    }

    // ==================== 球场 ====================

    private fun drawField() {
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2

        // 外围草皮（延伸到看台脚下）
        drawShapeAt(quad, 0f, -0.03f, 0f, 150f, 1f, 190f, 0f, 0.10f, 0.30f, 0.16f)

        // 割草条纹（14 条细条纹）
        val stripes = 14
        val stripeLen = GameState.FIELD_LENGTH / stripes
        for (i in 0 until stripes) {
            val zc = -halfL + stripeLen * (i + 0.5f)
            if (i % 2 == 0) {
                drawShapeAt(quad, 0f, 0f, zc, GameState.FIELD_WIDTH, 1f, stripeLen, 0f, 0.16f, 0.55f, 0.26f)
            } else {
                drawShapeAt(quad, 0f, 0f, zc, GameState.FIELD_WIDTH, 1f, stripeLen, 0f, 0.14f, 0.50f, 0.23f)
            }
        }

        // ---- 白线 ----
        val lw = 0.15f
        drawShapeAt(quad, -halfW, 0.03f, 0f, lw, 1f, GameState.FIELD_LENGTH + lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, halfW, 0.03f, 0f, lw, 1f, GameState.FIELD_LENGTH + lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, 0f, 0.03f, -halfL, GameState.FIELD_WIDTH + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, 0f, 0.03f, halfL, GameState.FIELD_WIDTH + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, 0f, 0.03f, 0f, GameState.FIELD_WIDTH + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(centerCircle, 0f, 0.05f, 0f, 1f, 1f, 1f, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, 0f, 0.04f, 0f, 0.4f, 1f, 0.4f, 0f, 0.95f, 0.95f, 0.95f)

        // 两端禁区 + 小禁区 + 点球点 + 球门柱
        for (sign in floatArrayOf(-1f, 1f)) {
            val goalZ = halfL * sign
            val dir = -sign

            val boxW = 40.32f
            val boxD = 16.5f
            drawShapeAt(quad, 0f, 0.03f, goalZ + dir * boxD, boxW + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, -boxW / 2, 0.03f, goalZ + dir * boxD / 2, lw, 1f, boxD, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, boxW / 2, 0.03f, goalZ + dir * boxD / 2, lw, 1f, boxD, 0f, 0.95f, 0.95f, 0.95f)

            val gW = 18.32f
            val gD = 5.5f
            drawShapeAt(quad, 0f, 0.03f, goalZ + dir * gD, gW + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, -gW / 2, 0.03f, goalZ + dir * gD / 2, lw, 1f, gD, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, gW / 2, 0.03f, goalZ + dir * gD / 2, lw, 1f, gD, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, 0f, 0.04f, goalZ + dir * 11f, 0.35f, 1f, 0.35f, 0f, 0.95f, 0.95f, 0.95f)

            val postH = 2.44f
            val goalHalf = GameState.GOAL_WIDTH / 2
            drawShapeAt(cube, -goalHalf, postH / 2, goalZ, 0.12f, postH, 0.12f, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(cube, goalHalf, postH / 2, goalZ, 0.12f, postH, 0.12f, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(cube, 0f, postH, goalZ, GameState.GOAL_WIDTH + 0.12f, 0.12f, 0.12f, 0f, 0.95f, 0.95f, 0.95f)
        }
    }

    // ==================== 球员 ====================

    private fun drawAllPlayers() {
        for ((index, player) in homePlayers.withIndex()) {
            if (player.sentOff) continue // 红牌罚下不再上场
            drawPlayer(
                player = player,
                look = homeLooks.getOrNull(index) ?: defaultLook(true),
                index = index,
                isActive = index == activePlayerIndex
            )
        }
        for ((index, player) in awayPlayers.withIndex()) {
            if (player.sentOff) continue
            drawPlayer(
                player = player,
                look = awayLooks.getOrNull(index) ?: defaultLook(false),
                index = index + 11,
                isActive = false
            )
        }
    }

    /**
     * 球员：姿态计算 + 人形绘制
     * 特殊姿态：滑铲（TACKLE，身体后仰 + 前腿伸出）、被铲倒地（FALL，平躺）
     */
    private fun drawPlayer(player: Player, look: PlayerLook, index: Int, isActive: Boolean) {
        val pos = player.position
        val vel = player.velocity
        val speed = kotlin.math.sqrt(vel.x * vel.x + vel.z * vel.z)
        val face = if (speed > 0.1f) vel else player.facingDirection
        val rotY = Math.toDegrees(
            kotlin.math.atan2(face.x.toDouble(), face.z.toDouble())
        ).toFloat()

        val sliding = player.animState == Player.AnimState.TACKLE && player.slideTimer > 0f
        val falling = player.animState == Player.AnimState.FALL && player.fallTimer > 0f

        val lean = when {
            falling -> -86f // 平躺
            sliding -> -60f // 后仰滑行
            else -> 0f
        }

        val amp = (speed / 6f).coerceIn(0f, 1f)
        val phase = frameTime * (4f + speed * 1.6f) + index * 1.9f
        var swing = sin(phase) * 32f * amp
        if (sliding || falling) swing = 0f

        drawHumanoid(
            px = pos.x,
            pz = pos.z,
            rotY = rotY,
            lean = lean,
            swing = swing,
            sliding = sliding,
            falling = falling,
            kit1 = rgb(look.kitColor1),
            kit2 = rgb(look.kitColor2),
            pattern = look.pattern,
            shorts = rgb(look.shortsColor),
            socks = rgb(look.socksColor),
            sleeve = if (look.pattern == KitPattern.SOLID) rgb(look.kitColor2) else rgb(look.kitColor1),
            skin = rgb(look.skinColor),
            hair = rgb(look.hairColor),
            hairStyle = look.hairStyle3D,
            isActive = isActive
        )
    }

    /**
     * 人形模型（球员/裁判共用）：
     * 阴影 → 双腿(胶囊摆动) → 球袜 → 球鞋 → 球裤(旋转体) → 躯干(旋转体+队服花纹)
     * → 肩垫 → 双臂(胶囊摆动) → 脖子 → 头(球) → 发型 → 选中标记
     */
    private fun drawHumanoid(
        px: Float, pz: Float, rotY: Float, lean: Float,
        swing: Float, sliding: Boolean, falling: Boolean,
        kit1: FloatArray, kit2: FloatArray, pattern: KitPattern,
        shorts: FloatArray, socks: FloatArray, sleeve: FloatArray,
        skin: FloatArray, hair: FloatArray, hairStyle: HairStyle3D,
        isActive: Boolean
    ) {
        val shoe = floatArrayOf(0.10f, 0.10f, 0.12f)

        // 阴影
        drawShapeAt(quad, px, 0.02f, pz, 0.6f, 1f, 0.6f, 0f, 0f, 0f, 0f, 0.28f)

        // 腿摆角：滑铲前腿伸出 / 倒地平放 / 跑动摆动
        val legFront = when {
            sliding -> 72f
            falling -> 8f
            else -> swing
        }
        val legBack = when {
            sliding -> -18f
            falling -> -6f
            else -> -swing
        }

        // 双腿（胶囊，肤色；髋部枢轴 0.90）
        drawBody(legShape, px, pz, rotY, lean, -0.105f, 0.90f, 0f, legFront, -0.42f, 1f, 1f, 1f, skin[0], skin[1], skin[2])
        drawBody(legShape, px, pz, rotY, lean, 0.105f, 0.90f, 0f, legBack, -0.42f, 1f, 1f, 1f, skin[0], skin[1], skin[2])

        // 球袜（队服独立配色，包住踝→小腿肚）
        drawBody(sockShape, px, pz, rotY, lean, -0.105f, 0.90f, 0f, legFront, -0.42f, 1f, 1f, 1f, socks[0], socks[1], socks[2])
        drawBody(sockShape, px, pz, rotY, lean, 0.105f, 0.90f, 0f, legBack, -0.42f, 1f, 1f, 1f, socks[0], socks[1], socks[2])

        // 球鞋
        drawBody(cube, px, pz, rotY, lean, -0.105f, 0.10f, 0.04f, legFront, 0f, 0.13f, 0.085f, 0.28f, shoe[0], shoe[1], shoe[2])
        drawBody(cube, px, pz, rotY, lean, 0.105f, 0.10f, 0.04f, legBack, 0f, 0.13f, 0.085f, 0.28f, shoe[0], shoe[1], shoe[2])

        // 球裤（旋转体，队服独立配色）
        drawBody(shortsShape, px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, shorts[0], shorts[1], shorts[2])

        // 躯干（旋转体 + 队服花纹）
        drawTorso(px, pz, rotY, lean, pattern, kit1, kit2)

        // 肩垫（球形，队服主色/袖色）
        drawBody(sphere, px, pz, rotY, lean, -0.24f, 1.58f, 0f, 0f, 0f, 0.15f, 0.12f, 0.15f, sleeve[0], sleeve[1], sleeve[2])
        drawBody(sphere, px, pz, rotY, lean, 0.24f, 1.58f, 0f, 0f, 0f, 0.15f, 0.12f, 0.15f, sleeve[0], sleeve[1], sleeve[2])

        // 双臂（胶囊，肤色，反向摆动；倒地张开 / 滑铲后撑）
        val armL = when {
            falling -> -35f
            sliding -> -50f
            else -> -swing * 0.8f
        }
        val armR = when {
            falling -> 35f
            sliding -> 30f
            else -> swing * 0.8f
        }
        drawBody(armShape, px, pz, rotY, lean, -0.245f, 1.57f, 0f, armL, -0.19f, 1f, 1f, 1f, skin[0], skin[1], skin[2])
        drawBody(armShape, px, pz, rotY, lean, 0.245f, 1.57f, 0f, armR, -0.19f, 1f, 1f, 1f, skin[0], skin[1], skin[2])

        // 脖子
        drawBody(neckShape, px, pz, rotY, lean, 0f, 1.66f, 0.01f, 0f, 0f, 1f, 1f, 1f, skin[0], skin[1], skin[2])

        // 头（肤色椭球）
        drawBody(headSphere, px, pz, rotY, lean, 0f, 1.84f, 0f, 0f, 0f, 0.155f, 0.17f, 0.155f, skin[0], skin[1], skin[2])

        // 发型（俯视角下最醒目的球星特征）
        drawHair(hairStyle, hair, px, pz, rotY, lean)

        // 受控球员标记
        if (isActive) {
            val bob = sin(frameTime * 4f) * 0.05f
            drawShapeAt(markerRing, px, 2.18f + bob, pz, 1f, 1f, 1f, 0f, 1f, 0.84f, 0.0f, 0.95f)
        }
    }

    /**
     * 躯干球衣：支持 5 种队服花纹（旋转体扇区/分段染色）
     * SOLID=纯色 / STRIPES=竖条纹(5扇区) / HOOPS=横条纹(4段) / HALF=左右拼色 / SASH=斜杠
     */
    private fun drawTorso(
        px: Float, pz: Float, rotY: Float, lean: Float,
        pattern: KitPattern, kit1: FloatArray, kit2: FloatArray
    ) {
        when (pattern) {
            KitPattern.SOLID ->
                drawBody(torsoSolid, px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, kit1[0], kit1[1], kit1[2])
            KitPattern.STRIPES -> {
                for (i in 0 until 5) {
                    val c = if (i % 2 == 0) kit1 else kit2
                    drawBody(torsoStripes[i], px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, c[0], c[1], c[2])
                }
            }
            KitPattern.HOOPS -> {
                for (i in torsoHoops.indices) {
                    val c = if (i % 2 == 0) kit1 else kit2
                    drawBody(torsoHoops[i], px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, c[0], c[1], c[2])
                }
            }
            KitPattern.HALF -> {
                drawBody(torsoHalf0, px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, kit1[0], kit1[1], kit1[2])
                drawBody(torsoHalf1, px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, kit2[0], kit2[1], kit2[2])
            }
            KitPattern.SASH -> {
                drawBody(torsoSashBase, px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, kit1[0], kit1[1], kit1[2])
                drawBody(torsoSashWedge, px, pz, rotY, lean, 0f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, kit2[0], kit2[1], kit2[2])
            }
        }
    }

    private fun drawHair(style: HairStyle3D, hair: FloatArray, px: Float, pz: Float, rotY: Float, lean: Float) {
        when (style) {
            HairStyle3D.BUZZ ->
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.87f, 0f, 0f, 0f, 0.16f, 0.15f, 0.16f, hair[0], hair[1], hair[2])
            HairStyle3D.SHORT ->
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.87f, 0f, 0f, 0f, 0.165f, 0.16f, 0.165f, hair[0], hair[1], hair[2])
            HairStyle3D.FLOW ->
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.86f, -0.03f, 0f, 0f, 0.175f, 0.17f, 0.20f, hair[0], hair[1], hair[2])
            HairStyle3D.AFRO ->
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.89f, 0f, 0f, 0f, 0.225f, 0.21f, 0.225f, hair[0], hair[1], hair[2])
            HairStyle3D.CURLY ->
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.87f, 0f, 0f, 0f, 0.18f, 0.17f, 0.18f, hair[0], hair[1], hair[2])
            HairStyle3D.PONYTAIL -> {
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.87f, 0f, 0f, 0f, 0.165f, 0.16f, 0.165f, hair[0], hair[1], hair[2])
                drawBody(headSphere, px, pz, rotY, lean, 0f, 1.68f, -0.17f, 0f, 0f, 0.07f, 0.11f, 0.07f, hair[0], hair[1], hair[2])
            }
            HairStyle3D.NONE -> {}
        }
    }

    private fun defaultLook(isHome: Boolean): PlayerLook {
        return if (isHome) {
            PlayerLook(0xFFC62828.toInt(), 0xFFFFFFFF.toInt(), 0xFFE8B58B.toInt(), 0xFF2B1B12.toInt(), HairStyle3D.SHORT)
        } else {
            PlayerLook(0xFF1565C0.toInt(), 0xFFFFFFFF.toInt(), 0xFFE8B58B.toInt(), 0xFF2B1B12.toInt(), HairStyle3D.SHORT)
        }
    }

    // ==================== 裁判 ====================

    /**
     * 裁判：人形模型（黑色裁判服），吹哨时嘴边哨子脉冲放大，出牌时头顶举起红/黄牌
     */
    private fun drawReferee(ref: RefereeState) {
        val pos = ref.position
        val rotY = Math.toDegrees(
            kotlin.math.atan2(ref.facing.x.toDouble(), ref.facing.z.toDouble())
        ).toFloat()

        val amp = (ref.speed / 6f).coerceIn(0f, 1f)
        val swing = sin(frameTime * 6f) * 30f * amp

        val kit = floatArrayOf(0.07f, 0.07f, 0.08f)
        drawHumanoid(
            px = pos.x,
            pz = pos.z,
            rotY = rotY,
            lean = 0f,
            swing = swing,
            sliding = false,
            falling = false,
            kit1 = kit,
            kit2 = kit,
            pattern = KitPattern.SOLID,
            shorts = kit,
            socks = kit,
            sleeve = kit,
            skin = rgb(0xFFE8B58B.toInt()),
            hair = rgb(0xFF2B1B12.toInt()),
            hairStyle = HairStyle3D.SHORT,
            isActive = false
        )

        // 哨子（吹哨时嘴边出现 + 脉冲放大）
        if (ref.whistleTimer > 0f) {
            val pulse = 1f + 0.35f * sin(frameTime * 25f)
            drawBody(
                sphere, pos.x, pos.z, rotY, 0f,
                0f, 1.82f, 0.17f, 0f, 0f,
                0.05f * pulse, 0.05f * pulse, 0.05f * pulse,
                0.25f, 0.25f, 0.28f
            )
        }

        // 红黄牌（头顶举起的小卡片）
        if (ref.cardTimer > 0f && ref.cardType != TackleRules.CardType.NONE) {
            val cardColor = if (ref.cardType == TackleRules.CardType.RED) {
                floatArrayOf(0.88f, 0.10f, 0.10f)
            } else {
                floatArrayOf(1.0f, 0.85f, 0.10f)
            }
            drawBody(
                cube, pos.x, pos.z, rotY, 0f,
                0.16f, 2.22f, 0.05f, 0f, 0f,
                0.16f, 0.24f, 0.02f,
                cardColor[0], cardColor[1], cardColor[2]
            )
        }
    }

    // ==================== 球 ====================

    private fun drawBall() {
        drawShapeAt(
            sphere,
            ballPosition.x, 0.11f + ballHeight, ballPosition.z,
            0.11f, 0.11f, 0.11f, 0f,
            0.96f, 0.96f, 0.96f
        )

        val shadowSize = 0.28f + ballHeight * 0.05f
        drawShapeAt(
            quad,
            ballPosition.x, 0.025f, ballPosition.z,
            shadowSize, 1f, shadowSize, 0f,
            0f, 0f, 0f, 0.35f / (1f + ballHeight * 0.4f)
        )
    }

    // ==================== 绘制辅助 ====================

    /** 世界坐标 + 缩放 的简单形状 */
    private fun drawShapeAt(
        shape: GLShape,
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        rotY: Float,
        r: Float, g: Float, b: Float,
        alpha: Float = 1f
    ) {
        if (!ready) return
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        shape.draw(program, vpMatrix, modelMatrix, mvpMatrix, r, g, b, alpha)
    }

    /**
     * 球员身体部件：
     * 平移到球员 → 朝向旋转 → leanX 全身倾倒（滑铲/倒地姿态）
     * → 部件偏移 → rotX 摆动 → pivotDrop 下移 → 缩放
     */
    private fun drawBody(
        shape: GLShape,
        px: Float, pz: Float,
        rotY: Float, leanX: Float,
        ox: Float, oy: Float, oz: Float,
        rotX: Float, pivotDrop: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        alpha: Float = 1f
    ) {
        if (!ready) return
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, 0f, pz)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        if (leanX != 0f) Matrix.rotateM(modelMatrix, 0, leanX, 1f, 0f, 0f)
        Matrix.translateM(modelMatrix, 0, ox, oy, oz)
        if (rotX != 0f) Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f)
        if (pivotDrop != 0f) Matrix.translateM(modelMatrix, 0, 0f, pivotDrop, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        shape.draw(program, vpMatrix, modelMatrix, mvpMatrix, r, g, b, alpha)
    }

    private fun rgb(argb: Int): FloatArray {
        return floatArrayOf(
            ((argb shr 16) and 0xFF) / 255f,
            ((argb shr 8) and 0xFF) / 255f,
            (argb and 0xFF) / 255f
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    // ==================== 数据接口 ====================

    /**
     * 设置游戏数据（每帧由渲染回调调用）
     */
    fun setGameData(
        homePlayers: List<Player>,
        awayPlayers: List<Player>,
        ballPosition: Vector3,
        ballHeight: Float,
        activePlayerIndex: Int,
        homeLooks: List<PlayerLook> = emptyList(),
        awayLooks: List<PlayerLook> = emptyList(),
        referee: RefereeState? = null
    ) {
        this.homePlayers = homePlayers
        this.awayPlayers = awayPlayers
        this.ballPosition = ballPosition
        this.ballHeight = ballHeight
        this.activePlayerIndex = activePlayerIndex
        if (homeLooks.isNotEmpty()) this.homeLooks = homeLooks
        if (awayLooks.isNotEmpty()) this.awayLooks = awayLooks
        if (referee != null) this.refereeState = referee
    }

    // ==================== 着色器程序 ====================

    private fun buildProgram(): GLProgram {
        val glProgram = GLProgram()
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        if (vs == 0 || fs == 0) return glProgram

        val id = GLES20.glCreateProgram()
        GLES20.glAttachShader(id, vs)
        GLES20.glAttachShader(id, fs)
        GLES20.glLinkProgram(id)

        val status = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteProgram(id)
            return glProgram
        }

        glProgram.id = id
        glProgram.aPosition = GLES20.glGetAttribLocation(id, "aPosition")
        glProgram.aNormal = GLES20.glGetAttribLocation(id, "aNormal")
        glProgram.aColor = GLES20.glGetAttribLocation(id, "aColor")
        glProgram.uMVP = GLES20.glGetUniformLocation(id, "uMVPMatrix")
        glProgram.uModel = GLES20.glGetUniformLocation(id, "uModelMatrix")
        glProgram.uColor = GLES20.glGetUniformLocation(id, "uColor")
        glProgram.uLight = GLES20.glGetUniformLocation(id, "uLightDir")
        glProgram.uAlpha = GLES20.glGetUniformLocation(id, "uAlpha")
        glProgram.uUseVColor = GLES20.glGetUniformLocation(id, "uUseVColor")
        return glProgram
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}

/**
 * 着色器程序句柄
 */
class GLProgram {
    var id = 0
    var aPosition = 0
    var aNormal = 0
    var aColor = -1
    var uMVP = 0
    var uModel = 0
    var uColor = 0
    var uLight = 0
    var uAlpha = 0
    var uUseVColor = -1
}

/**
 * 几何形状：顶点格式 [px,py,pz, nx,ny,nz] 交错排列；可选独立顶点色缓冲 [r,g,b,a]
 */
class GLShape(vertexData: FloatArray, colorData: FloatArray? = null) {

    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertexData)
            position(0)
        }

    private val colorBuffer: FloatBuffer? = colorData?.let {
        ByteBuffer
            .allocateDirect(it.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(it)
                position(0)
            }
    }

    val vertexCount: Int = vertexData.size / 6

    fun draw(
        program: GLProgram,
        vpMatrix: FloatArray,
        modelMatrix: FloatArray,
        mvpScratch: FloatArray,
        r: Float, g: Float, b: Float,
        alpha: Float = 1f
    ) {
        if (program.id == 0 || vertexCount == 0) return
        Matrix.multiplyMM(mvpScratch, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(program.uMVP, 1, false, mvpScratch, 0)
        GLES20.glUniformMatrix4fv(program.uModel, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(program.uColor, r, g, b)
        GLES20.glUniform1f(program.uAlpha, alpha)

        buffer.position(0)
        GLES20.glVertexAttribPointer(program.aPosition, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(3)
        GLES20.glVertexAttribPointer(program.aNormal, 3, GLES20.GL_FLOAT, false, 24, buffer)

        val cb = colorBuffer
        if (cb != null && program.aColor >= 0 && program.uUseVColor >= 0) {
            GLES20.glUniform1f(program.uUseVColor, 1f)
            GLES20.glEnableVertexAttribArray(program.aColor)
            cb.position(0)
            GLES20.glVertexAttribPointer(program.aColor, 4, GLES20.GL_FLOAT, false, 16, cb)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(program.aColor)
        } else {
            if (program.uUseVColor >= 0) GLES20.glUniform1f(program.uUseVColor, 0f)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        }
    }
}

// ==================== 几何体生成 ====================

/**
 * 旋转体（Lathe）网格：stations = (y, rx, rz) 自下而上，椭圆截面；
 * 支持 [startDeg, endDeg] 部分角度（竖条纹扇区 / 左右拼色半边）；
 * 法线含轮廓斜率（锥度光照正确）；可选端盖。
 */
private fun buildRevolution(
    stations: List<Triple<Float, Float, Float>>,
    radialSegments: Int = 14,
    startDeg: Float = 0f,
    endDeg: Float = 360f,
    capTop: Boolean = true,
    capBottom: Boolean = true
): FloatArray {
    val out = ArrayList<Float>(stations.size * (radialSegments + 1) * 36)
    val n = stations.size
    if (n < 2) return FloatArray(0)
    val a0 = Math.toRadians(startDeg.toDouble())
    val a1 = Math.toRadians(endDeg.toDouble())

    // 每层环：位置 + 斜率法线
    val ringPX = ArrayList<FloatArray>(n)
    val ringPZ = ArrayList<FloatArray>(n)
    val ringNX = ArrayList<FloatArray>(n)
    val ringNY = ArrayList<FloatArray>(n)
    val ringNZ = ArrayList<FloatArray>(n)
    for (i in 0 until n) {
        val y = stations[i].first
        val rx = stations[i].second
        val rz = stations[i].third
        val iPrev = if (i == 0) 0 else i - 1
        val iNext = if (i == n - 1) n - 1 else i + 1
        val dy = stations[iNext].first - stations[iPrev].first
        val dr = stations[iNext].second - stations[iPrev].second
        val slope = if (kotlin.math.abs(dy) < 0.0001f) 0f else dr / dy
        val px = FloatArray(radialSegments + 1)
        val pz = FloatArray(radialSegments + 1)
        val nx = FloatArray(radialSegments + 1)
        val ny = FloatArray(radialSegments + 1)
        val nz = FloatArray(radialSegments + 1)
        for (j in 0..radialSegments) {
            val a = a0 + (a1 - a0) * j / radialSegments
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            px[j] = ca * rx
            pz[j] = sa * rz
            var vx = ca
            var vy = -slope
            var vz = sa
            val len = kotlin.math.sqrt(vx * vx + vy * vy + vz * vz)
            nx[j] = vx / len
            ny[j] = vy / len
            nz[j] = vz / len
        }
        ringPX.add(px)
        ringPZ.add(pz)
        ringNX.add(nx)
        ringNY.add(ny)
        ringNZ.add(nz)
    }

    // 侧面（每层之间连成四边形 → 两个三角）
    for (i in 0 until n - 1) {
        val y0 = stations[i].first
        val y1 = stations[i + 1].first
        for (j in 0 until radialSegments) {
            out.addAll(
                listOf(
                    ringPX[i][j], y0, ringPZ[i][j], ringNX[i][j], ringNY[i][j], ringNZ[i][j],
                    ringPX[i + 1][j], y1, ringPZ[i + 1][j], ringNX[i + 1][j], ringNY[i + 1][j], ringNZ[i + 1][j],
                    ringPX[i + 1][j + 1], y1, ringPZ[i + 1][j + 1], ringNX[i + 1][j + 1], ringNY[i + 1][j + 1], ringNZ[i + 1][j + 1],
                    ringPX[i][j], y0, ringPZ[i][j], ringNX[i][j], ringNY[i][j], ringNZ[i][j],
                    ringPX[i + 1][j + 1], y1, ringPZ[i + 1][j + 1], ringNX[i + 1][j + 1], ringNY[i + 1][j + 1], ringNZ[i + 1][j + 1],
                    ringPX[i][j + 1], y0, ringPZ[i][j + 1], ringNX[i][j + 1], ringNY[i][j + 1], ringNZ[i][j + 1]
                )
            )
        }
    }

    // 底盖（扇形收拢到中轴）
    if (capBottom && stations.first().second > 0.001f) {
        val y = stations.first().first
        for (j in 0 until radialSegments) {
            out.addAll(
                listOf(
                    0f, y, 0f, 0f, -1f, 0f,
                    ringPX[0][j + 1], y, ringPZ[0][j + 1], 0f, -1f, 0f,
                    ringPX[0][j], y, ringPZ[0][j], 0f, -1f, 0f
                )
            )
        }
    }
    // 顶盖
    if (capTop && stations.last().second > 0.001f) {
        val y = stations.last().first
        for (j in 0 until radialSegments) {
            out.addAll(
                listOf(
                    0f, y, 0f, 0f, 1f, 0f,
                    ringPX[n - 1][j], y, ringPZ[n - 1][j], 0f, 1f, 0f,
                    ringPX[n - 1][j + 1], y, ringPZ[n - 1][j + 1], 0f, 1f, 0f
                )
            )
        }
    }

    return out.toFloatArray()
}

/** 单位球（半径 1，中心原点），带法线 */
private fun buildSphere(latBands: Int = 8, lonBands: Int = 12): FloatArray {
    val out = ArrayList<Float>(latBands * lonBands * 36)
    for (i in 0 until latBands) {
        val t1 = Math.PI * i / latBands
        val t2 = Math.PI * (i + 1) / latBands
        for (j in 0 until lonBands) {
            val p1 = 2.0 * Math.PI * j / lonBands
            val p2 = 2.0 * Math.PI * (j + 1) / lonBands
            val v00 = sphereVertex(t1, p1)
            val v01 = sphereVertex(t1, p2)
            val v10 = sphereVertex(t2, p1)
            val v11 = sphereVertex(t2, p2)
            out.addAll(v00); out.addAll(v10); out.addAll(v11)
            out.addAll(v00); out.addAll(v11); out.addAll(v01)
        }
    }
    return out.toFloatArray()
}

private fun sphereVertex(theta: Double, phi: Double): List<Float> {
    val sx = sin(theta) * cos(phi)
    val sy = cos(theta)
    val sz = sin(theta) * sin(phi)
    return listOf(
        sx.toFloat(), sy.toFloat(), sz.toFloat(),
        sx.toFloat(), sy.toFloat(), sz.toFloat()
    )
}

/** 单位立方体（边长 1，中心原点），每面独立法线 */
private fun buildCube(): FloatArray {
    val out = ArrayList<Float>(36 * 6)
    val h = 0.5f

    fun quad(a: List<Float>, b: List<Float>, c: List<Float>, d: List<Float>, n: List<Float>) {
        for (v in listOf(a, b, c, a, c, d)) {
            out.addAll(listOf(v[0], v[1], v[2], n[0], n[1], n[2]))
        }
    }

    // +Z / -Z
    quad(listOf(-h, -h, h), listOf(h, -h, h), listOf(h, h, h), listOf(-h, h, h), listOf(0f, 0f, 1f))
    quad(listOf(h, -h, -h), listOf(-h, -h, -h), listOf(-h, h, -h), listOf(h, h, -h), listOf(0f, 0f, -1f))
    // +X / -X
    quad(listOf(h, -h, h), listOf(h, -h, -h), listOf(h, h, -h), listOf(h, h, h), listOf(1f, 0f, 0f))
    quad(listOf(-h, -h, -h), listOf(-h, -h, h), listOf(-h, h, h), listOf(-h, h, -h), listOf(-1f, 0f, 0f))
    // +Y / -Y
    quad(listOf(-h, h, h), listOf(h, h, h), listOf(h, h, -h), listOf(-h, h, -h), listOf(0f, 1f, 0f))
    quad(listOf(-h, -h, -h), listOf(h, -h, -h), listOf(h, -h, h), listOf(-h, -h, h), listOf(0f, -1f, 0f))

    return out.toFloatArray()
}

/** XZ 平面单位方格（中心原点，法线 +Y） */
private fun buildQuad(): FloatArray {
    val h = 0.5f
    val out = ArrayList<Float>(36)
    for (v in listOf(
        listOf(-h, 0f, -h), listOf(h, 0f, -h), listOf(h, 0f, h),
        listOf(-h, 0f, -h), listOf(h, 0f, h), listOf(-h, 0f, h)
    )) {
        out.addAll(listOf(v[0], v[1], v[2], 0f, 1f, 0f))
    }
    return out.toFloatArray()
}

/** XZ 平面圆环（法线 +Y），用于中圈/选中标记 */
private fun buildRing(innerR: Float, outerR: Float, segments: Int = 32): FloatArray {
    val out = ArrayList<Float>(segments * 36)
    for (i in 0 until segments) {
        val a1 = 2.0 * Math.PI * i / segments
        val a2 = 2.0 * Math.PI * (i + 1) / segments
        val o1 = listOf(outerR * cos(a1).toFloat(), 0f, outerR * sin(a1).toFloat())
        val o2 = listOf(outerR * cos(a2).toFloat(), 0f, outerR * sin(a2).toFloat())
        val i1 = listOf(innerR * cos(a1).toFloat(), 0f, innerR * sin(a1).toFloat())
        val i2 = listOf(innerR * cos(a2).toFloat(), 0f, innerR * sin(a2).toFloat())
        for (v in listOf(o1, i1, i2, o1, i2, o2)) {
            out.addAll(listOf(v[0], v[1], v[2], 0f, 1f, 0f))
        }
    }
    return out.toFloatArray()
}
