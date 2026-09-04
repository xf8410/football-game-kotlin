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
    FREE_KICK_SETUP,   // 任意球/界外球/球门球/角球摆放
    PENALTY_SETUP      // 点球摆放
}

/** 定位球种类（犯规任意球/点球 + 出界三件套） */
private enum class SetPieceKind {
    FREE_KICK, PENALTY, THROW_IN, GOAL_KICK, CORNER
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
    val kind: SetPieceKind,
    val spot: Vector3,
    val attackingSide: GameState.TeamSide
)

/**
 * 游戏引擎核心类
 *
 * 实时模拟：球员 AI（压迫/抢断/铲球 + 无球跑位系统）、球的物理、
 * 裁判（对角线跟跑/鸣哨/出牌）、任意球/点球/界外球/球门球/角球、有利进攻规则。
 * 规则判定基于 IFAB《足球竞赛规则》Law 12 / Law 14 / Law 15-17。
 *
 * 对标《足球在线4》"核心比赛体验再次进化"：
 * 跑位分离力 v2（防扎堆）、传球通道拦截评分（少刀山球）、受压传球失误、
 * 挑传高弧线、接球转向、门将出击时机收紧、前锋躲避门将、跳起躲人避障。
 */
class GameEngine(
    val match: Match,
    var homePlayers: List<Player>,
    var awayPlayers: List<Player>
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

    /** 呼叫压位：按住时第二名队友无视距离参与逼抢（"呼叫压位"键，实况式呼叫协防） */
    var callPressing = false

    /**
     * 大按钮三态：随局势自动切换
     * SPRINT=按住加速（带球=爆趟） / SHOOT=蓄力射门 / TACKLE=点按铲球
     */
    enum class ActionMode { SPRINT, SHOOT, TACKLE }

    /** 无球跑位模式 */
    private enum class OffBallMode { ATTACK, DEFEND, LOOSE }

    /** 当前比赛阶段 */
    var phase: MatchPhase = MatchPhase.PLAYING
        private set

    /** 裁判 */
    var referee = RefereeState()

    /** 进球回调（HOME = 主队进攻 +z 方向球门） */
    var onGoal: ((GameState.TeamSide) -> Unit)? = null

    /** 音效事件："whistle" / "whistle_short" / "kick" / "tackle" */
    var onSound: ((String) -> Unit)? = null

    /** 界面横幅（犯规/牌/定位球/出界提示） */
    var onBanner: ((String) -> Unit)? = null

    /** 最后触球者（进球播报/出界球权判定用） */
    var lastTouch: Player? = null

    /** 替补席（每队 6 人：1 门将 + 5 外场，可主动换上场） */
    val homeBench: List<Player> = createBenchPlayers(match.homeTeam, GameState.TeamSide.HOME)
    val awayBench: List<Player> = createBenchPlayers(match.awayTeam, GameState.TeamSide.AWAY)

    /** 已用换人名额 */
    var homeSubsUsed = 0
        private set
    var awaySubsUsed = 0
        private set

    /** 比赛统计（暂停面板"统计数据"用：射门/传球/控球率） */
    data class MatchStats(
        var homeShots: Int = 0,
        var awayShots: Int = 0,
        var homePasses: Int = 0,
        var awayPasses: Int = 0,
        var possessionHome: Float = 0f,
        var possessionAway: Float = 0f
    )

    var stats = MatchStats()
        private set

    private var pickupCooldown = 0f      // 出球后短暂不可再拿球
    private var aiDecisionCooldown = 0f  // AI 持球决策间隔
    private var ballControlTime = 0f     // 当前持球者控球时间（刚拿球不易被抢断）
    private var freezeTimer = 0f         // 犯规冻结时间
    private var setupTimer = 0f          // 定位球摆放时间
    private var pendingSetPiece: SetPieceData? = null
    private var gkDiveTimer = 0f         // 点球时门将扑救
    private var gkDiveTargetX = 0f
    private var sprintKnockTimer = 0f    // 爆趟计时（带球按住加速时周期性把球趟出去）

    // ===== 大按钮三态支持状态 =====
    private var lastPasser: Player? = null   // 最近一次传球出球者（判定"接到传球"）
    private var passTravelTimer = 10f        // 距上次出球经过秒数
    private var receiveWindow = 0f           // >0 = 刚接住队友传球（按钮短暂切为射门）
    private var autoSwitchCooldown = 0f      // 无球自动换人防抖
    private var lastActionMode = ActionMode.SPRINT  // 大按钮迟滞状态（防贴边抖动）
    private var lastAttackSign = 1f          // 最近持球方进攻方向（裁判对角线跑位用）
    private var downedPlayers: List<Player> = emptyList()  // 倒地/滑铲中球员（避障用）

    // ===== 有利进攻（IFAB advantage）=====
    private var advantageTimer = 0f
    private var advantageSpot: Vector3? = null
    private var advantageSide: GameState.TeamSide? = null

    /** 射门偏出标记（底线出界时横幅"射门偏出"用） */
    private var lastShotMissed = false

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
        passTravelTimer += delta
        receiveWindow = (receiveWindow - delta).coerceAtLeast(0f)
        autoSwitchCooldown = (autoSwitchCooldown - delta).coerceAtLeast(0f)

        // 控球统计（暂停面板控球率用）+ 进攻方向记忆（裁判跑位用）
        ballOwner?.let { o ->
            lastAttackSign = if (o.teamSide == GameState.TeamSide.HOME) 1f else -1f
            if (o.teamSide == GameState.TeamSide.HOME) {
                stats.possessionHome += delta
            } else {
                stats.possessionAway += delta
            }
        }

        // ===== 有利进攻计时：被犯规方丢球 → 回溯到犯规地点补吹任意球 =====
        if (advantageTimer > 0f) {
            advantageTimer -= delta
            val advOwner = ballOwner
            val holderSide = advOwner?.teamSide ?: lastTouch?.teamSide
            if (holderSide != advantageSide) {
                // 丢球/出界：回溯
                val spot = advantageSpot ?: Vector3(0f, 0f, 0f)
                val side = advantageSide ?: GameState.TeamSide.HOME
                advantageTimer = 0f
                advantageSpot = null
                advantageSide = null
                advOwner?.hasBall = false
                ballOwner = null
                ballVelocity = Vector3.ZERO
                ballHeightVelocity = 0f
                referee.whistleTimer = 1.0f
                onSound?.invoke("whistle")
                pendingSetPiece = SetPieceData(
                    kind = SetPieceKind.FREE_KICK,
                    spot = spot,
                    attackingSide = side
                )
                phase = MatchPhase.FOUL_STOP
                freezeTimer = 1.0f
                onBanner?.invoke("有利结束，回溯任意球")
            } else if (advantageTimer <= 0f) {
                // 有利结束（进攻方保住球权），不再追溯
                advantageTimer = 0f
                advantageSpot = null
                advantageSide = null
            }
        }

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

        // 倒地/滑铲球员列表（跳起躲人避障用）
        downedPlayers = allActive().filter { it.fallTimer > 0f || it.slideTimer > 0f }

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

        // 无球时自动切到离球最近的己方球员（省掉"切换"按钮；手动切换另有 switchToNearest）
        // 迟滞 2.2m + 防抖 0.9s：减少频繁切换导致的"一顿一顿"
        val currentOwner = ballOwner
        val ctrl = activePlayer
        if (ctrl != null && autoSwitchCooldown <= 0f &&
            ctrl.fallTimer <= 0f && ctrl.slideTimer <= 0f &&
            (currentOwner == null || currentOwner.teamSide != playerSide)
        ) {
            val squad = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
            val nearest = squad
                .filter { !it.sentOff && !it.isGoalkeeper && it.fallTimer <= 0f && it.slideTimer <= 0f }
                .minByOrNull { it.position.distanceTo(ballPosition) }
            if (nearest != null && nearest !== ctrl &&
                nearest.position.distanceTo(ballPosition) + 2.2f < ctrl.position.distanceTo(ballPosition)
            ) {
                switchControlTo(nearest)
                autoSwitchCooldown = 0.9f
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
        val owner = ballOwner
        val ownerSide = owner?.teamSide

        // 压迫排序：压迫方（无球方）按距球距离排序
        val homeOutfield = players.filter { it.teamSide == GameState.TeamSide.HOME && !it.isGoalkeeper }
            .sortedBy { it.position.distanceTo(ballPosition) }
        val awayOutfield = players.filter { it.teamSide == GameState.TeamSide.AWAY && !it.isGoalkeeper }
            .sortedBy { it.position.distanceTo(ballPosition) }

        for (p in players) {
            if (p == activePlayer && p.isPlayerControlled) continue
            if (p.fallTimer > 0f || p.slideTimer > 0f) continue
            if (p == owner) { dribbleAI(p, delta); continue }
            if (p.isGoalkeeper) { goalkeeperAI(p, delta); continue }

            // 前锋躲避门将：对方门将持球时保持 4.5m 距离，不逼抢不抢断
            if (owner != null && owner.isGoalkeeper && owner.teamSide != p.teamSide) {
                val d = p.position.distanceTo(owner.position)
                if (d < 4.5f) {
                    val ax = p.position.x - owner.position.x
                    val az = p.position.z - owner.position.z
                    val len = kotlin.math.sqrt(ax * ax + az * az)
                    if (len > 0.01f) {
                        movePlayer(
                            p,
                            Vector3(owner.position.x + ax / len * 5f, 0f, owner.position.z + az / len * 5f),
                            delta, 0.9f
                        )
                    }
                }
                continue
            }

            val myOutfield = if (p.teamSide == GameState.TeamSide.HOME) homeOutfield else awayOutfield
            val rank = myOutfield.indexOfFirst { it === p }
            val isPressing = ownerSide == null || p.teamSide != ownerSide

            when {
                isPressing && rank == 0 -> {
                    chaseBall(p, delta, 1.05f)
                    tryPossession(p)
                    tryTackle(p)
                }
                isPressing && rank == 1 && (p.position.distanceTo(ballPosition) < 14f || callPressing) -> {
                    chaseBall(p, delta, 0.9f)   // 第二压迫者（呼叫压位时无视距离）
                }
                else -> {
                    // 其余球员：无球跑位系统（不再全员涌向球 → 不是碰碰车）
                    val mode = when {
                        ownerSide == null -> OffBallMode.LOOSE
                        ownerSide == p.teamSide -> OffBallMode.ATTACK
                        else -> OffBallMode.DEFEND
                    }
                    offBallAI(p, delta, mode)
                }
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
        // 门将持球受保护（前锋躲避门将）：不逼抢不抢断门将
        val cur = ballOwner
        if (cur != null && cur.isGoalkeeper && cur.teamSide != p.teamSide) return
        val dx = p.position.x - ballPosition.x
        val dz = p.position.z - ballPosition.z
        if (dx * dx + dz * dz < 1.2f * 1.2f) {
            assignBallOwner(p)
        }
    }

    /** 持球权转移（同步 hasBall / 控球保护时间 / 接传球射门窗口 / 控制权自动切换） */
    private fun assignBallOwner(p: Player) {
        if (ballOwner == p) return
        // 接到队友传球：短暂给出"射门"按钮（接球即射机会）
        val passer = lastPasser
        if (passer != null && p !== passer && passTravelTimer < 4f &&
            p.teamSide == passer.teamSide && passer.teamSide == playerSide && !p.isGoalkeeper
        ) {
            receiveWindow = 1.2f
        }
        lastPasser = null
        lastShotMissed = false
        // 我方拿球 → 控制权自动交给拿球者（门将除外）
        if (p.teamSide == playerSide && !p.isGoalkeeper && p !== activePlayer) {
            switchControlTo(p)
        }
        ballOwner?.hasBall = false
        ballOwner = p
        p.hasBall = true
        lastTouch = p
        // 接球朝向（停球转身更合理）：接球瞬间先面向进攻方向
        if (!p.isGoalkeeper) {
            val atk = if (p.teamSide == GameState.TeamSide.HOME) 1f else -1f
            p.facingDirection = Vector3(0f, 0f, atk)
        }
        ballControlTime = 0f
        ballHeight = 0.1f
        ballHeightVelocity = 0f
    }

    /** 控制权切到指定球员（自动换人共用） */
    private fun switchControlTo(p: Player) {
        activePlayer?.isActive = false
        activePlayer?.isPlayerControlled = false
        activePlayer = p
        p.isActive = true
        p.isPlayerControlled = true
    }

    // ==================== 无球跑位系统（不是碰碰车） ====================

    /**
     * 无球跑位：按"进攻/防守/松散球"三种局势分角色跑位，并施加队友最小间距分离力。
     *
     * 进攻（我方持球）：
     * - 边锋拉边贴边线，随球前压保持宽度（拉开进攻宽度）
     * - 中锋斜插身后空当，与持球者保持 13m 纵深（直塞目标）
     * - 中场：距持球者最近的 CM 回撤接应（7m 短传点），另一人拖后保护（攻守平衡）
     * - 边后卫沿边路套上（落后球 9m 的套边选项）
     * - 中后卫拖后，不越过球太多（保出球/回防支点）
     *
     * 防守（对方持球）：
     * - 中后卫收在球与球门之间，兼顾盯防最近对手
     * - 边后卫退守边路走廊
     * - 中场在球后 5m 组成拦截线
     * - 前场球员只回撤到中线附近松散盯人（保留反击身位，不深度回防）
     *
     * 松散球（无人持球）：全队向球适度收拢（阵型 50% + 球 50%）
     *
     * 通用（避障算法 v2）：与 7m 内所有队友按距离加权推开 —— 反扎堆/反碰碰车
     */
    private fun offBallAI(p: Player, delta: Float, mode: OffBallMode) {
        val attackSign = if (p.teamSide == GameState.TeamSide.HOME) 1f else -1f
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2

        // 己方外场队友（含持球者，用于分离；排除自己）
        val mates = (if (p.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers)
            .filter { !it.sentOff && !it.isGoalkeeper && it !== p }

        var tx: Float
        var tz: Float
        var speedFactor = 0.85f

        when (mode) {
            OffBallMode.ATTACK -> {
                when (p.role) {
                    "LW", "RW" -> {
                        // 拉边保持宽度，随球前压
                        tx = if (p.homePosition.x < 0) -(halfW - 3f) else (halfW - 3f)
                        tz = ballPosition.z + attackSign * 10f
                        speedFactor = 0.95f
                    }
                    "ST" -> {
                        // 斜插身后：保持球前 13m 纵深（直塞目标）
                        tz = if (attackSign > 0) {
                            (ballPosition.z + 13f).coerceAtMost(halfL - 6f)
                        } else {
                            (ballPosition.z - 13f).coerceAtLeast(-halfL + 6f)
                        }
                        tx = ballPosition.x * 0.5f
                        speedFactor = 0.95f
                    }
                    "CM" -> {
                        val cms = mates.filter { it.role == "CM" }
                            .sortedBy { it.position.distanceTo(ballPosition) }
                        if (cms.firstOrNull() === p) {
                            // 接应点：持球者身后 7m、横向偏 4m（短传安全点）
                            tx = ballPosition.x + (if (ballPosition.x > 0) -4f else 4f)
                            tz = ballPosition.z - attackSign * 7f
                            speedFactor = 0.95f
                        } else {
                            // 拖后保护（攻守平衡 / 二点保护）
                            tx = ballPosition.x * 0.55f
                            tz = ballPosition.z - attackSign * 10f
                        }
                    }
                    "LB", "RB" -> {
                        // 套边：沿边路跟进，落后球 9m
                        tx = if (p.homePosition.x < 0) -(halfW - 6f) else (halfW - 6f)
                        tz = ballPosition.z - attackSign * 9f
                    }
                    else -> {
                        // 中后卫拖后
                        tx = p.homePosition.x * 0.7f + ballPosition.x * 0.3f
                        tz = ballPosition.z - attackSign * 13f
                    }
                }
            }
            OffBallMode.DEFEND -> {
                when (p.role) {
                    "CB" -> {
                        // 收在球与球门之间，兼顾盯防最近对手
                        tz = ballPosition.z - attackSign * 10f
                        val mark = nearestOpponentOf(p)
                        tx = mark?.position?.x?.let { mx -> mx * 0.7f + p.homePosition.x * 0.3f }
                            ?: (p.homePosition.x * 0.7f + ballPosition.x * 0.3f)
                    }
                    "LB", "RB" -> {
                        tz = ballPosition.z - attackSign * 8f
                        tx = if (p.homePosition.x < 0) -(halfW - 7f) else (halfW - 7f)
                    }
                    "CM" -> {
                        // 中场拦截线
                        tz = ballPosition.z - attackSign * 5f
                        tx = ballPosition.x * 0.5f
                    }
                    else -> {
                        // 前场球员：回撤松散盯人，最多退到中线附近
                        tz = ballPosition.z - attackSign * 15f
                        tz = if (attackSign > 0) tz.coerceAtLeast(-2f) else tz.coerceAtMost(2f)
                        tx = nearestOpponentOf(p)?.position?.x ?: p.homePosition.x
                    }
                }
                // 防守纵深保护：不退到本方球门线
                tz = if (attackSign > 0) tz.coerceAtLeast(-halfL + 8f) else tz.coerceAtMost(halfL - 8f)
            }
            OffBallMode.LOOSE -> {
                tx = p.homePosition.x * 0.5f + ballPosition.x * 0.5f
                tz = p.homePosition.z * 0.5f + ballPosition.z * 0.5f
            }
        }

        // ===== 反扎堆（避障算法 v2）：7m 内所有队友按距离加权推开 =====
        for (mate in mates) {
            val dx = tx - mate.position.x
            val dz = tz - mate.position.z
            val d = kotlin.math.sqrt(dx * dx + dz * dz)
            if (d < 7f && d > 0.01f) {
                val w = (7f - d) / 7f          // 0~1 权重：越近推得越多
                val push = w * 4.2f
                tx += dx / d * push
                tz += dz / d * push
            }
        }

        movePlayer(
            p,
            Vector3(
                tx.coerceIn(-halfW + 2f, halfW - 2f),
                0f,
                tz.coerceIn(-halfL + 2f, halfL - 2f)
            ),
            delta,
            speedFactor
        )
    }

    /** 最近的对方外场球员（盯人用） */
    private fun nearestOpponentOf(p: Player): Player? {
        val opponents = if (p.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers
        return opponents.filter { !it.sentOff && !it.isGoalkeeper }
            .minByOrNull { it.position.distanceTo(p.position) }
    }

    // ==================== 铲球系统 ====================

    /**
     * AI 自动铲球决策
     */
    private fun tryTackle(defender: Player) {
        val victim = ballOwner ?: return
        if (victim.teamSide == defender.teamSide) return
        if (victim.isGoalkeeper) return   // 门将持球受保护（前锋躲避门将）
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
     * 玩家手动铲球（大按钮"铲球"态）
     * 贴身自动选站立抢断，稍远自动滑铲/飞铲；判罚交给 TackleRules（IFAB Law 12）
     */
    fun doTackle() {
        val defender = activePlayer ?: return
        val victim = ballOwner ?: return
        if (victim.teamSide == defender.teamSide) return
        if (victim.isGoalkeeper) return   // 门将持球受保护
        if (defender.tackleCooldown > 0f) return
        if (ballControlTime < 0.2f) return
        val dist = defender.position.distanceTo(victim.position)
        val type = chooseTackleType(defender, victim, dist, defender.velocity.length())
        defender.tackleCooldown = if (type == TackleRules.TackleType.STANDING) 0.9f else 1.3f
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
     * 3. 犯规 → 有利进攻（无牌+进攻半场）或 裁判及时鸣哨 → 冻结 → 任意球或点球
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

        // 判罚地点：防守方本方禁区内 → 点球
        val isPenalty = isInsideOwnBox(victim.position, defender.teamSide)
        val victimAttackSign = if (victim.teamSide == GameState.TeamSide.HOME) 1f else -1f

        // 有利进攻（IFAB advantage）：无牌犯规且在被犯规方进攻半场 → 比赛继续；
        // 若 2.5 秒内丢掉球权，回溯到犯规地点补吹任意球
        if (outcome.card == TackleRules.CardType.NONE && !isPenalty &&
            victimAttackSign * victim.position.z > 0f
        ) {
            advantageSpot = Vector3(victim.position.x, 0f, victim.position.z)
            advantageSide = victim.teamSide
            advantageTimer = 2.5f
            ballControlTime = 0f
            sprintKnockTimer = 0f
            onBanner?.invoke("有利进攻，比赛继续")
            return
        }

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
        advantageTimer = 0f
        advantageSpot = null
        advantageSide = null

        pendingSetPiece = SetPieceData(
            kind = if (isPenalty) SetPieceKind.PENALTY else SetPieceKind.FREE_KICK,
            spot = Vector3(victim.position.x, 0f, victim.position.z),
            attackingSide = victim.teamSide
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

    // ==================== 定位球（任意球 / 点球 / 界外球 / 球门球 / 角球） ====================

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
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2

        when (data.kind) {
            SetPieceKind.PENALTY -> {
                // ===== 点球（Law 14）：被罚方（防守方）球门点球点 =====
                val goalZ = attackSign * halfL
                phase = MatchPhase.PENALTY_SETUP
                setupTimer = 2.0f

                ballPosition = Vector3(0f, 0f, goalZ - attackSign * 11f)
                val taker = attackers.filter { !it.isGoalkeeper }.maxByOrNull { it.shooting }
                taker?.let {
                    it.position = Vector3(ballPosition.x, 0f, ballPosition.z - attackSign * 1.4f)
                    it.facingDirection = Vector3(0f, 0f, attackSign)
                }
                // 门将回门线
                defenders.filter { it.isGoalkeeper }.forEach {
                    it.position = Vector3(0f, 0f, goalZ - attackSign * 0.6f)
                }
                // 其他人退出禁区
                playersExcluding(attackers, taker).forEach {
                    it.position = Vector3(it.position.x.coerceIn(-25f, 25f), 0f, ballPosition.z - attackSign * 8f)
                }
                playersExcluding(defenders, defenders.firstOrNull { it.isGoalkeeper }).forEach {
                    it.position = Vector3(it.position.x.coerceIn(-25f, 25f), 0f, ballPosition.z - attackSign * 9f)
                }
                onBanner?.invoke("点球！")
            }
            SetPieceKind.FREE_KICK -> {
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
            SetPieceKind.THROW_IN -> {
                // ===== 界外球：最近的本方球员到线边发球 =====
                phase = MatchPhase.FREE_KICK_SETUP
                setupTimer = 0.7f
                val taker = attackers.filter { !it.isGoalkeeper }.minByOrNull { it.position.distanceTo(ballPosition) }
                taker?.let {
                    it.position = Vector3(
                        ballPosition.x + if (ballPosition.x > 0) 0.6f else -0.6f, 0f, ballPosition.z
                    )
                    it.facingDirection = Vector3(if (ballPosition.x > 0) -1f else 1f, 0f, 0f)
                }
            }
            SetPieceKind.GOAL_KICK -> {
                // ===== 球门球：门将开大脚，队友前压接应 =====
                phase = MatchPhase.FREE_KICK_SETUP
                setupTimer = 1.0f
                val gk = attackers.firstOrNull { it.isGoalkeeper }
                gk?.let {
                    it.position = Vector3(
                        ballPosition.x - if (ballPosition.x > 0) 1.2f else -1.2f, 0f,
                        ballPosition.z - attackSign * 0.8f
                    )
                    it.facingDirection = Vector3(0f, 0f, attackSign)
                }
                playersExcluding(attackers, gk).forEach {
                    it.position = Vector3(
                        it.position.x.coerceIn(-halfW + 4f, halfW - 4f), 0f,
                        (ballPosition.z + attackSign * 14f).coerceIn(-halfL + 6f, halfL - 6f)
                    )
                }
            }
            SetPieceKind.CORNER -> {
                // ===== 角球：主罚者到角旗区，两名防守球员守门前 9m 一带 =====
                phase = MatchPhase.FREE_KICK_SETUP
                setupTimer = 1.2f
                val goalZ = attackSign * halfL
                val taker = attackers.filter { !it.isGoalkeeper }.minByOrNull { it.position.distanceTo(ballPosition) }
                taker?.let {
                    it.position = Vector3(
                        ballPosition.x - if (ballPosition.x > 0) 0.8f else -0.8f, 0f,
                        ballPosition.z - attackSign * 0.6f
                    )
                    it.facingDirection = Vector3(0f, 0f, attackSign)
                }
                val toGoal = Vector3(-ballPosition.x * 0.35f, 0f, goalZ - ballPosition.z).normalized()
                defenders.filter { !it.isGoalkeeper }.sortedBy { it.position.distanceTo(ballPosition) }
                    .take(2).forEachIndexed { i, d ->
                        d.position = ballPosition + toGoal * (9.15f + i * 1.8f)
                    }
            }
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

        if (data.kind == SetPieceKind.PENALTY) {
            val attackingSide = data.attackingSide
            val taker = allActive().filter { it.teamSide == attackingSide && !it.isGoalkeeper }
                .maxByOrNull { it.shooting }
            if (taker != null) {
                assignBallOwner(taker)
                shootFrom(taker)
                // 门将随机扑救方向
                gkDiveTimer = 0.8f
                gkDiveTargetX = (Random.nextFloat() * 4.4f - 2.2f)
            }
        } else {
            // 任意球/界外球/球门球/角球：离球最近的本方球员（含门将）拿球
            val attackingSide = data.attackingSide
            val taker = allActive().filter { it.teamSide == attackingSide }
                .minByOrNull { it.position.distanceTo(ballPosition) }
            if (taker != null) {
                assignBallOwner(taker)
                // 门将开球门球不交给玩家操控（门将随后自动大脚开出）
                if (taker.teamSide == playerSide && !taker.isGoalkeeper) {
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

    /** 出界 → 鸣哨 + 对手球权 + 摆放定位球（awardedSide = 最后触球方的对手） */
    private fun awardSetPiece(kind: SetPieceKind, spot: Vector3, banner: String) {
        val awardedSide = lastTouch?.let { opponentSide(it.teamSide) } ?: GameState.TeamSide.HOME
        referee.whistleTimer = 0.9f
        onSound?.invoke("whistle_short")
        ballOwner = null
        ballVelocity = Vector3.ZERO
        ballHeightVelocity = 0f
        lastShotMissed = false
        advantageTimer = 0f
        advantageSpot = null
        advantageSide = null
        pendingSetPiece = SetPieceData(kind = kind, spot = spot, attackingSide = awardedSide)
        phase = MatchPhase.FOUL_STOP
        freezeTimer = if (kind == SetPieceKind.THROW_IN) 0.25f else 0.6f
        onBanner?.invoke(banner)
    }

    private fun opponentSide(side: GameState.TeamSide): GameState.TeamSide =
        if (side == GameState.TeamSide.HOME) GameState.TeamSide.AWAY else GameState.TeamSide.HOME

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
        val pressured = nearestOpponentDist(p) < 2.4f
        when {
            inShootRange(p) && (distToGoal < 12f || Random.nextFloat() < 0.6f) -> shootFrom(p)
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

    private fun goalkeeperAI(p: Player, delta: Float) {
        val ownSign = if (p.teamSide == GameState.TeamSide.HOME) -1f else 1f
        val goalZ = ownSign * GameState.FIELD_LENGTH / 2
        val lineZ = goalZ - ownSign * 2.5f
        val halfL = GameState.FIELD_LENGTH / 2

        var targetX = (ballPosition.x * 0.45f).coerceIn(-5.5f, 5.5f)
        var targetZ = lineZ
        var speedFactor = 1.1f

        // 点球扑救：飞向预定方向
        if (gkDiveTimer > 0f) {
            targetX = gkDiveTargetX
            speedFactor = 2.2f
        }

        // 自动出击（时机收紧）：自由球落在本方禁区内、且我是全队离球最近的防守者才出击，
        // 否则守在门线附近 —— 减少"出击拿不到球被打空门"
        val inOwnBox = ownSign * ballPosition.z > halfL - 16.5f &&
            kotlin.math.abs(ballPosition.x) < 20.16f
        val squad = if (p.teamSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val iAmClosest = squad.filter { !it.sentOff }
            .minByOrNull { it.position.distanceTo(ballPosition) } === p
        if (ballOwner == null && inOwnBox && iAmClosest && ballHeight < 2.4f) {
            targetX = ballPosition.x
            targetZ = ballPosition.z
            speedFactor = 1.6f
        }
        movePlayer(p, Vector3(targetX, 0f, targetZ), delta, speedFactor)

        // 抱球条件收紧：球朝自己来或已基本停稳时才抱（不出击拿不到球）
        val ballIncoming = ownSign * ballVelocity.z > 0f || ballVelocity.length() < 6f
        if (ballOwner == null && pickupCooldown <= 0f && ballHeight < 2.0f &&
            p.position.distanceTo(ballPosition) < 1.8f && ballIncoming
        ) {
            assignBallOwner(p)
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
        var dir = toTarget.normalized()
        // 跳起躲人（避障）：前方 1.8m 内有倒地/滑铲球员时向空侧绕行
        for (o in downedPlayers) {
            if (o === p) continue
            val rx = o.position.x - p.position.x
            val rz = o.position.z - p.position.z
            val ahead = rx * dir.x + rz * dir.z
            if (ahead > 0f && ahead < 1.8f) {
                val side = rx * dir.z - rz * dir.x
                val deg = if (side > 0f) 60.0 else -60.0
                val rad = Math.toRadians(deg)
                val nx = dir.x * cos(rad) - dir.z * sin(rad)
                val nz = dir.x * sin(rad) + dir.z * cos(rad)
                dir = Vector3(nx, 0f, nz)
                break
            }
        }
        val speed = p.getGameStats().speed * speedFactor
        p.velocity = dir * speed.coerceAtMost(dist / delta)
        p.position = p.position + p.velocity * delta
        p.facingDirection = dir
    }

    // ==================== 裁判 ====================

    /**
     * 裁判 AI（对角线跑位）：跟随进攻方向，保持在球斜后方约 9m、
     * 固定一侧通道（+x 侧偏移），与球至少 6m，不挡传球/射门路线；
     * 定位球时站事发点斜侧 6m。
     */
    private fun updateReferee(delta: Float) {
        var target: Vector3 = when (phase) {
            MatchPhase.PLAYING -> Vector3(
                ballPosition.x * 0.55f + 8f,
                0f,
                ballPosition.z - lastAttackSign * 9f
            )
            else -> {
                val spot = pendingSetPiece?.spot ?: ballPosition
                Vector3(spot.x + 6f, 0f, spot.z - 6f)
            }
        }
        // 与球保持至少 6m
        val toTarget = target - ballPosition
        if (toTarget.length() < 6f) {
            val dir = toTarget.normalized()
            target = Vector3(ballPosition.x + dir.x * 6f, 0f, ballPosition.z + dir.z * 6f)
        }

        val dx = target.x - referee.position.x
        val dz = target.z - referee.position.z
        val dist = kotlin.math.sqrt(dx * dx + dz * dz)
        if (dist > 0.5f) {
            val speed = 7.4f
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
        // 限制在场地附近
        referee.position = Vector3(
            referee.position.x.coerceIn(-GameState.FIELD_WIDTH / 2 - 2f, GameState.FIELD_WIDTH / 2 + 2f),
            0f,
            referee.position.z.coerceIn(-GameState.FIELD_LENGTH / 2 + 1f, GameState.FIELD_LENGTH / 2 - 1f)
        )
    }

    // ==================== 球的物理 ====================

    private fun updateBall(delta: Float) {
        val owner = ballOwner ?: run {
            updateFreeBall(delta)
            return
        }

        // ===== 爆趟：带球按住加速，每 0.45s 把球往前趟出一大步（自己追），风险与收益并存 =====
        if (owner.isPlayerControlled && isSprinting) {
            sprintKnockTimer += delta
            if (sprintKnockTimer >= 0.45f) {
                val dir = owner.facingDirection
                ballVelocity = dir * (owner.velocity.length() * 1.4f + 2.5f)
                ballHeightVelocity = 0.2f
                ballOwner = null
                owner.hasBall = false
                lastTouch = owner
                pickupCooldown = 0.12f
                ballControlTime = 0f
                sprintKnockTimer = 0f
                return
            }
        } else {
            sprintKnockTimer = 0f
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

        // 持球越界（球员还在盘带但球已出线）→ 立即鸣哨收球权
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2
        if (kotlin.math.abs(ballPosition.x) > halfW + 0.15f) {
            val label = if (owner.teamSide == playerSide) "球出界 · 对方界外球" else "球出界 · 我方界外球"
            awardSetPiece(
                SetPieceKind.THROW_IN,
                Vector3(
                    if (ballPosition.x > 0) halfW - 0.4f else -halfW + 0.4f,
                    0f,
                    ballPosition.z.coerceIn(-halfL + 2f, halfL - 2f)
                ),
                label
            )
            return
        }
        val overTop = ballPosition.z > halfL + 0.15f
        val overBottom = ballPosition.z < -halfL - 0.15f
        if (overTop || overBottom) {
            val endSign = if (overTop) 1f else -1f
            val inGoalMouth = kotlin.math.abs(ballPosition.x) < GameState.GOAL_WIDTH / 2
            if (!inGoalMouth) {
                val attackingSide = if (endSign > 0) GameState.TeamSide.HOME else GameState.TeamSide.AWAY
                if (owner.teamSide == attackingSide) {
                    val label = if (lastShotMissed) "射门偏出 · 球门球" else "球出界 · 球门球"
                    awardSetPiece(
                        SetPieceKind.GOAL_KICK,
                        Vector3(if (ballPosition.x >= 0) 9f else -9f, 0f, endSign * (halfL - 5.5f)),
                        label
                    )
                } else {
                    awardSetPiece(
                        SetPieceKind.CORNER,
                        Vector3(
                            (if (ballPosition.x >= 0) 1f else -1f) * (halfW - 0.6f),
                            0f,
                            endSign * (halfL - 0.6f)
                        ),
                        "球出界 · 角球"
                    )
                }
            }
        }
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
        // 出界不再反弹：越过边线/底线 → 由 checkGoalAndBounds 判界外球/球门球/角球
    }

    /**
     * 出界与进球判定（IFAB Law 15/16/17）：
     * - 整体越过边线 → 界外球（最后触球方的对手掷球）
     * - 越过球门线非进球 → 进攻方最后触球 = 球门球；防守方最后触球 = 角球
     * - 球门线内低于横梁 → 进球
     */
    private fun checkGoalAndBounds() {
        val halfW = GameState.FIELD_WIDTH / 2
        val halfL = GameState.FIELD_LENGTH / 2
        val inGoalMouth = kotlin.math.abs(ballPosition.x) < GameState.GOAL_WIDTH / 2 && ballHeight < 2.44f

        // ===== 边线 → 界外球 =====
        if (kotlin.math.abs(ballPosition.x) > halfW + 0.15f) {
            awardSetPiece(
                SetPieceKind.THROW_IN,
                Vector3(
                    if (ballPosition.x > 0) halfW - 0.4f else -halfW + 0.4f,
                    0f,
                    ballPosition.z.coerceIn(-halfL + 2f, halfL - 2f)
                ),
                "界外球"
            )
            return
        }

        // ===== 底线 =====
        val overTop = ballPosition.z > halfL + 0.15f
        val overBottom = ballPosition.z < -halfL - 0.15f
        if (overTop || overBottom) {
            val endSign = if (overTop) 1f else -1f
            if (inGoalMouth) {
                onGoal?.invoke(if (endSign > 0) GameState.TeamSide.HOME else GameState.TeamSide.AWAY)
                kickoffReset()
                return
            }
            val attackingSide = if (endSign > 0) GameState.TeamSide.HOME else GameState.TeamSide.AWAY
            if (lastTouch?.teamSide == attackingSide) {
                // 进攻方最后触球 → 球门球
                val label = if (lastShotMissed) "射门偏出 · 球门球" else "球门球"
                awardSetPiece(
                    SetPieceKind.GOAL_KICK,
                    Vector3(if (ballPosition.x >= 0) 9f else -9f, 0f, endSign * (halfL - 5.5f)),
                    label
                )
            } else {
                // 防守方最后触球 → 角球
                awardSetPiece(
                    SetPieceKind.CORNER,
                    Vector3(
                        (if (ballPosition.x >= 0) 1f else -1f) * (halfW - 0.6f),
                        0f,
                        endSign * (halfL - 0.6f)
                    ),
                    "角球"
                )
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
        lastPasser = null
        receiveWindow = 0f
        ballControlTime = 0f
        pickupCooldown = 0.3f
        gkDiveTimer = 0f
        callPressing = false
        sprintKnockTimer = 0f
        lastActionMode = ActionMode.SPRINT
        advantageTimer = 0f
        advantageSpot = null
        advantageSide = null
        lastShotMissed = false
        stats = MatchStats()
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

    // ==================== 大按钮三态（加速 / 射门 / 铲球） ====================

    /**
     * 右侧大按钮当前该显示的动作（UI 每帧读取，驱动按键文案与颜色）：
     * - 己方持球：刚接队友传球 或 已进入射门范围 → 射门；否则 → 加速（按住=带球爆趟）
     * - 对方持球：与持球者贴身 → 铲球；否则 → 加速
     * - 自由球 → 加速
     *
     * 迟滞切换：SHOOT 进入 24m/18m、退出 29m/22m；TACKLE 进入 2.8m、退出 3.6m
     * → 阈值附近反复横跳（按钮抽搐/射门键闪没）消除
     */
    fun currentActionMode(): ActionMode {
        val me = activePlayer ?: return ActionMode.SPRINT
        val owner = ballOwner ?: return ActionMode.SPRINT
        val mode = when {
            owner.teamSide == playerSide -> {
                if (owner === me && (receiveWindow > 0f ||
                            inShootRange(me) ||
                            (lastActionMode == ActionMode.SHOOT && inShootZone(me, 29f, 22f)))
                ) {
                    ActionMode.SHOOT
                } else {
                    ActionMode.SPRINT
                }
            }
            me.position.distanceTo(owner.position) <=
                (if (lastActionMode == ActionMode.TACKLE) TACKLE_EXIT_DIST else TACKLE_TRIGGER_DIST)
            -> ActionMode.TACKLE
            else -> ActionMode.SPRINT
        }
        lastActionMode = mode
        return mode
    }

    /** 射门区域（zRange = 距对方球门线纵深，xRange = 横向半宽） */
    private fun inShootZone(p: Player, zRange: Float, xRange: Float): Boolean {
        val attackZ = if (p.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }
        return kotlin.math.abs(attackZ - p.position.z) < zRange && kotlin.math.abs(p.position.x) < xRange
    }

    /** 是否已进入射门范围（与 AI 持球决策同一标准） */
    private fun inShootRange(p: Player): Boolean = inShootZone(p, 24f, 18f)

    // ==================== 解围 ====================

    /** 是否在本方禁区附近（本方球门 30m 半径内，用于显示"解围"按钮） */
    fun inOwnBoxZone(p: Player): Boolean {
        val ownGoalZ = if (p.teamSide == GameState.TeamSide.HOME) {
            -GameState.FIELD_LENGTH / 2
        } else {
            GameState.FIELD_LENGTH / 2
        }
        return p.position.distanceTo(Vector3(0f, 0f, ownGoalZ)) < 30f
    }

    /** 当前是否可解围：操控球员己方控球且在本方禁区附近 */
    fun canClear(): Boolean {
        val owner = ballOwner ?: return false
        if (!owner.isPlayerControlled) return false
        return inOwnBoxZone(owner)
    }

    /** 解围：本方禁区附近大脚踢向前场（高球远踢） */
    fun doClearance() {
        val player = activePlayer ?: return
        if (ballOwner != player) return
        val attackSign = if (player.teamSide == GameState.TeamSide.HOME) 1f else -1f
        val dir = Vector3(
            Random.nextFloat() * 24f - 12f,
            0f,
            attackSign
        ).normalized()
        ballVelocity = dir * (GameState.SHOT_SPEED * 0.9f)
        ballHeightVelocity = 5f + Random.nextFloat() * 2f
        ballOwner = null
        player.hasBall = false
        lastTouch = player
        pickupCooldown = 0.4f
        ballControlTime = 0f
        player.animState = Player.AnimState.KICK
        player.actionCooldown = 0.4f
        onSound?.invoke("kick")
    }

    // ==================== 换人系统 ====================

    /** 可用替补：替补席中尚未上场的球员（名额用完返回空列表） */
    fun substitutesFor(side: GameState.TeamSide): List<Player> {
        val bench = if (side == GameState.TeamSide.HOME) homeBench else awayBench
        val field = if (side == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val used = if (side == GameState.TeamSide.HOME) homeSubsUsed else awaySubsUsed
        if (used >= MAX_SUBS) return emptyList()
        return bench.filter { b -> field.none { it === b } }
    }

    /**
     * 主动换人：替补顶替所选场上球员的位置槽（选谁下场 = 选替补打哪个位置）
     * 门将位只能由门将替补顶替（场上必须保持一名门将）；返回是否成功。
     */
    fun substitute(outgoing: Player, sub: Player): Boolean {
        val side = outgoing.teamSide
        if (outgoing.isGoalkeeper != sub.isGoalkeeper) return false
        if (substitutesFor(side).none { it === sub }) return false
        val squad = if (side == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val idx = squad.indexOfFirst { it === outgoing }
        if (idx < 0) return false

        val wasActive = activePlayer === outgoing
        val wasControlled = outgoing.isPlayerControlled

        // 替补接管槽位：位置/朝向/阵型落位与被换者一致
        sub.position = Vector3(outgoing.position.x, 0f, outgoing.position.z)
        sub.homePosition = outgoing.homePosition
        sub.velocity = Vector3.ZERO
        sub.facingDirection = outgoing.facingDirection
        sub.hasBall = false
        sub.isActive = wasActive
        sub.isPlayerControlled = wasControlled
        sub.isSprinting = false
        sub.fallTimer = 0f
        sub.slideTimer = 0f
        sub.tackleCooldown = 0f
        sub.actionCooldown = 0f
        sub.yellowCards = 0
        sub.sentOff = false
        sub.animState = Player.AnimState.IDLE

        outgoing.isActive = false
        outgoing.isPlayerControlled = false
        if (wasActive) activePlayer = sub
        // 被换者正持球 → 球权交给替补
        if (ballOwner === outgoing) assignBallOwner(sub)

        val newList = squad.toMutableList()
        newList[idx] = sub
        if (side == GameState.TeamSide.HOME) {
            homePlayers = newList
            homeSubsUsed++
        } else {
            awayPlayers = newList
            awaySubsUsed++
        }
        return true
    }

    // ==================== 动作接口 ====================

    fun doPass() {
        val player = activePlayer ?: return
        passFrom(player)
    }

    /** 射门（可带力度 0~1：快点=半力推射，长按蓄力=满力爆射） */
    fun doShoot(power: Float = 0.5f) {
        val player = activePlayer ?: return
        shootFrom(player, power)
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
            lastPasser = player
            passTravelTimer = 0f
            if (player.teamSide == GameState.TeamSide.HOME) stats.homePasses++ else stats.awayPasses++
            val lead = Vector3(0f, 0f, forwardDir * 8f)
            val targetPos = bestTarget.position + lead
            val dir = (targetPos - player.position).flatten().normalized()
            // 挑传（直塞升级）：高弧线越过封堵，落点带提前量
            ballVelocity = dir * (GameState.PASS_SPEED * 1.15f)
            ballHeightVelocity = 2.4f
            ballOwner = null
            player.hasBall = false
            lastTouch = player
            pickupCooldown = 0.4f
            ballControlTime = 0f
            onSound?.invoke("kick")
        }
    }

    /** 传球通道检测：中段有对方球员靠近 → 刀山球 */
    private fun laneBlocked(from: Player, to: Player): Boolean {
        val ax = from.position.x
        val az = from.position.z
        val abx = to.position.x - ax
        val abz = to.position.z - az
        val len2 = abx * abx + abz * abz
        if (len2 < 0.01f) return false
        val opponents = if (from.teamSide == GameState.TeamSide.HOME) awayPlayers else homePlayers
        for (o in opponents) {
            if (o.sentOff) continue
            val t = ((o.position.x - ax) * abx + (o.position.z - az) * abz) / len2
            if (t < 0.12f || t > 0.88f) continue
            val cx = ax + abx * t
            val cz = az + abz * t
            val dx = o.position.x - cx
            val dz = o.position.z - cz
            if (dx * dx + dz * dz < 3.6f) return true   // ~1.9m 内
        }
        return false
    }

    /** 绕 Y 轴旋转（XZ 平面，角度制） */
    private fun rotateY(v: Vector3, deg: Float): Vector3 {
        val rad = Math.toRadians(deg.toDouble())
        val c = cos(rad)
        val s = sin(rad)
        return Vector3(v.x * c - v.z * s, 0f, v.x * s + v.z * c)
    }

    /** 通用传球（指定出球者）：通道拦截评分 + 受压迫低质量传球失误 */
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
                // 传球选点：通道被拦截 = 刀山球，大幅降分（减少有空当却传被断）
                val lanePenalty = if (laneBlocked(player, teammate)) 25f else 0f
                val score = forwardScore - dist * 0.2f - lanePenalty
                if (score > bestScore) {
                    bestScore = score
                    bestTarget = teammate
                }
            }
        }

        if (bestTarget != null) {
            lastPasser = player
            passTravelTimer = 0f
            if (player.teamSide == GameState.TeamSide.HOME) stats.homePasses++ else stats.awayPasses++
            var dir = (bestTarget.position - player.position).flatten().normalized()
            var speed = GameState.PASS_SPEED
            // 低质量传球判定：受压迫/刀山球 → 合理失误（传偏）
            var errorChance = 0f
            val pressure = nearestOpponentDist(player)
            if (pressure < 2.4f) errorChance += (2.4f - pressure) * 0.22f
            if (laneBlocked(player, bestTarget)) errorChance += 0.15f
            if (Random.nextFloat() < errorChance) {
                val dev = (if (Random.nextBoolean()) 1 else -1) * (18 + Random.nextInt(24))
                dir = rotateY(dir, dev.toFloat())
                speed *= 0.85f
            }
            ballVelocity = dir * speed
            ballHeightVelocity = 0.5f
            ballOwner = null
            player.hasBall = false
            lastTouch = player
            pickupCooldown = 0.4f
            ballControlTime = 0f
            onSound?.invoke("kick")
        }
    }

    /**
     * 通用射门（指定射门者）
     * power 0~1：越大球速越快（0.8~1.3 倍）、球越高、准头越差（大力出奇迹也有风险）
     */
    fun shootFrom(player: Player, power: Float = 0.5f) {
        if (ballOwner != player) return
        val p = power.coerceIn(0f, 1f)

        val targetGoalZ = if (player.teamSide == GameState.TeamSide.HOME) {
            GameState.FIELD_LENGTH / 2
        } else {
            -GameState.FIELD_LENGTH / 2
        }

        val spread = 1.2f + 2.6f * p
        val dir = Vector3(
            Random.nextFloat() * spread * 2f - spread - player.position.x * 0.05f,
            0f,
            targetGoalZ - player.position.z
        ).normalized()

        ballVelocity = dir * (GameState.SHOT_SPEED * (0.8f + 0.5f * p))
        ballHeightVelocity = 1.2f + 2.4f * p
        ballOwner = null
        player.hasBall = false
        lastTouch = player
        pickupCooldown = 0.4f
        ballControlTime = 0f
        player.animState = Player.AnimState.KICK
        player.actionCooldown = 0.4f
        if (player.teamSide == GameState.TeamSide.HOME) stats.homeShots++ else stats.awayShots++
        lastShotMissed = true
        onSound?.invoke("kick")
    }

    /** 手动切换到离球最近的己方外场球员（"切换"键，防抖 0.8s；球门一侧的球员优先） */
    fun switchToNearest() {
        val squad = if (playerSide == GameState.TeamSide.HOME) homePlayers else awayPlayers
        val me = activePlayer
        val ownSign = if (playerSide == GameState.TeamSide.HOME) -1f else 1f
        val target = squad
            .filter { it !== me && !it.sentOff && !it.isGoalkeeper && it.fallTimer <= 0f && it.slideTimer <= 0f }
            .minByOrNull {
                var d = it.position.distanceTo(ballPosition)
                if (ownSign * it.position.z > ownSign * ballPosition.z) d -= 2.5f
                d
            } ?: return
        switchControlTo(target)
        autoSwitchCooldown = 0.8f
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

        /** 贴身判定阈值：与对方持球者距离 ≤ 此值时，大按钮从"加速"切为"铲球" */
        const val TACKLE_TRIGGER_DIST = 2.8f

        /** 铲球退出阈值（迟滞）：≥ 此值才切回加速，防贴边抖动 */
        private const val TACKLE_EXIT_DIST = 3.6f

        /** 每队换人名额上限 */
        const val MAX_SUBS = 5

        // 替补席：1 门将 + 5 外场（门将位只能由门将替补顶替）
        private val BENCH_ROLES = listOf("GK", "CB", "LB", "CM", "RW", "FW")
        private val BENCH_NUMBERS = listOf(12, 13, 14, 15, 16, 17)

        // 4-3-3 基础站位 (x, z)，主队进攻 +z，场地 105 x 68
        // 下标：0=GK 1=LB 2/3=CB 4=RB 5/6/7=CM 8=LW 9=ST 10=RW（招牌球星按角色卡可落在任意外场槽）
        private val BASE_POSITIONS = listOf(
            Pair(0f, -44f),                                                   // GK
            Pair(-22f, -30f), Pair(-8f, -33f), Pair(8f, -33f), Pair(22f, -30f), // DF
            Pair(-18f, -12f), Pair(0f, -16f), Pair(18f, -12f),                 // MF
            Pair(-24f, 4f), Pair(0f, 2f), Pair(24f, 4f)                        // FW
        )

        /**
         * 为一支球队生成 11 名球员（4-3-3）
         * 招牌球星所在槽位由角色卡决定（StarLikeness.starSlotForTeam），默认 9 = 中锋
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

        /**
         * 生成替补席（1 门将 + 5 外场，站在场外待命，换人后顶替所选位置槽上场）
         */
        fun createBenchPlayers(team: com.football.game.model.Team?, side: GameState.TeamSide): List<Player> {
            val offZ = if (side == GameState.TeamSide.HOME) -62f else 62f
            return BENCH_ROLES.mapIndexed { i, role ->
                Player(
                    id = "${team?.id ?: side.name}_sub_$i",
                    name = "${BENCH_NUMBERS[i]}号",
                    number = BENCH_NUMBERS[i],
                    role = role,
                    teamSide = side,
                    teamId = team?.id ?: "",
                    teamName = team?.name ?: "",
                    isGoalkeeper = role == "GK",
                    position = Vector3((i - 2.5f) * 4f, 0f, offZ),
                    homePosition = Vector3((i - 2.5f) * 4f, 0f, offZ)
                )
            }
        }
    }
}

private fun sin(rad: Double): Float = kotlin.math.sin(rad).toFloat()
private fun cos(rad: Double): Float = kotlin.math.cos(rad).toFloat()
