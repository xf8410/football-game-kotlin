package com.football.game.ai

import com.football.game.core.GameState
import com.football.game.core.Vector3
import com.football.game.model.Player
import kotlin.random.Random

/**
 * 单个球员AI（第一层）
 * 处理：接近球、选择站位、传球/射门/抢断、避开队友和边界、回到所属区域
 *
 * 三层AI架构：
 *   1. 球员AI（本类）- 单个球员的行为决策
 *   2. 球队战术AI（TeamAI）- 球队整体战术
 *   3. 活动修正（GameState中的event modifiers）- 特殊活动规则
 */
class PlayerAI {
    companion object {
        // AI决策频率（不需要每帧决策）
        private const val DECISION_INTERVAL = 0.15f
    }

    private var decisionTimer = 0.0f
    private var currentTarget = Vector3.ZERO
    private var currentAction = "idle"
    private var isChasingBall = false

    /**
     * 更新AI决策
     * @param player 球员
     * @param delta 帧间隔时间
     * @param hasBall 是否控球
     * @param isNearestToBall 是否离球最近
     * @param ballPosition 球的位置
     * @param teammates 队友列表
     * @param opponents 对手列表
     * @param aiParams AI参数
     * @param teamAI 球队战术AI引用
     * @param eventModifiers 活动修正参数
     */
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

        // 限制决策频率（性能优化）
        if (decisionTimer < DECISION_INTERVAL) {
            applyMovement(player, delta)
            return
        }
        decisionTimer = 0.0f

        // 根据情况决策
        when {
            hasBall -> decideWithBall(player, ballPosition, teammates, opponents, aiParams, teamAI)
            isNearestToBall -> decideChaseBall(player, ballPosition, aiParams)
            else -> decideOffBall(player, ballPosition, teammates, opponents, teamAI, eventModifiers)
        }

