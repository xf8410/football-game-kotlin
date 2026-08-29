package com.football.game.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.football.game.ui.component.TouchControls
import kotlinx.coroutines.delay

/**
 * 比赛屏幕：真实 OpenGL 3D 场景
 * 关节式球员模型 + 裁判（鸣哨/出牌）+ 铲球/犯规/任意球/点球 + 合成音效
 * 极简按键：单一大按钮三态（加速/射门/铲球）+ 传球/直塞/解围情境键 + 主动换人
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
    var currentAnnouncement by remember { mutableStateOf<GoalAnnouncement?>(null) }
    var hasBall by remember { mutableStateOf(false) }
    var actionMode by remember { mutableStateOf(GameEngine.ActionMode.SPRINT) }
    var showClearance by remember { mutableStateOf(false) }
    var showSubDialog by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }
    var glView by remember { mutableStateOf<GameGLSurfaceView?>(null) }

    val match = remember {
        com.football.game.model.Match(
            homeTeam = homeTeam ?: Team(id = "home", name = homeTeamName, shortName = "HOM"),
            awayTeam = awayTeam ?: Team(id = "away", name = awayTeamName, shortName = "AWY")
        )
    }

    // 十一名球员（4-3-3，下标 9 为中锋/招牌球星位）+ 各 5 名替补
    val gameEngine = remember(match) {
        GameEngine(
            match = match,
            homePlayers = GameEngine.createTeamPlayers(match.homeTeam, GameState.TeamSide.HOME),
            awayPlayers = GameEngine.createTeamPlayers(match.awayTeam, GameState.TeamSide.AWAY)
        ).apply {
            activePlayer = homePlayers.getOrNull(9)
            activePlayer?.isActive = true
            activePlayer?.isPlayerControlled = true
        }
    }

    // 球员外观（球衣配色 + 肤色/发型，招牌球星套用球队标志性特征）
    val homeLooks = remember(match) { StarLikeness.lookForTeam(match.homeTeam, 11) }
    val awayLooks = remember(match) { StarLikeness.lookForTeam(match.awayTeam, 11) }

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

    // 引擎实时模拟（~60fps）+ 推送渲染数据
    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            if (!isPaused && !isFinished) {
                gameEngine.update(0.016f)
                glView?.updateGameData(
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
            }
        }
    }

    // 比赛时钟（50 倍加速：+5s 游戏时间 / 100ms）
    LaunchedEffect(isPaused, isFinished) {
        while (matchTime < 2700f) {
            delay(100L)
            if (!isPaused && !isFinished) {
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
        // OpenGL 3D 场景
        AndroidView(
            factory = { ctx ->
                GameGLSurfaceView(ctx).also { view -> glView = view }
            },
            modifier = Modifier.fillMaxSize()
        )

        GoalAnnouncementUI(
            announcement = currentAnnouncement,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        Scoreboard(
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            homeScore = homeScore,
            awayScore = awayScore,
            matchTime = matchTime,
            currentHalf = currentHalf,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )

        // 换人按钮（暂停比赛并打开换人面板）
        Button(
            onClick = {
                isPaused = true
                showSubDialog = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(text = "换人", color = Color.White, fontSize = 13.sp)
        }

        // 犯规/牌/定位球横幅
        bannerText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
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

        TouchControls(
            gameEngine = gameEngine,
            actionMode = actionMode,
            hasBall = hasBall,
            showClearance = showClearance,
            modifier = Modifier.fillMaxSize()
        )

        // 主动换人面板（打开时比赛暂停）
        if (showSubDialog) {
            SubstitutionDialog(
                gameEngine = gameEngine,
                onDismiss = {
                    showSubDialog = false
                    isPaused = false
                }
            )
        }
    }
}

/**
 * 主动换人面板：选一名场上球员 + 一名替补 → 确认换人
 * 替补顶替同槽位上场（继承位置/阵型落位），门将不可换下，每队最多 5 个名额
 */
@Composable
private fun SubstitutionDialog(
    gameEngine: GameEngine,
    onDismiss: () -> Unit
) {
    var selectedOut by remember { mutableStateOf<Player?>(null) }
    var selectedSub by remember { mutableStateOf<Player?>(null) }

    val onField = gameEngine.homePlayers.filter { !it.isGoalkeeper && !it.sentOff }
    val bench = gameEngine.substitutesFor(GameState.TeamSide.HOME)
    val subsLeft = GameEngine.MAX_SUBS - gameEngine.homeSubsUsed

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
                    text = "选择下场的场上球员：",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                onField.forEach { p ->
                    val selected = selectedOut === p
                    Text(
                        text = "${p.number}号 ${p.role}" + if (p.yellowCards > 0) "  🟨×${p.yellowCards}" else "",
                        color = if (selected) Color(0xFF80CBC4) else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF2E7D32) else Color.Transparent)
                            .clickable { selectedOut = if (selected) null else p }
                            .padding(horizontal = 6.dp, vertical = 7.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "选择替补上场：",
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
                }
                bench.forEach { p ->
                    val selected = selectedSub === p
                    Text(
                        text = "${p.number}号 ${p.role}",
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
                Text(text = "返回比赛", color = Color.White)
            }
        }
    )
}

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
    Column(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(homeTeamName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(awayTeamName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$homeScore", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Text("-", color = Color.Gray, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Text("$awayScore", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        }
        val minutes = (matchTime / 60).toInt()
        val halfText = if (currentHalf == 1) "上半场" else "下半场"
        Text(text = "$halfText ${String.format("%02d:%02d", minutes, 0)}", color = Color.Yellow, fontSize = 14.sp)
    }
}
