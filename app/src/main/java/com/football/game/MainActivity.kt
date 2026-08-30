package com.football.game

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.football.game.model.League
import com.football.game.model.Player
import com.football.game.model.Team
import com.football.game.ui.screen.EraSelectScreen
import com.football.game.ui.screen.LeagueScreen
import com.football.game.ui.screen.MainMenuScreen
import com.football.game.ui.screen.MatchScreen
import com.football.game.ui.screen.SettingsScreen
import com.football.game.ui.screen.TeamSelectScreen
import com.football.game.ui.theme.FootballGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 崩溃报告优先：上次启动崩溃过 → 用"原生 View"显示报告页（不依赖 Compose，
        // 即使 Compose/主题本身崩溃也能显示出来），用户截图即可定位
        val previousCrash = CrashReporter.readPreviousCrash(this)
        if (previousCrash != null) {
            showCrashReport(previousCrash)
            return
        }
        CrashReporter.install(this)

        enableEdgeToEdge()
        setContent {
            FootballGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FootballGameApp()
                }
            }
        }
    }

    /**
     * 原生 View 崩溃报告页：完整堆栈 + 复制按钮 + 清除并重启按钮
     */
    private fun showCrashReport(report: String) {
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = "应用上次启动时崩溃了\n\n崩溃信息如下，请截图发给开发者，\n然后点\"清除并重新启动\"。"
            textSize = 16f
            setPadding(0, 0, 0, pad)
        }

        val body = TextView(this).apply {
            text = report
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }

        val copyBtn = Button(this).apply {
            text = "复制崩溃信息"
            setOnClickListener {
                try {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("crash", report))
                    Toast.makeText(this@MainActivity, "已复制", Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) {
                }
            }
        }

        val restartBtn = Button(this).apply {
            text = "清除并重新启动"
            setOnClickListener {
                CrashReporter.clear(this@MainActivity)
                recreate()
            }
        }

        col.addView(title)
        col.addView(body)
        col.addView(
            copyBtn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad }
        )
        col.addView(
            restartBtn,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = pad / 2 }
        )
        scroll.addView(col)
        setContentView(scroll)
    }
}

/**
 * 足球游戏主应用
 */
@Composable
fun FootballGameApp() {
    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }
    var selectedHomeTeam by remember { mutableStateOf<Team?>(null) }
    var selectedAwayTeam by remember { mutableStateOf<Team?>(null) }
    var selectedLeague by remember { mutableStateOf<League?>(null) }
    var selectedLegendPlayer by remember { mutableStateOf<Player?>(null) }

    when (currentScreen) {
        Screen.MAIN_MENU -> {
            MainMenuScreen(
                onQuickMatch = {
                    currentScreen = Screen.TEAM_SELECT_HOME
                },
                onLeagueMode = {
                    currentScreen = Screen.LEAGUE
                },
                onCupMode = {
                    currentScreen = Screen.ERA_SELECT  // 传奇模式
                },
                onSettings = {
                    currentScreen = Screen.SETTINGS
                },
                onExit = {
                    // TODO: 退出应用
                }
            )
        }

        Screen.TEAM_SELECT_HOME -> {
            TeamSelectScreen(
                title = "选择主队",
                onTeamSelected = { team ->
                    selectedHomeTeam = team
                    currentScreen = Screen.TEAM_SELECT_AWAY
                },
                onBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.TEAM_SELECT_AWAY -> {
            TeamSelectScreen(
                title = "选择客队",
                onTeamSelected = { team ->
                    selectedAwayTeam = team
                    currentScreen = Screen.MATCH
                },
                onBack = {
                    currentScreen = Screen.TEAM_SELECT_HOME
                }
            )
        }

        Screen.MATCH -> {
            val homeTeam = selectedHomeTeam
            val awayTeam = selectedAwayTeam

            if (homeTeam != null && awayTeam != null) {
                MatchScreen(
                    homeTeamName = homeTeam.name,
                    awayTeamName = awayTeam.name,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    onMatchEnd = {
                        currentScreen = Screen.MAIN_MENU
                    }
                )
            } else {
                currentScreen = Screen.MAIN_MENU
            }
        }

        Screen.LEAGUE -> {
            LeagueScreen(
                onTeamSelected = { league, team ->
                    selectedLeague = league
                    selectedHomeTeam = team
                    val awayTeamRandom = league.teams.filter { it.id != team.id }.randomOrNull()
                    selectedAwayTeam = awayTeamRandom
                    currentScreen = Screen.MATCH
                },
                onBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.ERA_SELECT -> {
            EraSelectScreen(
                onPlayerSelected = { player ->
                    selectedLegendPlayer = player
                    // 根据传奇球员选择对应的球队
                    selectedHomeTeam = Team(
                        id = player.teamId,
                        name = player.teamName,
                        shortName = player.teamName.take(3)
                    )
                    // AI选择对手
                    val allTeams = com.football.game.data.LeagueDatabase.getAllTeams()
                    selectedAwayTeam = allTeams.filter { it.id != player.teamId }.randomOrNull()
                    currentScreen = Screen.MATCH
                },
                onBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.SETTINGS -> {
            SettingsScreen(
                onBack = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }

        Screen.CUP -> {
            // TODO: 杯赛页面
            currentScreen = Screen.MAIN_MENU
        }
    }
}

/**
 * 屏幕枚举
 */
enum class Screen {
    MAIN_MENU,
    TEAM_SELECT_HOME,
    TEAM_SELECT_AWAY,
    MATCH,
    SETTINGS,
    LEAGUE,
    CUP,
    ERA_SELECT
}

@Preview(showBackground = true)
@Composable
fun FootballGameAppPreview() {
    FootballGameTheme {
        FootballGameApp()
    }
}