        applyMovement(player, delta)
    }

    /**
     * 有球时的决策
     */
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

        // 射门判定
        if (distToGoal < 25.0f) {
            val shootProb = aiParams.passAccuracy * 0.5f  // 使用传球准确率作为射门概率基础
            if (Random.nextFloat() < shootProb) {
                currentAction = "shoot"
                // 调用射门逻辑（在GameEngine中处理）
                return
            }
        }

        // 传球判定
        val passPref = teamAI?.getPassPreference() ?: 0.4f
        val passProb = aiParams.passAccuracy * 0.3f * (1.0f + passPref)
        if (Random.nextFloat() < passProb) {
            val bestTarget = findBestPassTarget(player, teammates, opponents, aiParams)
            if (bestTarget != null) {
                currentAction = "pass"
                // 调用传球逻辑（在GameEngine中处理）
                return
            }
        }

        // 带球前进
        val goalDir = Vector3(0.0f, 0.0f, targetGoalZ - player.position.z).normalized()
        val avoid = avoidOpponents(player, opponents)
        currentTarget = player.position + (goalDir + avoid * 2).normalized() * 5
        currentAction = "dribble"
    }

    /**
     * 追球时的决策
     */
    private fun decideChaseBall(player: Player, ballPosition: Vector3, aiParams: GameState.AIParams) {
        isChasingBall = true
        currentTarget = ballPosition
        currentAction = "chase"

        // 如果接近球，尝试抢断
        if (player.position.distanceTo(ballPosition) < 2.0f) {
            currentAction = "tackle"
            // 调用抢断逻辑（在GameEngine中处理）
        }
    }

    /**
     * 无球时的决策
     */
    private fun decideOffBall(
        player: Player,
        ballPosition: Vector3,
        teammates: List<Player>,
        opponents: List<Player>,
        teamAI: TeamAI?,
        eventModifiers: Map<String, Any>?
    ) {
        isChasingBall = false

        // 回到阵型位置 + 战术偏移
        val homePos = player.homePosition
        val offset = teamAI?.getFormationOffset(player.teamSide) ?: Vector3.ZERO
        val pressIntensity = teamAI?.getPressIntensity() ?: 0.5f

        // 根据球的位置调整站位
        val ballToHome = homePos - ballPosition

        // 如果球离自己近且需要逼抢，向球靠近
        if (ballToHome.length() < 20.0f && Random.nextFloat() < pressIntensity) {
            // 向球的方向移动，但保持一定距离
            val pressPos = ballPosition + ballToHome.normalized() * 5
            currentTarget = pressPos
            currentAction = "press"
        } else {
            // 回到阵型位置
            currentTarget = homePos + offset
            currentAction = "position"
        }

        // 门将特殊行为
        if (player.isGoalkeeper) {
            goalkeeperAI(player, ballPosition)
        }

        // 检查活动修正：AI不能进入特定区域
        checkZoneRestrictions(player, eventModifiers)
    }

    /**
     * 门将AI
     */
    private fun goalkeeperAI(player: Player, ballPosition: Vector3) {
        val targetGoalZ = if (player.teamSide == GameState.TeamSide.HOME) {
            -GameState.FIELD_LENGTH / 2
        } else {
            GameState.FIELD_LENGTH / 2
        }

        // 门将在小禁区内移动，跟随球的横向位置
        val gkX = ballPosition.x * 0.3f.coerceIn(-3.0f, 3.0f)
        val gkZ = targetGoalZ + if (player.teamSide == GameState.TeamSide.HOME) 5.0f else -5.0f
        currentTarget = Vector3(gkX, 0.0f, gkZ)

        // 如果球很近，冲出来
        val ballToGoal = kotlin.math.abs(ballPosition.z - targetGoalZ)
        if (ballToGoal < 15.0f && ballPosition.distanceTo(Vector3(gkX, 0.0f, gkZ)) < 8.0f) {
            currentTarget = ballPosition
            currentAction = "gk_rush"
        }
    }

    /**
     * 寻找最佳传球目标
     */
    private fun findBestPassTarget(
        player: Player,
        teammates: List<Player>,
        opponents: List<Player>,
        aiParams: GameState.AIParams
    ): Player? {
        var bestTarget: Player? = null
        var bestScore = -999.0f

        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1

        for (teammate in teammates) {
            if (teammate == player || teammate.isGoalkeeper) continue

            val dist = player.position.distanceTo(teammate.position)
            if (dist < 5 || dist > 40) continue

            // 评分：向前传球优先 + 距离适中 + 附近无对手
            val forwardScore = (teammate.position.z - player.position.z) * forwardDir * 0.5f
            val distScore = -kotlin.math.abs(dist - 20) * 0.2f  // 20米左右最佳
            val opponentPressure = countNearbyOpponents(teammate.position, opponents, 8.0f)
            val pressureScore = -opponentPressure * 3.0f

            // 加入随机性（AI失误）
            val errorRate = 0.1f
            val randomFactor = Random.nextFloat(-errorRate, errorRate) * 10

            val totalScore = forwardScore + distScore + pressureScore + randomFactor

            if (totalScore > bestScore) {
                bestScore = totalScore
                bestTarget = teammate
            }
        }

        return bestTarget
    }

    /**
     * 避开对手
     */
    private fun avoidOpponents(player: Player, opponents: List<Player>): Vector3 {
        var avoidDir = Vector3.ZERO
        for (opp in opponents) {
            val dist = player.position.distanceTo(opp.position)
            if (dist < 4.0f && dist > 0.1f) {
                avoidDir += (player.position - opp.position).normalized() / dist
            }
        }
        return avoidDir.normalized()
    }

    /**
     * 统计附近对手数量
     */
    private fun countNearbyOpponents(pos: Vector3, opponents: List<Player>, radius: Float): Int {
        var count = 0
        for (opp in opponents) {
            if (pos.distanceTo(opp.position) < radius) {
                count++
            }
        }
        return count
    }

    /**
     * 应用移动
     */
    private fun applyMovement(player: Player, delta: Float) {
        // 向目标移动
        val toTarget = currentTarget - player.position

        if (toTarget.length() > 0.5f) {
            player.inputDirection = toTarget.normalized()
            // 根据距离决定是否冲刺
            player.isSprinting = toTarget.length() > 10.0f &&
                    currentAction in listOf("chase", "press", "dribble")
        } else {
            player.inputDirection = Vector3.ZERO
            player.isSprinting = false
        }
    }

    /**
     * 检查区域限制
     */
    private fun checkZoneRestrictions(player: Player, eventModifiers: Map<String, Any>?) {
        // 活动修正：AI不能进入特定区域
        // 在球员AI层处理
    }
}