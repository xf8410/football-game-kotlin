package com.football.game.core

import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 物理系统
 */
class PhysicsSystem {
    
    companion object {
        val FIELD_BOUNDS = BoundingBox(
            min = Vector3(-GameState.FIELD_WIDTH / 2, 0f, -GameState.FIELD_LENGTH / 2),
            max = Vector3(GameState.FIELD_WIDTH / 2, 0.5f, GameState.FIELD_LENGTH / 2)
        )
    }
    
    /**
     * 处理两个球员之间的碰撞
     */
    fun resolvePlayerCollision(p1: PlayerModel, p2: PlayerModel): Boolean {
        val dist = p1.position.distanceTo(p2.position)
        val minDist = p1.bodyRadius + p2.bodyRadius
        
        if (dist < 0.001f || dist >= minDist) return false
        
        val pushDir = (p1.position - p2.position).normalized()
        val overlap = minDist - dist
        
        val p1Push = overlap * 0.5f
        val p2Push = overlap * 0.5f
        
        p1.position = p1.position + pushDir * p1Push
        p2.position = p2.position - pushDir * p2Push
        
        return true
    }
    
    /**
     * 处理球员与球场边界的碰撞
     */
    fun resolveFieldBounds(player: PlayerModel) {
        val pos = player.position
        val radius = player.bodyRadius
        
        if (pos.x - radius < -GameState.FIELD_WIDTH / 2) {
            player.position = Vector3(-GameState.FIELD_WIDTH / 2 + radius, 0f, pos.z)
        }
        if (pos.x + radius > GameState.FIELD_WIDTH / 2) {
            player.position = Vector3(GameState.FIELD_WIDTH / 2 - radius, 0f, pos.z)
        }
        if (pos.z - radius < -GameState.FIELD_LENGTH / 2) {
            player.position = Vector3(pos.x, 0f, -GameState.FIELD_LENGTH / 2 + radius)
        }
        if (pos.z + radius > GameState.FIELD_LENGTH / 2) {
            player.position = Vector3(pos.x, 0f, GameState.FIELD_LENGTH / 2 - radius)
        }
    }
    
    /**
     * 更新球的物理
     */
    fun updateBallPhysics(ball: Ball, players: List<PlayerModel>, delta: Float) {
        if (ball.owner != null) {
            updateBallWithOwner(ball, ball.owner!!, delta)
        } else {
            updateFreeBall(ball, delta)
        }
        checkBallPossession(ball, players)
    }
    
    private fun updateBallWithOwner(ball: Ball, owner: PlayerModel, delta: Float) {
        val footOffset = Vector3(
            cos(owner.rotation) * 0.6f,
            0.1f,
            sin(owner.rotation) * 0.6f
        )
        val targetPos = owner.position + footOffset
        ball.position = ball.position.lerp(targetPos, delta * 15f)
        ball.velocity = Vector3.ZERO
        ball.height = 0.1f
        ball.heightVelocity = 0f
    }
    
    private fun updateFreeBall(ball: Ball, delta: Float) {
        if (ball.height < 0.2f) {
            ball.velocity = ball.velocity * (1f - GameState.BALL_FRICTION * delta)
        }
        if (ball.height > 0.1f) {
            ball.velocity = ball.velocity * (1f - GameState.BALL_AIR_DRAG * delta)
        }
        
        if (ball.height > 0f || ball.heightVelocity > 0f) {
            ball.heightVelocity -= GameState.BALL_GRAVITY * delta
            ball.height += ball.heightVelocity * delta
            if (ball.height < 0f) {
                ball.height = 0f
                ball.heightVelocity = if (kotlin.math.abs(ball.heightVelocity) > 1f) {
                    -ball.heightVelocity * 0.5f
                } else {
                    0f
                }
            }
        }
        
        ball.position = ball.position + ball.velocity * delta
        ball.position = Vector3(
            ball.position.x.coerceIn(-GameState.FIELD_WIDTH / 2, GameState.FIELD_WIDTH / 2),
            0f,
            ball.position.z
        )
    }
    
    private fun checkBallPossession(ball: Ball, players: List<PlayerModel>) {
        if (ball.owner != null) return
        if (ball.height > 1.5f) return
        
        var nearestPlayer: PlayerModel? = null
        var nearestDist = Float.MAX_VALUE
        
        for (player in players) {
            val dist = player.position.distanceTo(ball.position)
            if (dist < 1.2f && dist < nearestDist) {
                nearestDist = dist
                nearestPlayer = player
            }
        }
        
        if (nearestPlayer != null) {
            ball.owner = nearestPlayer
        }
    }
}

/**
 * 球数据类
 */
class Ball(
    var position: Vector3 = Vector3.ZERO,
    var velocity: Vector3 = Vector3.ZERO,
    var height: Float = 0f,
    var heightVelocity: Float = 0f,
    var spin: Float = 0f,
    var owner: PlayerModel? = null
)

// BoundingBox 统一由 PlayerModel.kt 提供（含 intersects/contains/overlap 完整实现），
// 此处不再重复声明，避免 Redeclaration 编译错误。
