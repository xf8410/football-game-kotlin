package com.football.game.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.football.game.core.GameState
import com.football.game.core.Vector3
import com.football.game.data.HairStyle3D
import com.football.game.data.PlayerLook
import com.football.game.model.Player
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * 游戏渲染器
 * OpenGL ES 2.0：真实关节式球员模型（头/发型/躯干/手臂/腿/球袜），
 * 条纹草皮 + 白线球场 + 球门，替代旧的"胶囊/圆点"占位渲染
 */
class GameRenderer : GLSurfaceView.Renderer {

    // ==================== 着色器 ====================

    private val vertexShader = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        attribute vec4 aPosition;
        attribute vec3 aNormal;
        varying vec3 vNormal;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vNormal = (uModelMatrix * vec4(aNormal, 0.0)).xyz;
        }
    """

    private val fragmentShader = """
        precision mediump float;
        uniform vec3 uColor;
        uniform vec3 uLightDir;
        uniform float uAlpha;
        varying vec3 vNormal;
        void main() {
            vec3 n = normalize(vNormal);
            float diff = max(dot(n, normalize(uLightDir)), 0.0);
            float lambert = 0.62 + 0.38 * diff;
            gl_FragColor = vec4(uColor * lambert, uAlpha);
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
    private lateinit var cube: GLShape
    private lateinit var quad: GLShape
    private lateinit var centerCircle: GLShape
    private lateinit var markerRing: GLShape
    private var ready = false
    private var frameTime = 0f

    // ==================== 游戏数据 ====================

    private var homePlayers: List<Player> = emptyList()
    private var awayPlayers: List<Player> = emptyList()
    private var homeLooks: List<PlayerLook> = emptyList()
    private var awayLooks: List<PlayerLook> = emptyList()
    private var ballPosition = Vector3.ZERO
    private var ballHeight = 0f
    private var activePlayerIndex = -1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.13f, 0.36f, 0.19f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        program = buildProgram()
        if (program.id == 0) return

        sphere = GLShape(buildSphere(latBands = 8, lonBands = 12))
        cube = GLShape(buildCube())
        quad = GLShape(buildQuad())
        centerCircle = GLShape(buildRing(innerR = 8.9f, outerR = 9.15f))
        markerRing = GLShape(buildRing(innerR = 0.34f, outerR = 0.42f, segments = 28))

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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(program.id)
        GLES20.glUniform3f(program.uLight, 0.4f, -0.85f, -0.35f)

        // 相机跟随球
        camTargetX = lerp(camTargetX, ballPosition.x, 0.08f)
        camTargetZ = lerp(camTargetZ, ballPosition.z, 0.08f)
        camPosX = lerp(camPosX, camTargetX * 0.85f, 0.08f)
        camPosY = lerp(camPosY, 36f, 0.08f)
        camPosZ = lerp(camPosZ, camTargetZ - 30f, 0.08f)

        Matrix.setLookAtM(
            viewMatrix, 0,
            camPosX, camPosY, camPosZ,
            camTargetX, 0f, camTargetZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        drawField()
        drawAllPlayers()
        drawBall()
        frameTime += 0.016f
    }

    // ==================== 球场 ====================

    private fun drawField() {
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2

        // 外围草皮
        drawShapeAt(quad, 0f, -0.02f, 0f, 140f, 1f, 190f, 0f, 0.12f, 0.36f, 0.19f)

        // 割草条纹（10 条）
        val stripeLen = GameState.FIELD_LENGTH / 10f
        for (i in 0 until 10) {
            val zc = -halfL + stripeLen * (i + 0.5f)
            if (i % 2 == 0) {
                drawShapeAt(quad, 0f, 0f, zc, GameState.FIELD_WIDTH, 1f, stripeLen, 0f, 0.16f, 0.55f, 0.26f)
            } else {
                drawShapeAt(quad, 0f, 0f, zc, GameState.FIELD_WIDTH, 1f, stripeLen, 0f, 0.14f, 0.50f, 0.23f)
            }
        }

        // ---- 白线 ----
        val lw = 0.15f
        // 边线
        drawShapeAt(quad, -halfW, 0.03f, 0f, lw, 1f, GameState.FIELD_LENGTH + lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, halfW, 0.03f, 0f, lw, 1f, GameState.FIELD_LENGTH + lw, 0f, 0.95f, 0.95f, 0.95f)
        // 底线
        drawShapeAt(quad, 0f, 0.03f, -halfL, GameState.FIELD_WIDTH + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
        drawShapeAt(quad, 0f, 0.03f, halfL, GameState.FIELD_WIDTH + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
        // 中线
        drawShapeAt(quad, 0f, 0.03f, 0f, GameState.FIELD_WIDTH + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
        // 中圈
        drawShapeAt(centerCircle, 0f, 0.05f, 0f, 1f, 1f, 1f, 0f, 0.95f, 0.95f, 0.95f)
        // 中点
        drawShapeAt(quad, 0f, 0.04f, 0f, 0.4f, 1f, 0.4f, 0f, 0.95f, 0.95f, 0.95f)

        // 两端禁区 + 小禁区 + 点球点 + 球门
        for (sign in floatArrayOf(-1f, 1f)) {
            val goalZ = halfL * sign
            val dir = -sign
            // 大禁区：宽 40.32 深 16.5
            val boxW = 40.32f
            val boxD = 16.5f
            drawShapeAt(quad, 0f, 0.03f, goalZ + dir * boxD, boxW + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, -boxW / 2, 0.03f, goalZ + dir * boxD / 2, lw, 1f, boxD, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, boxW / 2, 0.03f, goalZ + dir * boxD / 2, lw, 1f, boxD, 0f, 0.95f, 0.95f, 0.95f)
            // 小禁区：宽 18.32 深 5.5
            val gW = 18.32f
            val gD = 5.5f
            drawShapeAt(quad, 0f, 0.03f, goalZ + dir * gD, gW + lw, 1f, lw, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, -gW / 2, 0.03f, goalZ + dir * gD / 2, lw, 1f, gD, 0f, 0.95f, 0.95f, 0.95f)
            drawShapeAt(quad, gW / 2, 0.03f, goalZ + dir * gD / 2, lw, 1f, gD, 0f, 0.95f, 0.95f, 0.95f)
            // 点球点
            drawShapeAt(quad, 0f, 0.04f, goalZ + dir * 11f, 0.35f, 1f, 0.35f, 0f, 0.95f, 0.95f, 0.95f)
            // 球门（门柱 + 横梁）
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
            drawPlayer(
                player = player,
                look = homeLooks.getOrNull(index) ?: defaultLook(true),
                index = index,
                isActive = index == activePlayerIndex
            )
        }
        for ((index, player) in awayPlayers.withIndex()) {
            drawPlayer(
                player = player,
                look = awayLooks.getOrNull(index) ?: defaultLook(false),
                index = index + 11,
                isActive = false
            )
        }
    }

    /**
     * 关节式球员模型：
     * 阴影 → 双腿(摆动) → 球袜 → 球裤 → 躯干 → 球袖 → 双臂(摆动) → 头 → 发型 → 选中标记
     */
    private fun drawPlayer(player: Player, look: PlayerLook, index: Int, isActive: Boolean) {
        val pos = player.position
        val vel = player.velocity
        val speed = kotlin.math.sqrt(vel.x * vel.x + vel.z * vel.z)
        val face = if (speed > 0.1f) vel else player.facingDirection
        val rotY = Math.toDegrees(
            kotlin.math.atan2(face.x.toDouble(), face.z.toDouble())
        ).toFloat()

        val amp = (speed / 6f).coerceIn(0f, 1f)
        val phase = frameTime * (4f + speed * 1.6f) + index * 1.9f
        val swing = sin(phase) * 32f * amp

        val kit1 = rgb(look.kitColor1)
        val kit2 = rgb(look.kitColor2)
        val skin = rgb(look.skinColor)
        val hairC = rgb(look.hairColor)

        // 阴影
        drawShapeAt(quad, pos.x, 0.02f, pos.z, 0.55f, 1f, 0.55f, 0f, 0f, 0f, 0f, 0.28f)

        // 双腿（肤色）+ 球袜
        drawBody(cube, pos.x, pos.z, rotY, -0.11f, 0.80f, 0f, swing, -0.30f, 0.15f, 0.60f, 0.17f, skin[0], skin[1], skin[2])
        drawBody(cube, pos.x, pos.z, rotY, 0.11f, 0.80f, 0f, -swing, -0.30f, 0.15f, 0.60f, 0.17f, skin[0], skin[1], skin[2])
        drawBody(cube, pos.x, pos.z, rotY, -0.11f, 0.38f, 0f, swing, 0f, 0.16f, 0.18f, 0.18f, kit2[0], kit2[1], kit2[2])
        drawBody(cube, pos.x, pos.z, rotY, 0.11f, 0.38f, 0f, -swing, 0f, 0.16f, 0.18f, 0.18f, kit2[0], kit2[1], kit2[2])

        // 球裤
        drawBody(cube, pos.x, pos.z, rotY, 0f, 0.90f, 0f, 0f, 0f, 0.34f, 0.22f, 0.24f, kit2[0], kit2[1], kit2[2])
        // 躯干（球衣）
        drawBody(cube, pos.x, pos.z, rotY, 0f, 1.30f, 0f, 0f, 0f, 0.44f, 0.60f, 0.26f, kit1[0], kit1[1], kit1[2])
        // 球袖
        drawBody(cube, pos.x, pos.z, rotY, -0.29f, 1.50f, 0f, 0f, 0f, 0.14f, 0.16f, 0.17f, kit1[0], kit1[1], kit1[2])
        drawBody(cube, pos.x, pos.z, rotY, 0.29f, 1.50f, 0f, 0f, 0f, 0.14f, 0.16f, 0.17f, kit1[0], kit1[1], kit1[2])
        // 双臂（肤色，反向摆动）
        drawBody(cube, pos.x, pos.z, rotY, -0.29f, 1.52f, 0f, -swing * 0.8f, -0.24f, 0.09f, 0.48f, 0.11f, skin[0], skin[1], skin[2])
        drawBody(cube, pos.x, pos.z, rotY, 0.29f, 1.52f, 0f, swing * 0.8f, -0.24f, 0.09f, 0.48f, 0.11f, skin[0], skin[1], skin[2])

        // 头（肤色球体）
        drawBody(sphere, pos.x, pos.z, rotY, 0f, 1.74f, 0f, 0f, 0f, 0.15f, 0.165f, 0.15f, skin[0], skin[1], skin[2])

        // 发型（俯视角下最醒目的球星特征）
        drawHair(look.hairStyle3D, hairC, pos.x, pos.z, rotY)

        // 受控球员标记
        if (isActive) {
            val bob = sin(frameTime * 4f) * 0.05f
            drawShapeAt(markerRing, pos.x, 2.15f + bob, pos.z, 1f, 1f, 1f, 0f, 1f, 0.84f, 0.0f, 0.95f)
        }
    }

    private fun drawHair(style: HairStyle3D, hair: FloatArray, px: Float, pz: Float, rotY: Float) {
        when (style) {
            HairStyle3D.BUZZ ->
                drawBody(sphere, px, pz, rotY, 0f, 1.78f, 0f, 0f, 0f, 0.155f, 0.125f, 0.155f, hair[0], hair[1], hair[2])
            HairStyle3D.SHORT ->
                drawBody(sphere, px, pz, rotY, 0f, 1.78f, 0f, 0f, 0f, 0.16f, 0.145f, 0.16f, hair[0], hair[1], hair[2])
            HairStyle3D.FLOW ->
                drawBody(sphere, px, pz, rotY, 0f, 1.77f, -0.03f, 0f, 0f, 0.17f, 0.155f, 0.20f, hair[0], hair[1], hair[2])
            HairStyle3D.AFRO ->
                drawBody(sphere, px, pz, rotY, 0f, 1.80f, 0f, 0f, 0f, 0.22f, 0.20f, 0.22f, hair[0], hair[1], hair[2])
            HairStyle3D.CURLY ->
                drawBody(sphere, px, pz, rotY, 0f, 1.78f, 0f, 0f, 0f, 0.175f, 0.16f, 0.175f, hair[0], hair[1], hair[2])
            HairStyle3D.PONYTAIL -> {
                drawBody(sphere, px, pz, rotY, 0f, 1.78f, 0f, 0f, 0f, 0.16f, 0.145f, 0.16f, hair[0], hair[1], hair[2])
                drawBody(sphere, px, pz, rotY, 0f, 1.58f, -0.17f, 0f, 0f, 0.07f, 0.11f, 0.07f, hair[0], hair[1], hair[2])
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

    // ==================== 球 ====================

    private fun drawBall() {
        // 球体
        drawShapeAt(
            sphere,
            ballPosition.x, 0.11f + ballHeight, ballPosition.z,
            0.11f, 0.11f, 0.11f, 0f,
            0.96f, 0.96f, 0.96f
        )
        // 影子
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
     * 平移到球员 → 朝向旋转 → 部件偏移 → rotX 摆动 → pivotDrop 下移 → 缩放
     */
    private fun drawBody(
        shape: GLShape,
        px: Float, pz: Float, rotY: Float,
        ox: Float, oy: Float, oz: Float,
        rotX: Float,
        pivotDrop: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        alpha: Float = 1f
    ) {
        if (!ready) return
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, 0f, pz)
        if (rotY != 0f) Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
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
     * 设置游戏数据（每帧由 UI 线程调用）
     */
    fun setGameData(
        homePlayers: List<Player>,
        awayPlayers: List<Player>,
        ballPosition: Vector3,
        ballHeight: Float,
        activePlayerIndex: Int,
        homeLooks: List<PlayerLook> = emptyList(),
        awayLooks: List<PlayerLook> = emptyList()
    ) {
        this.homePlayers = homePlayers
        this.awayPlayers = awayPlayers
        this.ballPosition = ballPosition
        this.ballHeight = ballHeight
        this.activePlayerIndex = activePlayerIndex
        if (homeLooks.isNotEmpty()) this.homeLooks = homeLooks
        if (awayLooks.isNotEmpty()) this.awayLooks = awayLooks
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
        glProgram.uMVP = GLES20.glGetUniformLocation(id, "uMVPMatrix")
        glProgram.uModel = GLES20.glGetUniformLocation(id, "uModelMatrix")
        glProgram.uColor = GLES20.glGetUniformLocation(id, "uColor")
        glProgram.uLight = GLES20.glGetUniformLocation(id, "uLightDir")
        glProgram.uAlpha = GLES20.glGetUniformLocation(id, "uAlpha")
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
    var uMVP = 0
    var uModel = 0
    var uColor = 0
    var uLight = 0
    var uAlpha = 0
}

/**
 * 几何形状：顶点格式 [px,py,pz, nx,ny,nz] 交错排列
 */
class GLShape(vertexData: FloatArray) {

    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertexData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertexData)
            position(0)
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }
}

// ==================== 几何体生成 ====================

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
