package com.football.game.core

import com.football.game.model.Match
import com.football.game.model.Player
import kotlin.random.Random

/**
 * 比赛阶段
 */
enum class MatchPhase {
    PLAYING,           // 正常比赛
    FOUL_STOP,         // 犯规鸣哨（球员冻结，裁判跑向事发地）
    FREE_KICK_SETUP,   // 任意球摆放
    PENALTY_SETUP      // 点球摆放
}

/**
 * 裁判状态
 */
data class RefereeState(
    var position: Vector3 = Vector3(0f, 0f, -10f),
    var facing: Vector3 = Vector3(0f, 0f, 1f),
    var speed: Float = 0f,             // 当前速度（跑动动画用）
    var whistleTimer: Float = 0f,      // >0 = 正在吹哨
    var cardTimer: Float = 0f,         // >0 = 正在出示红/黄牌
    var cardType: TackleRules.CardType = TackleRules.CardType.NONE
)

/** 待执行的定位球 */
private data class SetPieceData(
    val spot: Vector3,
    val attackingSide: GameState.TeamSide,
    val isPenalty: Boolean
)

/**
 * 游戏引擎核心类
 *
 * 实时模拟：球员 AI（压迫/抢断/铲球）、球的物理、裁判（跟球跑动/鸣哨/出牌）、
 * 任意球与点球。规则判定基于 IFAB《足球竞赛规则》Law 12 / Law 14。
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

    /** 当前比赛阶段 */
    var phase: MatchPhase = MatchPhase.PLAYING
        private set

    /** 裁判 */
    var referee = RefereeState()

    /** 进球回调（HOME = 主队进攻 +z 方向球门） */
    var onGoal: ((GameState.TeamSide) -> Unit)? = null

    /** 音效事件："whistle" / "whistle_short" / "kick" / "tackle" */
    var onSound: ((String) -> Unit)? = null

    /** 界面横幅（犯规/牌/定位球提示） */
    var onBanner: ((String) -> Unit)? = null

    /** 最后触球者（进球播报用） */
    var lastTouch: Player? = null

    private var pickupCooldown = 0f      // 出球后短暂不可再拿球
    private var aiDecisionCooldown = 0f  // AI 持球决策间隔
    private var ballControlTime = 0f     // 当前持球者控球时间（刚拿球不易被抢断）
    private var freezeTimer = 0f         // 犯规冻结时间
    private var setupTimer = 0f          // 定位球摆放时间
    private var pendingSetPiece: SetPieceData? = null
    private var gkDiveTimer = 0f         // 点球时门将扑救
    private var gkDiveTargetX = 0f

    data class Vector2D(val x: Float = 0f, val y: Float = 0f) {
        companion object { val ZERO = Vector2D() }
        fun normalized(): Vector2D {
            val len = length()
            return if (len > 0.001f) Vector2D(x / len, y / len) else ZERO
        }
        fun length(): Float {
            return kotlin.math.sqrt(x * x + y * y)
        }
    }

    // ==================== 主循环 ====================

    /**
     * 每帧调用（delta 秒）
     */
    fun update(delta: Float) {
        if (delta <= 0f) return

        // 裁判计时器
        referee.whistleTimer = (referee.whistleTimer - delta).coerceAtLeast(0f)
        referee.cardTimer = (referee.cardTimer - delta).coerceAtLeast(0f)
        updateReferee(delta)

        when (phase) {
            MatchPhase.PLAYING -> updatePlaying(delta)
            MatchPhase.FOUL_STOP -> {
                freezeTimer -= delta
                if (freezeTimer <= 0f) setupSetPiece()
            }
            MatchPhase.FREE_KICK_SETUP, MatchPhase.PENALTY_SETUP -> {
                setupTimer -= delta
                if (setupTimer <= 0f) executeRestart()
            }
        }
    }

    private fun updatePlaying(delta: Float) {
        pickupCooldown = (pickupCooldown - delta).coerceAtLeast(0f)
        aiDecisionCooldown -= delta
        if (gkDiveTimer > 0f) gkDiveTimer -= delta

        // 倒地/滑铲状态推进
        for (p in allActive()) {
            p.tackleCooldown = (p.tackleCooldown - delta).coerceAtLeast(0f)
            if (p.fallTimer > 0f) {
                p.fallTimer -= delta
                p.velocity = Vector3.ZERO
                if (p.fallTimer <= 0f) p.animState = Player.AnimState.IDLE
                continue
            }
            if (p.slideTimer > 0f) {
                p.slideTimer -= delta
                p.velocity = p.velocity * 0.93f
                p.position = p.position + p.velocity * delta
                if (p.slideTimer <= 0f) p.animState = Player.AnimState.IDLE
                continue
            }
        }

        // 罚下者自动换人
        val ap = activePlayer
        if (ap != null && ap.sentOff) {
            val replacement = homePlayers.firstOrNull { !it.sentOff && !it.isGoalkeeper }
            if (replacement != null) {
                ap.isActive = false
                ap.isPlayerControlled = false
                activePlayer = replacement
                replacement.isActive = true
                replacement.isPlayerControlled = true
            }
        }

        updateControlledPlayer(delta)
        updateAIPlayers(delta)
        updateBall(delta)
        checkGoalAndBounds()
        clampPositions()
    }

    // ==================== 球员 AI ====================

    private fun allActive(): List<Player> = (homePlayers + awayPlayers).filter { !it.sentOff }

    private fun updateControlledPlayer(delta: Float) {
        val p = activePlayer ?: return
        if (p.sentOff || p.fallTimer > 0f || p.slideTimer > 0f) return
        if (inputVector.length() > 0.15f) {
            val dir = Vector3(inputVector.x, 0f, inputVector.y).normalized()
            val stats = p.getGameStats()
            val speed = stats.speed * if (isSprinting) 1.35f else 1f
            p.velocity = dir * speed
            p.position = p.position + p.velocity * delta
            p.facingDirection = dir
        } else {
            p.velocity = Vector3.ZERO
        }
        if (ballOwner == p) lastTouch = p
    }

    private fun updateAIPlayers(delta: Float) {
        val players = allActive()
        val ownerSide = ballOwner?.teamSide

        // 压迫排序：压迫方（无球方）按距球距离排序
        val homeOutfield = players.filter { it.teamSide == GameState.TeamSide.HOME && !it.isGoalkeeper }
            .sortedBy { it.position.distanceTo(ballPosition) }
        val awayOutfield = players.filter { it.teamSide == GameState.TeamSide.AWAY && !it.isGoalkeeper }
            .sortedBy { it.position.distanceTo(ballPosition) }

        for (p in players) {
            if (p == activePlayer && p.isPlayerControlled) continue
            if (p.fallTimer > 0f || p.slideTimer > 0f) continue
            if (p == ballOwner) { dribbleAI(p, delta); continue }
            if (p.isGoalkeeper) { goalkeeperAI(p, delta); continue }

            val myOutfield = if (p.teamSide == GameState.TeamSide.HOME) homeOutfield else awayOutfield
            val rank = myOutfield.indexOfFirst { it === p }
            val isPressing = ownerSide == null || p.teamSide != ownerSide

            when {
                isPressing && rank == 0 -> {
                    chaseBall(p, delta, 1.05f)
                    tryPossession(p)
                    tryTackle(p)
                }
                isPressing && rank == 1 && p.position.distanceTo(ballPosition) < 14f -> {
                    chaseBall(p, delta, 0.9f)   // 第二压迫者
                }
                else -> formationAI(p, delta)
            }
        }
    }

    private fun chaseBall(p: Player, delta: Float, speedFactor: Float) {
        // 预判球的落点
        val intercept = Vector3(
            ballPosition.x + ballVelocity.x * 0.25f,
            0f,
            ballPosition.z + ballVelocity.z * 0.25f
        )
        movePlayer(p, intercept, delta, speedFactor)
    }

    private fun tryPossession(p: Player) {
        if (pickupCooldown > 0f || ballHeight > 1.4f) return
        val dx = p.position.x - ballPosition.x
        val dz = p.position.z - ballPosition.z
        if (dx * dx + dz * dz < 1.2f * 1.2f) {
            setBallOwner(p)
        }
    }

    private fun setBallOwner(p: Player) {
        if (ballOwner == p) return
        ballOwner?.hasBall = false
        ballOwner = p
        p.hasBall = true
        lastTouch = p
        ballControlTime = 0f
        ballHeight = 0.1f
        ballHeightVelocity = 0f
    }

    // ==================== 铲球系统 ====================

    /**
     * AI 自动铲球决策
     */
    private fun tryTackle(defender: Player) {
        val victim = ballOwner ?: return
        if (victim.teamSide == defender.teamSide) return
        if (defender.tackleCooldown > 0f) return
        // 盘带不被抢断判定：刚拿球有短暂保护（裁判不会吹这种身体接触）
        if (ballControlTime < 0.35f) return

        val dist = defender.position.distanceTo(victim.position)
        if (dist > 2.6f) return

        val attemptChance = if (dist < 1.4f) 0.8f else 0.35f
        if (Random.nextFloat() > attemptChance) return

        val speed = defender.velocity.length()
        val type = chooseTackleType(defender, victim, dist, speed)
        defender.tackleCooldown = if (type == TackleRules.TackleType.STANDING) 0.9f else 1.3f
        resolveTackle(defender, victim, type)
    }

    /**
     * 玩家手动铲球
     */
    fun doTackle(type: TackleRules.TackleType) {
        val defender = activePlayer ?: return
        val victim = ballOwner ?: return
        if (victim.teamSide == defender.teamSide) return
        if (defender.tackleCooldown > 0f) return
        if (ballControlTime < 0.2f) return
        defender.tackleCooldown = 1.0f
        resolveTackle(defender, victim, type)
    }

    /** 按距离/速度/角度选择铲球类型 */
    private fun chooseTackleType(defender: Player, victim: Player, dist: Float, speed: Float): TackleRules.TackleType {
        val behind = isFromBehind(victim, defender)
        return when {
            dist < 1.1f -> TackleRules.TackleType.STANDING
            behind && dist < 2.4f -> when (Random.nextInt(10)) {
                in 0..1 -> TackleRules.TackleType.SLIDE_ANKLE
                in 2..6 -> TackleRules.TackleType.SLIDE_LEG
                else -> TackleRules.TackleType.SLIDE_BALL
            }
            speed > 5.5f && dist < 2.6f && Random.nextFloat() < 0.35f -> TackleRules.TackleType.FLYING
            else -> TackleRules.TackleType.SLIDE_BALL
        }
    }

    /** 是否从背后接近（防守者在进攻方身后扇形内） */
    private fun isFromBehind(victim: Player, defender: Player): Boolean {
        val dx = defender.position.x - victim.position.x
        val dz = defender.position.z - victim.position.z
        val len = kotlin.math.sqrt(dx * dx + dz * dz)
        if (len < 0.001f) return false
        val dot = (victim.facingDirection.x * dx + victim.facingDirection.z * dz) / len
        return dot > 0.35f
    }

    /**
     * 铲球判定与执行：
     * 1. 被铲位置判定（先碰球 / 铲腿 / 铲脚踝）
     * 2. TackleRules 依 IFAB Law 12 判定（干净 / 任意球 / 黄牌 / 红牌）
     * 3. 犯规 → 裁判及时鸣哨 → 冻结 → 任意球或点球
     */
    private fun resolveTackle(defender: Player, victim: Player, type: TackleRules.TackleType) {
        val dist = defender.position.distanceTo(victim.position)
        val fromBehind = isFromBehind(victim, defender)
        val defenderSpeed = defender.velocity.length()

        // 被铲位置判定：碰球点 / 腿 / 脚踝
        val dBall = defender.position.distanceTo(ballPosition)
        val contact = when {
            dBall < 0.85f && ballHeight < 1.0f -> TackleRules.ContactKind.BALL
            fromBehind && dist < 0.75f -> TackleRules.ContactKind.ANKLE
            dist < 0.85f -> TackleRules.ContactKind.LEG
            else -> TackleRules.ContactKind.MISS
        }

        // 盘带护球判定：进攻方正背身护球（面向背离防守者）
        val dx = defender.position.x - victim.position.x
        val dz = defender.position.z - victim.position.z
        val len = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
        val shielding = (victim.facingDirection.x * dx + victim.facingDirection.z * dz) / len < -0.2f

        val outcome = TackleRules.judge(
            type = type,
            contact = contact,
            fromBehind = fromBehind,
            shielding = shielding,
            defending = defender.defending,
            dribbling = victim.dribbling,
            defenderSpeed = defenderSpeed,
            rng = Random
        )

        if (outcome.ballWon) {
            // 干净断球：球被捅走
            val away = Vector3(dx, 0f, dz).normalized()
            ballOwner = null
            victim.hasBall = false
            ballVelocity = away * 8.5f
            ballHeightVelocity = 0.4f
            ballControlTime = 0f
            pickupCooldown = 0.3f
            lastTouch = defender
            startTackleAnim(defender, type)
            onSound?.invoke("tackle")
            return
        }

        if (!outcome.isFoul) {
            // 合理冲撞 / 扑空：比赛继续
            if (contact != TackleRules.ContactKind.MISS) startTackleAnim(defender, type)
            return
        }

        // ==================== 犯规！ ====================
        startTackleAnim(defender, type)
        if (type != TackleRules.TackleType.STANDING) onSound?.invoke("tackle")

        // 进攻方被铲倒
        victim.fallTimer = 1.6f
        victim.animState = Player.AnimState.FALL
        victim.velocity = Vector3.ZERO
        if (ballOwner == victim) {
            victim.hasBall = false
            ballOwner = null
        }

        // 纪律处罚（两黄变一红）
        var cardText = ""
        if (outcome.card == TackleRules.CardType.YELLOW) {
            defender.yellowCards++
            if (defender.yellowCards >= 2) {
                defender.sentOff = true
                cardText = " · 两黄变一红，${defender.number}号被罚下！"
            } else {
                cardText = " · 黄牌（${defender.number}号）"
            }
        } else if (outcome.card == TackleRules.CardType.RED) {
            defender.sentOff = true
            cardText = " · 红牌！${defender.number}号被罚下！"
        }

        // 判罚地点：防守方本方禁区内 → 点球
        val isPenalty = isInsideOwnBox(victim.position, defender.teamSide)
        val kindText = if (isPenalty) "点球！" else "任意球"

        referee.whistleTimer = 1.1f   // 裁判及时鸣哨
        if (outcome.card != TackleRules.CardType.NONE) {
            referee.cardTimer = 1.8f
            referee.cardType = outcome.card
        }
        onSound?.invoke("whistle")

        ballOwner = null
        ballVelocity = Vector3.ZERO
        ballHeightVelocity = 0f
        ballControlTime = 0f

        pendingSetPiece = SetPieceData(
            spot = Vector3(victim.position.x, 0f, victim.position.z),
            attackingSide = victim.teamSide,
            isPenalty = isPenalty
        )
        phase = MatchPhase.FOUL_STOP
        freezeTimer = 1.2f

        val bannerText = "犯规！${outcome.reason} → $kindText$cardText"
        onBanner?.invoke(bannerText)
    }

    private fun startTackleAnim(defender: Player, type: TackleRules.TackleType) {
        if (type != TackleRules.TackleType.STANDING) {
            defender.slideTimer = 0.7f
            defender.animState = Player.AnimState.TACKLE
        }
    }

    /** 判断 spot 是否在 offenderSide 的本方禁区内 */
    private fun isInsideOwnBox(spot: Vector3, offenderSide: GameState.TeamSide): Boolean {
        val ownSign = if (offenderSide == GameState.TeamSide.HOME) -1f else 1f
        val z = spot.z * ownSign
        return kotlin.math.abs(spot.x) <= 20.16f && z >= GameState.FIELD_LENGTH / 2 - 16.5f && z <= GameState.FIELD_LENGTH / 2
    }

    // ==================== 定位球（任意球 / 点球） ====================

    private fun setupSetPiece() {
        val data = pendingSetPiece ?: run {
            phase = MatchPhase.PLAYING
            return
        }
        ballPosition = Vector3(data.spot.x, 0f, data.spot.z)
        ballVelocity = Vector3.ZERO
        ballHeight = 0f
        ballHeightVelocity = 0f
        ballOwner = null

        val attackingSide = data.attackingSide
        val attackSign = if (attackingSide == GameState.TeamSide.HOME) 1f else -1f
        val attackers = allActive().filter { it.teamSide == attackingSide }
        val defenders = allActive().filter { it.teamSide != attackingSide }

        if (data.isPenalty) {
            // ===== 点球（Law 14）=====
            val goalZ = -attackSign * GameState.FIELD_LENGTH / 2   // 被罚方球门
            ballPosition = Vector3(0f, 0f, goalZ + attackSign * 11f)
            phase = MatchPhase.PENALTY_SETUP
            setupTimer = 2.0f

            val taker = attackers.filter { !it.isGoalkeeper }.maxByOrNull { it.shooting }
            taker?.let {
                it.position = Vector3(ballPosition.x, 0f, ballPosition.z - attackSign * 1.4f)
                it.facingDirection = Vector3(0f, 0f, attackSign)
            }
            // 门将回门线
            defenders.filter { it.isGoalkeeper }.forEach {
                it.position = Vector3(0f, 0f, goalZ + attackSign * 0.6f)
            }
            // 其他人退出禁区
            playersExcluding(attackers, taker).forEach {
                it.position = Vector3(it.position.x.coerceIn(-25f, 25f), 0f, ballPosition.z - attackSign * 8f)
            }
            playersExcluding(defenders, defenders.firstOrNull { it.isGoalkeeper }).forEach {
                it.position = Vector3(it.position.x.coerceIn(-25f, 25f), 0f, ballPosition.z - attackSign * 9f)
            }
            onBanner?.invoke("点球！")
        } else {
            // ===== 任意球 =====
            phase = MatchPhase.FREE_KICK_SETUP
            setupTimer = 1.6f

            val taker = attackers.filter { !it.isGoalkeeper }.minByOrNull { it.position.distanceTo(ballPosition) }
            taker?.let {
                it.position = Vector3(ballPosition.x, 0f, ballPosition.z - attackSign * 1.0f)
                it.facingDirection = Vector3(0f, 0f, attackSign)
            }
            // 人墙：两名防守球员在球前 9.15m
            val wallCenter = Vector3(ballPosition.x, 0f, ballPosition.z + attackSign * 9.15f)
            defenders.filter { !it.isGoalkeeper }.sortedBy { it.position.distanceTo(wallCenter) }
                .take(2).forEachIndexed { i, d ->
                    d.position = Vector3(wallCenter.x + (i - 0.5f) * 1.8f, 0f, wallCenter.z)
                }
            onBanner?.invoke("任意球")
        }
    }

    private fun playersExcluding(list: List<Player>, excluded: Player?): List<Player> =
        if (excluded == null) list else list.filter { it !== excluded }

    private fun executeRestart() {
        val data = pendingSetPiece ?: run {
            phase = MatchPhase.PLAYING
            return
        }
        onSound?.invoke("whistle_short")

        if (data.isPenalty) {
            val attackingSide = data.attackingSide
            val taker = allActive().filter { it.teamSide == attackingSide && !it.isGoalkeeper }
                .maxByOrNull { it.shooting }
            if (taker != null) {
                setBallOwner(taker)
                shootFrom(taker)
                // 门将随机扑救方向
                gkDiveTimer = 0.8f
                gkDiveTargetX = (Random.nextFloat() * 4.4f - 2.2f)
            }
        } else {
            val attackingSide = data.attackingSide
            val taker = allActive().filter { it.teamSide == attackingSide && !it.isGoalkeeper }
                .minByOrNull { it.position.distanceTo(ballPosition) }
            if (taker != null) {
                setBallOwner(taker)
                // 玩家操控球队时把控制权交给主罚者
                if (taker.teamSide == playerSide) {
                    activePlayer?.isActive = false
                    activePlayer?.isPlayerControlled = false
                    activePlayer = taker
                    taker.isActive = true
                    taker.isPlayerControlled = true
                }
            }
        }
        phase = MatchPhase.PLAYING
    }

    // ==================== 盘带 AI（方向判定 + 护球） ====================

    private fun dribbleAI(p: Player, delta: Float) {
        lastTouch = p
        ballControlTime += delta
        val attackZ = if (p.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }
        val attackSign = if (p.teamSide == GameState.TeamSide.HOME) 1f else -1f

        // ===== 盘带方向判定：7 个候选方向，选"空当 + 逼近球门"最优 =====
        val opponents = (if (p.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers)
            .filter { !it.sentOff }
        val angles = doubleArrayOf(0.0, 25.0, -25.0, 50.0, -50.0, 75.0, -75.0)
        var bestDir = Vector3(0f, 0f, attackSign)
        var bestScore = -Float.MAX_VALUE
        for (deg in angles) {
            val dir = Vector3(sin(deg), 0f, attackSign * cos(deg))
            val probe = p.position + dir * 3.5f
            var openness = 6f
            for (o in opponents) {
                val d = o.position.distanceTo(probe)
                if (d < openness) openness = d
            }
            val progress = (probe.z - p.position.z) * attackSign
            var score = openness + progress * 0.3f
            // 撞墙惩罚
            if (kotlin.math.abs(probe.x) > 31f || kotlin.math.abs(probe.z) > 50f) score -= 6f
            if (score > bestScore) {
                bestScore = score
                bestDir = dir
            }
        }
        movePlayer(p, p.position + bestDir * 4f, delta, 0.9f)

        // ===== 持球决策：射门 / 传球（压迫时更倾向传球）=====
        if (aiDecisionCooldown > 0f) return
        aiDecisionCooldown = 0.3f

        val distToGoal = kotlin.math.abs(attackZ - p.position.z)
        val inShootRange = distToGoal < 24f && kotlin.math.abs(p.position.x) < 18f
        val pressured = nearestOpponentDist(p) < 2.4f
        when {
            inShootRange && (distToGoal < 12f || Random.nextFloat() < 0.6f) -> shootFrom(p)
            pressured && Random.nextFloat() < 0.75f -> passFrom(p)
            Random.nextFloat() < 0.15f -> passFrom(p)
            else -> {} // 继续盘带
        }
    }

    private fun nearestOpponentDist(p: Player): Float {
        val opponents = if (p.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers
        var best = Float.MAX_VALUE
        for (o in opponents) {
            if (o.sentOff) continue
            val d = p.position.distanceTo(o.position)
            if (d < best) best = d
        }
        return best
    }

    private fun formationAI(p: Player, delta: Float) {
        // 阵型位置随球整体移动 25%，防守时兼顾盯人
        var targetX = p.homePosition.x * 0.75f + ballPosition.x * 0.25f
        var targetZ = p.homePosition.z * 0.75f + ballPosition.z * 0.25f
        val ownerSide = ballOwner?.teamSide
        if (ownerSide != null && ownerSide != p.teamSide && (p.role == "CB" || p.role == "LB" || p.role == "RB")) {
            // 盯防最近对手
            val mark = (if (p.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers)
                .filter { !it.sentOff && !it.isGoalkeeper }
                .minByOrNull { it.position.distanceTo(p.position) }
            if (mark != null) {
                targetX = targetX * 0.6f + mark.position.x * 0.4f
                targetZ = targetZ * 0.6f + mark.position.z * 0.4f
            }
        }
        movePlayer(p, Vector3(targetX, 0f, targetZ), delta, 0.8f)
    }

    private fun goalkeeperAI(p: Player, delta: Float) {
        val ownGoalZ = if (p.teamSide == GameState.TeamSide.HOME) {
            -GameState.FIELD_LENGTH / 2 + 2.5f
        } else {
            GameState.FIELD_LENGTH / 2 - 2.5f
        }
        var targetX = (ballPosition.x * 0.45f).coerceIn(-5.5f, 5.5f)
        var speedFactor = 1.1f

        // 点球扑救：飞向预定方向
        if (gkDiveTimer > 0f) {
            targetX = gkDiveTargetX
            speedFactor = 2.2f
        }
        movePlayer(p, Vector3(targetX, 0f, ownGoalZ), delta, speedFactor)

        // 抱住进入小禁区的自由球
        if (ballOwner == null && pickupCooldown <= 0f && ballHeight < 2.2f &&
            p.position.distanceTo(ballPosition) < 2.0f
        ) {
            setBallOwner(p)
        }

        // 持球后大脚开往前场
        if (ballOwner == p && aiDecisionCooldown <= 0f) {
            aiDecisionCooldown = 0.5f
            val dirSign = if (p.teamSide == GameState.TeamSide.HOME) 1 else -1
            val target = Vector3(
                Random.nextFloat() * 20f - 10f, 0f,
                dirSign * (GameState.FIELD_LENGTH / 2 - 20f)
            )
            val dir = (target - p.position).flatten().normalized()
            ballVelocity = dir * (GameState.PASS_SPEED * 1.4f)
            ballHeightVelocity = 3f
            ballOwner = null
            p.hasBall = false
            lastTouch = p
            pickupCooldown = 0.5f
            onSound?.invoke("kick")
        }
    }

    private fun movePlayer(p: Player, target: Vector3, delta: Float, speedFactor: Float) {
        val toTarget = Vector3(target.x - p.position.x, 0f, target.z - p.position.z)
        val dist = toTarget.length()
        if (dist < 0.4f) {
            p.velocity = Vector3.ZERO
            return
        }
        val dir = toTarget.normalized()
        val speed = p.getGameStats().speed * speedFactor
        p.velocity = dir * speed.coerceAtMost(dist / delta)
        p.position = p.position + p.velocity * delta
        p.facingDirection = dir
    }

    // ==================== 裁判 ====================

    /**
     * 裁判 AI：跟随攻防跑动（保持在球侧后方约 10m），犯规时跑向事发地鸣哨
     */
    private fun updateReferee(delta: Float) {
        var target = when (phase) {
            MatchPhase.PLAYING -> Vector3(
                ballPosition.x - 6f,
                0f,
                ballPosition.z - 6f
            )
            else -> {
                val spot = pendingSetPiece?.spot ?: ballPosition
                Vector3(spot.x + 5f, 0f, spot.z - 5f)
            }
        }
        // 与球保持至少 5m
        val toBall = target - ballPosition
        if (toBall.length() < 5f) {
            val dir = toBall.normalized()
            target = Vector3(
                ballPosition.x + dir.x * 5f,
                0f,
                ballPosition.z + dir.z * 5f
            )
        }

        val dx = target.x - referee.position.x
        val dz = target.z - referee.position.z
        val dist = kotlin.math.sqrt(dx * dx + dz * dz)
        if (dist > 0.5f) {
            val speed = 6.8f
            val step = speed * delta
            val move = kotlin.math.min(step, dist)
            referee.position = Vector3(
                referee.position.x + dx / dist * move,
                0f,
                referee.position.z + dz / dist * move
            )
            referee.facing = Vector3(dx / dist, 0f, dz / dist)
            referee.speed = speed
        } else {
            referee.speed = 0f
        }
        // 限制在场地内
        referee.position = Vector3(
            referee.position.x.coerceIn(-GameState.FIELD_WIDTH / 2 - 2f, GameState.FIELD_WIDTH / 2 + 2f),
            0f,
            referee.position.z.coerceIn(-GameState.FIELD_LENGTH / 2 - 2f, GameState.FIELD_LENGTH / 2 + 2f)
        )
    }

    // ==================== 球的物理 ====================

    private fun updateBall(delta: Float) {
        val owner = ballOwner ?: run {
            updateFreeBall(delta)
            return
        }
        // 球粘在控球者前方
        val dir = owner.facingDirection
        val targetX = owner.position.x + dir.x * 0.55f
        val targetZ = owner.position.z + dir.z * 0.55f
        val follow = (delta * 12f).coerceAtMost(1f)
        ballPosition = Vector3(
            ballPosition.x + (targetX - ballPosition.x) * follow,
            0f,
            ballPosition.z + (targetZ - ballPosition.z) * follow
        )
        ballHeight = 0.11f
        ballHeightVelocity = 0f
        ballVelocity = Vector3.ZERO
        ballControlTime += delta
    }

    private fun updateFreeBall(delta: Float) {
        // 高度物理（重力 + 弹跳）
        ballHeightVelocity -= GameState.BALL_GRAVITY * delta
        ballHeight += ballHeightVelocity * delta
        if (ballHeight < 0f) {
            ballHeight = 0f
            ballHeightVelocity = if (kotlin.math.abs(ballHeightVelocity) > 1.5f) {
                -ballHeightVelocity * 0.55f
            } else {
                0f
            }
        }
        // 地面摩擦 / 空气阻力
        val drag = if (ballHeight < 0.15f) GameState.BALL_FRICTION else GameState.BALL_AIR_DRAG
        ballVelocity = ballVelocity * (1f - drag * delta)
        ballPosition = ballPosition + ballVelocity * delta

        // 边线反弹
        val halfW = GameState.FIELD_WIDTH / 2
        if (ballPosition.x < -halfW) {
            ballPosition = Vector3(-halfW, 0f, ballPosition.z)
            ballVelocity = Vector3(-ballVelocity.x * 0.6f, 0f, ballVelocity.z)
        } else if (ballPosition.x > halfW) {
            ballPosition = Vector3(halfW, 0f, ballPosition.z)
            ballVelocity = Vector3(-ballVelocity.x * 0.6f, 0f, ballVelocity.z)
        }
        // 底线反弹（球门范围内除外，由 checkGoalAndBounds 处理）
        val halfL = GameState.FIELD_LENGTH / 2
        val outsideGoalMouth = kotlin.math.abs(ballPosition.x) > GameState.GOAL_WIDTH / 2 || ballHeight > 2.44f
        if (outsideGoalMouth) {
            if (ballPosition.z > halfL) {
                ballPosition = Vector3(ballPosition.x, 0f, halfL)
                ballVelocity = Vector3(ballVelocity.x, 0f, -ballVelocity.z * 0.6f)
            } else if (ballPosition.z < -halfL) {
                ballPosition = Vector3(ballPosition.x, 0f, -halfL)
                ballVelocity = Vector3(ballVelocity.x, 0f, -ballVelocity.z * 0.6f)
            }
        }
    }

    private fun checkGoalAndBounds() {
        val halfL = GameState.FIELD_LENGTH / 2
        val inGoalMouth = kotlin.math.abs(ballPosition.x) < GameState.GOAL_WIDTH / 2 && ballHeight < 2.44f

        if (ballPosition.z > halfL + 0.2f) {
            // 主队进攻 +z 球门
            if (inGoalMouth) {
                onGoal?.invoke(GameState.TeamSide.HOME)
                kickoffReset()
            } else if (ballPosition.z > halfL + 3f) {
                onSound?.invoke("whistle_short")
                kickoffReset()
            }
        } else if (ballPosition.z < -halfL - 0.2f) {
            if (inGoalMouth) {
                onGoal?.invoke(GameState.TeamSide.AWAY)
                kickoffReset()
            } else if (ballPosition.z < -halfL - 3f) {
                onSound?.invoke("whistle_short")
                kickoffReset()
            }
        }
    }

    /** 开球重置 */
    fun kickoffReset() {
        phase = MatchPhase.PLAYING
        pendingSetPiece = null
        ballPosition = Vector3.ZERO
        ballVelocity = Vector3.ZERO
        ballHeight = 0f
        ballHeightVelocity = 0f
        ballOwner = null
        lastTouch = null
        ballControlTime = 0f
        pickupCooldown = 0.3f
        gkDiveTimer = 0f
        for (p in homePlayers + awayPlayers) {
            p.position = p.homePosition
            p.velocity = Vector3.ZERO
            p.hasBall = false
            p.fallTimer = 0f
            p.slideTimer = 0f
            p.animState = Player.AnimState.IDLE
            p.facingDirection = if (p.teamSide == GameState.TeamSide.HOME) {
                Vector3(0f, 0f, 1f)
            } else {
                Vector3(0f, 0f, -1f)
            }
        }
    }

    private fun clampPositions() {
        val halfW = GameState.FIELD_WIDTH / 2 + 1f
        val halfL = GameState.FIELD_LENGTH / 2 + 1f
        for (p in allActive()) {
            if (kotlin.math.abs(p.position.x) > halfW || kotlin.math.abs(p.position.z) > halfL) {
                p.position = Vector3(
                    p.position.x.coerceIn(-halfW, halfW),
                    0f,
                    p.position.z.coerceIn(-halfL, halfL)
                )
            }
        }
    }

    // ==================== 动作接口 ====================

    fun doPass() {
        val player = activePlayer ?: return
        passFrom(player)
    }

    fun doShoot() {
        val player = activePlayer ?: return
        shootFrom(player)
    }

    fun doThroughBall() {
        val player = activePlayer ?: return
        if (ballOwner != player) return

        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1

        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE

        for (teammate in team) {
            if (teammate === player || teammate.sentOff) continue
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
            lastTouch = player
            pickupCooldown = 0.4f
            ballControlTime = 0f
            onSound?.invoke("kick")
        }
    }

    /** 通用传球（指定出球者） */
    fun passFrom(player: Player) {
        if (ballOwner != player) return

        val team = if (player.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val forwardDir = if (player.teamSide == GameState.TeamSide.HOME) 1 else -1

        var bestTarget: Player? = null
        var bestScore = -Float.MAX_VALUE

        for (teammate in team) {
            if (teammate === player || teammate.sentOff) continue
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
            lastTouch = player
            pickupCooldown = 0.4f
            ballControlTime = 0f
            onSound?.invoke("kick")
        }
    }

    /** 通用射门（指定射门者） */
    fun shootFrom(player: Player) {
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
        lastTouch = player
        pickupCooldown = 0.4f
        ballControlTime = 0f
        player.animState = Player.AnimState.KICK
        player.actionCooldown = 0.4f
        onSound?.invoke("kick")
    }

    fun switchPlayer() {
        val team = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        if (team.isEmpty()) return

        val currentIdx = team.indexOf(activePlayer)
        for (i in 1..team.size) {
            val idx = (currentIdx + i) % team.size
            if (!team[idx].isGoalkeeper && !team[idx].sentOff) {
                activePlayer?.isActive = false
                activePlayer?.isPlayerControlled = false
                activePlayer = team[idx]
                team[idx].isActive = true
                team[idx].isPlayerControlled = true
                return
            }
        }
    }

    companion object {
        private val ROLES = listOf("GK", "LB", "CB", "CB", "RB", "CM", "CM", "CM", "LW", "ST", "RW")
        private val NUMBERS = listOf(1, 3, 4, 5, 2, 6, 8, 10, 11, 9, 7)

        // 4-3-3 基础站位 (x, z)，主队进攻 +z，场地 105 x 68
        private val BASE_POSITIONS = listOf(
            Pair(0f, -44f),                                                   // GK
            Pair(-22f, -30f), Pair(-8f, -33f), Pair(8f, -33f), Pair(22f, -30f), // DF
            Pair(-18f, -12f), Pair(0f, -16f), Pair(18f, -12f),                 // MF
            Pair(-24f, 4f), Pair(0f, 2f), Pair(24f, 4f)                        // FW
        )

        /**
         * 为一支球队生成 11 名球员（4-3-3）
         * 下标 0 = 门将，下标 9 = 中锋（招牌球星位）
         */
        fun createTeamPlayers(team: com.football.game.model.Team?, side: GameState.TeamSide): List<Player> {
            val sign = if (side == GameState.TeamSide.HOME) 1f else -1f
            return BASE_POSITIONS.mapIndexed { i, (x, z) ->
                Player(
                    id = "${team?.id ?: side.name}_$i",
                    name = "${NUMBERS[i]}号",
                    number = NUMBERS[i],
                    role = ROLES[i],
                    teamSide = side,
                    teamId = team?.id ?: "",
                    teamName = team?.name ?: "",
                    isGoalkeeper = i == 0,
                    position = Vector3(x * sign, 0f, z * sign),
                    homePosition = Vector3(x * sign, 0f, z * sign)
                )
            }
        }
    }
}

private fun sin(rad: Double): Float = kotlin.math.sin(rad).toFloat()
private fun cos(rad: Double): Float = kotlin.math.cos(rad).toFloat()
