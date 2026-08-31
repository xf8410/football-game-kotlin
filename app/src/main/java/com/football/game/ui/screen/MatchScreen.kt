package com.football.game.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.football.game.core.GameEngine
import com.football.game.core.GameState
import com.football.game.core.GoalAnnouncement
import com.football.game.core.GoalTypes
import com.football.game.data.StarLikeness
import com.football.game.model.Player
import com.football.game.model.Team
import com.football.game.render.GameGLSurfaceView
import com.football.game.sound.SoundManager
import com.football.game.ui.component.GoalAnnouncementUI
import com.football.game.ui.component.HudCircleButton
import com.football.game.ui.component.TouchControls
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 比赛屏幕：真实 OpenGL 3D 场景（最佳球会 × 实况足球 风格 HUD）
 * 顶部记分栏 + 跳过/暂停/换人圆键 + 底部雷达小地图 + 半透明圆形情境按键
 * 招牌球星按角色卡落在对应位置槽（同一球星不同球队不同职责，如阿什拉夫在大巴黎踢边锋）
 *
 * 抽搐修复：比赛模拟由 GL 渲染线程逐帧驱动（GameGLSurfaceView.onFrameUpdate），
 * 先 gameEngine.update(dt) 再绘制 → 模拟与画面同帧同相；
 * 旧方案（协程 delay(16) 固定步长 vs 渲染线程自由节拍）的不同步抖动彻底移除。
 */
