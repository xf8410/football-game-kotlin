package com.football.game.ai

import com.football.game.core.GameState
import com.football.game.core.Vector3
import com.football.game.model.Player
import kotlin.random.Random

/**
 * 球员AI（第一层）
 */
class PlayerAI {
    companion object {
        private const val DECISION_INTERVAL = 0.15f
    }
    
    private var decisionTimer = 0f
    private var currentTarget = Vector3.ZERO
    private var currentAction = "idle"
    
    fun update(
        player: Player,
        delta: Float,
        hasBall: Boolean,
        isNearestToBall: Boolean,
        ballPosition: Vector3,
        teammates: List<Player>,
        opponents: List<Player>,
        aiParams: GameState.AIParams,
        teamAI: TeamAI? = null,
        eventModifiers: Map<String, Any>? = null
    ) {
        decisionTimer += delta
        if (decisionTimer < DECISION_INTERVAL) {
            applyMovement(player, delta)
            return
        }
        decisionTimer = 0f
        
        when {
            hasBall -> decideWithBall(player, ballPosition, teammates, opponents, aiParams, teamAI)
            isNearestToBall -> decideChaseBall(player, ballPosition, aiParams)
            else -> decideOffBall(player, ballPosition, teammates, opponents, teamAI, eventModifiers)
        }
        
        applyMovement(player, delta)
    }
    
    private fun decideWithBall(
        player: Player,
        ballPosition: Vector3,
        teammates: List<Player>,
        opponents: List<Player>,
        aiParams: GameState.AIParams,
        teamAI: TeamAI?
    ) {
        val targetGoalZ = if (player.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }
        val distToGoal = kotlin.math.abs(player.position.z - targetGoalZ)
        
        if (distToGoal < 25f) {
            val shootProb = aiParams.passAccuracy * 0.5f
            if (Random.nextFloat() < shootProb) {
                currentAction = "shoot"
                return
            }
        }
        
        val passPref = teamAI?.getPassPreference() ?: 0.4f
        val passProb = aiParams.passAccuracy * 0.3f * (1f + passPref)
        if (Random.nextFloat() < passProb) {
            currentAction = "pass"
            return
        }
        
        val goalDir = Vector3(0f, 0f, targetGoalZ - player.position.z).normalized()
        currentTarget = player.position + goalDir * 5f
        currentAction = "dribble"
    }
    
    private fun decideChaseBall(player: Player, ballPosition: Vector3, aiParams: GameState.AIParams) {
        currentTarget = ballPosition
        currentAction = "chase"
    }
    
    private fun decideOffBall(
        player: Player,
        ballPosition: Vector3,
        teammates: List<Player>,
        opponents: List<Player>,
        teamAI: TeamAI?,
        eventModifiers: Map<String, Any>?
    ) {
        val homePos = player.homePosition
        val offset = teamAI?.getFormationOffset(player.teamSide) ?: Vector3.ZERO
        
        currentTarget = homePos + offset
        currentAction = "position"
    }
    
    private fun applyMovement(player: Player, delta: Float) {
        val toTarget = currentTarget - player.position
        
        if (toTarget.length() > 0.5f) {
            player.inputDirection = toTarget.normalized()
            player.isSprinting = toTarget.length() > 10f && currentAction in listOf("chase", "dribble")
        } else {
            player.inputDirection = Vector3.ZERO
            player.isSprinting = false
        }
    }
}