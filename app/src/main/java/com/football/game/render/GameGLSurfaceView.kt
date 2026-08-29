package com.football.game.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.football.game.core.Vector3
import com.football.game.data.PlayerLook
import com.football.game.model.Player

/**
 * 游戏 OpenGL 视图
 * 封装 GLSurfaceView，处理触摸输入
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

    init {
        // 设置 OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // 设置渲染器
        renderer = GameRenderer()
        setRenderer(renderer)

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
     * 更新游戏数据（含球员外观）
     */
    fun updateGameData(
        homePlayers: List<Player>,
        awayPlayers: List<Player>,
        ballPosition: Vector3,
        ballHeight: Float,
        activePlayerIndex: Int,
        homeLooks: List<PlayerLook> = emptyList(),
        awayLooks: List<PlayerLook> = emptyList()
    ) {
        renderer?.setGameData(
            homePlayers = homePlayers,
            awayPlayers = awayPlayers,
            ballPosition = ballPosition,
            ballHeight = ballHeight,
            activePlayerIndex = activePlayerIndex,
            homeLooks = homeLooks,
            awayLooks = awayLooks
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
