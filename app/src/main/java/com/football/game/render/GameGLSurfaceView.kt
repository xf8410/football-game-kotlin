package com.football.game.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.football.game.core.RefereeState
import com.football.game.core.Vector3
import com.football.game.model.Player
import com.football.game.ui.component.PlayerLook

/**
 * 游戏 OpenGL 视图
 * 封装 GLSurfaceView，处理触摸输入
 *
 * 抽搐修复：比赛模拟改为由渲染线程逐帧驱动（onFrameUpdate），
 * update 与绘制同帧同相 → 不再出现"模拟线程 vs 渲染线程"不同步的画面抖动
 */
class GameGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private var renderer: GameRenderer? = null
    private var touchListener: OnTouchListener? = null

    // 触摸状态
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isTouching = false

    /**
     * 每帧模拟回调：GL 渲染线程每帧绘制前调用，参数 = 真实帧间隔（秒）。
     * MatchScreen 在这里推进 gameEngine.update(dt) 并推送渲染数据。
     * 附带好处：app 退到后台时 GL 停帧 → 模拟自动暂停（不再后台偷偷踢球）。
     */
    var onFrameUpdate: ((Float) -> Unit)? = null

    init {
        // 设置 OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // 设置渲染器
        val r = GameRenderer()
        renderer = r
        r.frameCallback = { dt -> onFrameUpdate?.invoke(dt) }
        setRenderer(r)

        // 连续渲染模式（游戏需要持续渲染）
        renderMode = RENDERMODE_CONTINUOUSLY

        // 启用透明度
        setZOrderOnTop(false)
    }

    /**
     * 设置触摸监听器
     */
    fun setGameTouchListener(listener: OnTouchListener) {
        this.touchListener = listener
    }

    /**
     * 更新游戏数据（含球员外观与裁判）
     */
    fun updateGameData(
        homePlayers: List<Player>,
        awayPlayers: List<Player>,
        ballPosition: Vector3,
        ballHeight: Float,
        activePlayerIndex: Int,
        homeLooks: List<PlayerLook> = emptyList(),
        awayLooks: List<PlayerLook> = emptyList(),
        referee: RefereeState? = null
    ) {
        renderer?.setGameData(
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            ballPosition = ballPosition,
            ballHeight = ballHeight,
            activePlayerIndex = activePlayerIndex,
            homeLooks = homeLooks,
            awayLooks = awayLooks,
            referee = referee
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y
                isTouching = true
                touchListener?.onTouchDown(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - lastTouchX
                val deltaY = y - lastTouchY
                lastTouchX = x
                lastTouchY = y
                touchListener?.onTouchMove(x, y, deltaX, deltaY)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                touchListener?.onTouchUp(x, y)
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * 触摸事件监听器接口
     */
    interface OnTouchListener {
        fun onTouchDown(x: Float, y: Float)
        fun onTouchMove(x: Float, y: Float, deltaX: Float, deltaY: Float)
        fun onTouchUp(x: Float, y: Float)
    }
}