@Composable
fun MatchScreen(
    homeTeamName: String = "红队",
    awayTeamName: String = "蓝队",
    homeTeam: Team? = null,
    awayTeam: Team? = null,
    onMatchEnd: () -> Unit = {}
) {
    var matchTime by remember { mutableFloatStateOf(0f) }
    var homeScore by remember { mutableIntStateOf(0) }
    var awayScore by remember { mutableIntStateOf(0) }
    var currentHalf by remember { mutableIntStateOf(1) }
    var isPaused by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var skipping by remember { mutableStateOf(false) }
    var skipAvailable by remember { mutableStateOf(false) }
    var radarFrame by remember { mutableIntStateOf(0) }
    var currentAnnouncement by remember { mutableStateOf<GoalAnnouncement?>(null) }
    var hasBall by remember { mutableStateOf(false) }
    var actionMode by remember { mutableStateOf(GameEngine.ActionMode.SPRINT) }
    var showClearance by remember { mutableStateOf(false) }
    var defending by remember { mutableStateOf(false) }
    var showSubDialog by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }
    var glView by remember { mutableStateOf<GameGLSurfaceView?>(null) }

    val match = remember {
        com.football.game.model.Match(
            homeTeam = homeTeam ?: Team(id = "home", name = homeTeamName, shortName = "HOM"),
            awayTeam = awayTeam ?: Team(id = "away", name = awayTeamName, shortName = "AWY")
        )
    }

    // 招牌球星槽位（角色卡解析：默认中锋；阿什拉夫在大巴黎 = 边锋槽 10）
    val homeStarSlot = remember(match) { StarLikeness.starSlotForTeam(match.homeTeam) }
    val awayStarSlot = remember(match) { StarLikeness.starSlotForTeam(match.awayTeam) }

    // 十一名球员（4-3-3）+ 各 6 名替补（含门将）
    val gameEngine = remember(match) {
        GameEngine(
            match = match,
            homePlayers = GameEngine.createTeamPlayers(match.homeTeam, GameState.TeamSide.HOME),
            awayPlayers = GameEngine.createTeamPlayers(match.awayTeam, GameState.TeamSide.AWAY)
        ).apply {
            activePlayer = homePlayers.getOrNull(homeStarSlot)
            activePlayer?.isActive = true
            activePlayer?.isPlayerControlled = true
        }
    }

    // 球员外观（队服系统：经典配色/竖条纹/斜杠 + 独立球裤球袜 + 撞衫自动换客场 + 门将独立荧光色）
    val (homeLooks, awayLooks) = remember(match) {
        StarLikeness.looksForMatch(match.homeTeam, match.awayTeam, 11)
    }

    // 进球处理
    fun handleGoal(scoringSide: GameState.TeamSide) {
        val isHome = scoringSide == GameState.TeamSide.HOME
        if (isHome) homeScore++ else awayScore++
        SoundManager.play(SoundManager.Sfx.CHEER)

        val scorer = gameEngine.lastTouch
        val minute = (matchTime / 60f).toInt() + if (currentHalf == 2) 45 else 0

        currentAnnouncement = GoalTypes.generateGoalAnnouncement(
            goalMethod = GoalTypes.GoalMethod.SHOT_NORMAL,
            scorerName = scorer?.let { "${it.number}号" } ?: "球员",
            minute = minute,
            goalCount = 1,
            teamName = if (isHome) homeTeamName else awayTeamName,
            opponentName = if (isHome) awayTeamName else homeTeamName,
            currentScore = Pair(homeScore, awayScore),
            goalPosition = GoalTypes.detectGoalPosition(0f, 0f, GameState.GOAL_WIDTH),
            goalContext = GoalTypes.detectGoalContext(
                teamScore = if (isHome) homeScore else awayScore,
                opponentScore = if (isHome) awayScore else homeScore,
                minute = minute,
                isExtraTime = false,
                isSecondHalf = currentHalf == 2
            )
        )
    }

    // 引擎事件接线（音效 + 横幅 + 进球）
    LaunchedEffect(Unit) {
        gameEngine.onSound = { key ->
            when (key) {
                "whistle" -> SoundManager.play(SoundManager.Sfx.WHISTLE)
                "whistle_short" -> SoundManager.play(SoundManager.Sfx.WHISTLE_SHORT)
                "kick" -> SoundManager.play(SoundManager.Sfx.KICK)
                "tackle" -> SoundManager.play(SoundManager.Sfx.TACKLE)
            }
        }
        gameEngine.onBanner = { text -> bannerText = text }
        gameEngine.onGoal = { side -> handleGoal(side) }
    }

    // 横幅自动消失
    LaunchedEffect(bannerText) {
        if (bannerText != null) {
            delay(2800L)
            bannerText = null
        }
    }

    // 比赛时钟（50 倍加速：+5s 游戏时间 / 100ms；跳过时由渲染回调接管）
    LaunchedEffect(isPaused, isFinished) {
        while (matchTime < 2700f) {
            delay(100L)
            if (!isPaused && !isFinished && !skipping) {
                matchTime += 5f
                if (matchTime >= 2700f && currentHalf == 1) {
                    currentHalf = 2
                    matchTime = 0f
                } else if (matchTime >= 2700f && currentHalf == 2) {
                    isFinished = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E7D32))
    ) {
        // OpenGL 3D 场景（比赛模拟由渲染线程逐帧驱动：先 update 后 draw，同帧同相，消除抽搐）
        AndroidView(
            factory = { ctx ->
                GameGLSurfaceView(ctx).also { view ->
                    glView = view
                    view.onFrameUpdate = { dt ->
                        if (!isPaused && !isFinished) {
                            if (skipping) {
                                repeat(48) { gameEngine.update(0.016f) }
                                matchTime += 5f * 48
                                if (matchTime >= 2700f) {
                                    if (currentHalf == 1) {
                                        currentHalf = 2
                                        matchTime = 0f
                                    } else {
                                        isFinished = true
                                        skipping = false
                                    }
                                }
                            } else {
                                gameEngine.update(dt)
                            }
                            // 每帧推送渲染数据（引用拷贝，代价极低）
                            view.updateGameData(
                                homePlayers = gameEngine.homePlayers,
                                awayPlayers = gameEngine.awayPlayers,
                                ballPosition = gameEngine.ballPosition,
                                ballHeight = gameEngine.ballHeight,
                                activePlayerIndex = gameEngine.homePlayers.indexOf(gameEngine.activePlayer),
                                homeLooks = homeLooks,
                                awayLooks = awayLooks,
                                referee = gameEngine.referee
                            )
                            hasBall = gameEngine.ballOwner?.isPlayerControlled == true
                            actionMode = gameEngine.currentActionMode()
                            showClearance = gameEngine.canClear()
                            val owner = gameEngine.ballOwner
                            defending = owner == null || owner.teamSide != gameEngine.playerSide
                            skipAvailable = abs(homeScore - awayScore) >= 3
                            radarFrame++
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部栏：记分板 + 跳过/暂停/换人
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Scoreboard(
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                homeScore = homeScore,
                awayScore = awayScore,
                matchTime = matchTime,
                currentHalf = currentHalf,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // ⏩ 跳过：净胜 3 球后开放（3:0 可跳、3:1 不行）
            HudCircleButton(
                label = "⏩",
                size = 44.dp,
                enabled = skipAvailable && !isPaused && !isFinished
            ) {
                skipping = true
                bannerText = "净胜3球，跳过剩余比赛…"
            }
            Spacer(modifier = Modifier.width(6.dp))
            HudCircleButton(label = if (isPaused) "▶" else "⏸", size = 44.dp) {
                isPaused = !isPaused
                if (isPaused) skipping = false
            }
            Spacer(modifier = Modifier.width(6.dp))
            HudCircleButton(label = "换人", size = 44.dp) {
                isPaused = true
            }
        }

        GoalAnnouncementUI(
            announcement = currentAnnouncement,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 104.dp, start = 16.dp)
        )

        // 犯规/牌/定位球横幅
        bannerText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 150.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = text,
                    color = Color(0xFFFFD54F),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 底部雷达小地图（最佳球会风格）
        RadarMiniMap(
            gameEngine = gameEngine,
            frame = radarFrame,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(width = 180.dp, height = 120.dp)
        )

        TouchControls(
            gameEngine = gameEngine,
            actionMode = actionMode,
            hasBall = hasBall,
            showClearance = showClearance,
            defending = defending,
            modifier = Modifier.fillMaxSize()
        )

        // 比赛暂停面板（比赛计划：角色卡 + 统计 + 阵容 + 继续/换人/重新挑战/放弃）
        if (isPaused && !showSubDialog && !isFinished) {
            PauseMenuDialog(
                gameEngine = gameEngine,
                homeTeam = match.homeTeam,
                starSlot = homeStarSlot,
                onResume = { isPaused = false },
                onOpenSubs = { showSubDialog = true },
                onRestart = {
                    homeScore = 0
                    awayScore = 0
                    matchTime = 0f
                    currentHalf = 1
                    skipping = false
                    isFinished = false
                    (gameEngine.homePlayers + gameEngine.awayPlayers).forEach { p ->
                        p.yellowCards = 0
                        p.sentOff = false
                        p.hasBall = false
                        p.fallTimer = 0f
                        p.slideTimer = 0f
                        p.isActive = false
                        p.isPlayerControlled = false
                        p.animState = Player.AnimState.IDLE
                    }
                    gameEngine.kickoffReset()
                    gameEngine.homePlayers.getOrNull(homeStarSlot)?.let { star ->
                        star.isActive = true
                        star.isPlayerControlled = true
                        gameEngine.activePlayer = star
                    }
                    bannerText = null
                },
                onAbandon = onMatchEnd
            )
        }

        // 主动换人面板（从暂停面板进入，关闭后回到暂停面板）
        if (showSubDialog) {
            SubstitutionDialog(
                gameEngine = gameEngine,
                onDismiss = { showSubDialog = false }
            )
        }
    }
}

/**
 * 记分板（最佳球会风格）：队名 + 比分 + 累计时钟（上半场 0-45'，下半场 45-90'）
 */
@Composable
fun Scoreboard(
    homeTeamName: String,
    awayTeamName: String,
    homeScore: Int,
    awayScore: Int,
    matchTime: Float,
    currentHalf: Int,
    modifier: Modifier = Modifier
) {
    val total = (currentHalf - 1) * 2700 + matchTime
    val mm = (total / 60).toInt()
    val ss = (total % 60).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(homeTeamName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(awayTeamName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$homeScore", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text("-", color = Color.Gray, fontSize = 20.sp)
            Text("$awayScore", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentHalf == 1) "上半场" else "下半场",
                color = Color(0xFFBDBDBD),
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
            Text(
                text = String.format("%02d:%02d", mm, ss),
                color = Color(0xFFFFD54F),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 雷达小地图：全场球员/球/操控标记（半透明深底）
 * frame 每帧递增驱动重绘，直接读取引擎实时数据
 */
@Composable
fun RadarMiniMap(
    gameEngine: GameEngine,
    frame: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 场地底 + 边线 + 中线
        drawRect(color = Color.Black.copy(alpha = 0.4f), size = size)
        drawRect(
            color = Color.White.copy(alpha = 0.45f),
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(width = 1.dp.toPx())
        )
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(w / 2f, 0f),
            end = Offset(w / 2f, h),
            strokeWidth = 1.dp.toPx()
        )

        // 坐标映射：x ∈ [-34, 34]（宽 68），z ∈ [-52.5, 52.5]（长 105）
        fun mapX(x: Float) = (x / 68f + 0.5f) * w
        fun mapY(z: Float) = (z / 105f + 0.5f) * h

        // 主队（白）客队（红）
        gameEngine.homePlayers.forEach { p ->
            if (!p.sentOff) {
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(mapX(p.position.x), mapY(p.position.z))
                )
            }
        }
        gameEngine.awayPlayers.forEach { p ->
            if (!p.sentOff) {
                drawCircle(
                    color = Color(0xFFEF5350),
                    radius = 3.dp.toPx(),
                    center = Offset(mapX(p.position.x), mapY(p.position.z))
                )
            }
        }

        // 操控球员（黄）+ 球（亮黄）
        gameEngine.activePlayer?.let { p ->
            drawCircle(
                color = Color(0xFFFFEB3B),
                radius = 4.5f.dp.toPx(),
                center = Offset(mapX(p.position.x), mapY(p.position.z))
            )
        }
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = 2.5f.dp.toPx(),
            center = Offset(mapX(gameEngine.ballPosition.x), mapY(gameEngine.ballPosition.z))
        )
    }
}

/**
 * 比赛暂停面板（最佳球会"比赛计划"式）：角色卡 + 统计数据 + 首发阵容 + 继续/换人/重新挑战/放弃
 */
@Composable
private fun PauseMenuDialog(
    gameEngine: GameEngine,
    homeTeam: Team,
    starSlot: Int,
    onResume: () -> Unit,
    onOpenSubs: () -> Unit,
    onRestart: () -> Unit,
    onAbandon: () -> Unit
) {
    val s = gameEngine.stats
    val totalPoss = s.possessionHome + s.possessionAway
    val homePct = if (totalPoss < 5f) 50 else (s.possessionHome / totalPoss * 100).toInt()

    AlertDialog(
        onDismissRequest = onResume,
        containerColor = Color(0xFF1B5E20),
        title = {
            Text(
                text = "比赛暂停 · 比赛计划",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // ===== 招牌球星角色卡（同一球星不同球队不同职责） =====
                StarLikeness.roleCardForTeam(homeTeam)?.let { (starName, card) ->
                    val roleHere = card.byTeam[homeTeam.id] ?: card.defaultRole
                    Text(
                        text = "角色卡 · $starName（${gameEngine.homePlayers.getOrNull(starSlot)?.number ?: "-"}号）",
                        color = Color(0xFFFFD54F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "本场位置：${roleLabel(roleHere)}　　可踢：${card.roles.joinToString(" / ") { roleLabel(it) }}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "职责：${card.duty}",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "统计数据",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "控球率  $homePct% : ${100 - homePct}%",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(homePct.toFloat())
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                    Box(
                        modifier = Modifier
                            .weight((100 - homePct).toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFFEF5350))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "射门  ${s.homeShots} : ${s.awayShots}      传球  ${s.homePasses} : ${s.awayPasses}",
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "首发阵容（4-3-3）",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    FormationMiniPitch(
                        gameEngine = gameEngine,
                        starSlot = starSlot,
                        modifier = Modifier.size(width = 110.dp, height = 160.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        gameEngine.homePlayers.forEachIndexed { index, p ->
                            Text(
                                text = "${p.number}号 ${roleLabel(p.role)}" +
                                    if (index == starSlot) "  ⭐招牌球星" else "",
                                color = if (index == starSlot) Color(0xFFFFD54F) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (index == starSlot) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onOpenSubs) {
                    Text(text = "换人", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onResume) {
                    Text(text = "继续", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRestart) {
                    Text(text = "重新挑战", color = Color.White)
                }
                TextButton(onClick = onAbandon) {
                    Text(text = "放弃比赛", color = Color(0xFFEF9A9A))
                }
            }
        }
    )
}

/**
 * 首发阵容迷你球场（4-3-3 站位点位，主队进攻方向朝上，招牌球星为金色点）
 */
@Composable
private fun FormationMiniPitch(
    gameEngine: GameEngine,
    starSlot: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color(0xFF2E7D32).copy(alpha = 0.9f), size = size)
        drawRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(width = 1.dp.toPx())
        )
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.dp.toPx()
        )
        // 主队站位：z 越大越靠近对方球门 → 画在上方
        gameEngine.homePlayers.forEachIndexed { index, p ->
            val cx = (p.homePosition.x / 68f + 0.5f) * w
            val cy = (0.5f - p.homePosition.z / 105f) * h
            drawCircle(
                color = when {
                    p.isGoalkeeper -> Color(0xFFFFD54F)
                    index == starSlot -> Color(0xFFFFD54F)
                    else -> Color.White
                },
                radius = if (index == starSlot && !p.isGoalkeeper) 4.5f.dp.toPx() else 3.5f.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * 主动换人面板：选一名场上球员（= 选替补要打的位置槽）+ 一名替补 → 确认换人
 * 替补顶替所选球员的位置槽上场；门将位只能换上门将替补；每队最多 5 个名额
 */
@Composable
private fun SubstitutionDialog(
    gameEngine: GameEngine,
    onDismiss: () -> Unit
) {
    var selectedOut by remember { mutableStateOf<Player?>(null) }
    var selectedSub by remember { mutableStateOf<Player?>(null) }

    val onField = gameEngine.homePlayers.filter { !it.sentOff }
    val bench = gameEngine.substitutesFor(GameState.TeamSide.HOME)
    val subsLeft = GameEngine.MAX_SUBS - gameEngine.homeSubsUsed
    // 选了门将下场 → 只显示门将替补；选了外场球员 → 只显示外场替补
    val benchFiltered = when (val out = selectedOut) {
        null -> bench
        else -> bench.filter { it.isGoalkeeper == out.isGoalkeeper }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B5E20),
        title = {
            Text(
                text = "主动换人 · 剩余 $subsLeft 个名额",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "第一步：选下场的球员（替补将顶替他的位置）",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                onField.forEach { p ->
                    val selected = selectedOut === p
                    Text(
                        text = "${p.number}号 ${roleLabel(p.role)}" + if (p.yellowCards > 0) "  🟨×${p.yellowCards}" else "",
                        color = if (selected) Color(0xFF80CBC4) else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable {
                                selectedOut = if (selected) null else p
                                if (selectedOut == null) selectedSub = null
                            }
                            .padding(horizontal = 6.dp, vertical = 7.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "第二步：选替补上场" +
                        if (selectedOut != null) "（顶替${selectedOut!!.number}号位）" else "",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (bench.isEmpty()) {
                    Text(
                        text = if (subsLeft <= 0) "换人名额已用完" else "没有可用替补",
                        color = Color(0xFFBDBDBD),
                        fontSize = 13.sp
                    )
                } else if (benchFiltered.isEmpty()) {
                    Text(
                        text = "所选位置没有对应替补",
                        color = Color(0xFFBDBDBD),
                        fontSize = 13.sp
                    )
                }
                benchFiltered.forEach { p ->
                    val selected = selectedSub === p
                    Text(
                        text = "${p.number}号 ${roleLabel(p.role)}",
                        color = if (selected) Color(0xFF80CBC4) else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable { selectedSub = if (selected) null else p }
                            .padding(horizontal = 6.dp, vertical = 7.dp)
                    )
                }
                if (selectedOut == null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "提示：门将位只能换上门将替补（12号）",
                        color = Color(0xFFBDBDBD),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val out = selectedOut
                    val sub = selectedSub
                    if (out != null && sub != null && gameEngine.substitute(out, sub)) {
                        onDismiss()
                    }
                }
            ) {
                Text(text = "确认换人", color = Color(0xFF80CBC4), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "返回", color = Color.White)
            }
        }
    )
}

/** 位置代码 → 中文名 */
private fun roleLabel(role: String): String = when (role) {
    "GK" -> "门将"
    "CB" -> "中后卫"
    "LB" -> "左后卫"
    "RB" -> "右后卫"
    "CM" -> "中前卫"
    "LW" -> "左边锋"
    "RW" -> "右边锋"
    "ST", "FW" -> "前锋"
    else -> role
}
