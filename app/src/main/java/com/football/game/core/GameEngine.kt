package com.football.game.core

import com.football.game.model.Match
import com.football.game.model.Player
import kotlin.random.Random

/**
 * 游戏引擎核心类
 */
class GameEngine(
    val match: Match,
    val homePlayers: List<Player>,
    val awayPlayers: List<Player>
) {
    var ballPosition = Vector3.ZERO
    var ballVelocity = Vector3.ZERO
    var ballHeight = 0f
    var ballHeightVelocity = 0f
    var ballOwner: Player? = null
    
    var activePlayer: Player? = null
    var playerSide: GameState.TeamSide = GameState.TeamSide.HOME
    var inputVector = Vector2D.ZERO
    var isSprinting = false
    
    data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
        companion object { val ZERO = Vector2D() }
        fun normalized(): Vector2D {
            val len = kotlin.math.sqrt(x * x + y * y)
            return if (len > 0.001f) Vector2D(x / len, y / len) else ZERO
        }
    }
    
    fun doPass() {
        val player = activePlayer ?: return
        if (ballOwner != player) return
        
        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1
        
        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE
        
        for (teammate in team) {
            if (teammate == player) continue
            val forwardScore = (teammate.position.z - player.position.z) * forwardDir
            val dist = player.position.distanceTo(teammate.position)
            if (dist in 3f..40f) {
                val score = forwardScore - dist * 0.2f
                if (score > bestScore) {
                    bestScore = score
                    bestTarget = teammate
                }
            }
        }
        
        if (bestTarget != null) {
            val dir = (bestTarget.position - player.position).flatten().normalized()
            ballVelocity = dir * GameState.PASS_SPEED
            ballHeightVelocity = 0.5f
            ballOwner = null
            player.hasBall = false
        }
    }
    
    fun doShoot() {
        val player = activePlayer ?: return
        if (ballOwner != player) return
        
        val targetGoalZ = if (player.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }
        
        val dir = Vector3(
            Random.nextFloat() * 6f - 3f - player.position.x * 0.05f,
            0f,
            targetGoalZ - player.position.z
        ).normalized()
        
        ballVelocity = dir * GameState.SHOT_SPEED
        ballHeightVelocity = 2f + Random.nextFloat() * 2f
        ballOwner = null
        player.hasBall = false
    }
    
    fun doThroughBall() {
        val player = activePlayer ?: return
        if (ballOwner != player) return
        
        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1
        
        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE
        
        for (teammate in team) {
            if (teammate == player) continue
            val forwardScore = (teammate.position.z - player.position.z) * forwardDir
            val dist = player.position.distanceTo(teammate.position)
            if (dist in 5f..40f && forwardScore > 5) {
                if (forwardScore > bestScore) {
                    bestScore = forwardScore
                    bestTarget = teammate
                }
            }
        }
        
        if (bestTarget != null) {
            val lead = Vector3(0f, 0f, forwardDir * 8f)
            val targetPos = bestTarget.position + lead
            val dir = (targetPos - player.position).flatten().normalized()
            ballVelocity = dir * (GameState.PASS_SPEED * 1.3f)
            ballHeightVelocity = 0.3f
            ballOwner = null
            player.hasBall = false
        }
    }
    
    fun switchPlayer() {
        val team = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        if (team.isEmpty()) return
        
        val currentIdx = team.indexOf(activePlayer)
        for (i in 1..team.size) {
            val idx = (currentIdx + i) % team.size
            if (!team[idx].isGoalkeeper) {
                activePlayer?.isActive = false
                activePlayer?.isPlayerControlled = false
                activePlayer = team[idx]
                team[idx].isActive = true
                team[idx].isPlayerControlled = true
                return
            }
        }
    }
}