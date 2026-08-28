package com.football.game

import android.os.Bundle
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
import com.football.game.ui.screen.MainMenuScreen
import com.football.game.ui.screen.MatchScreen
import com.football.game.ui.theme.FootballGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

/**
 * 足球游戏主应用
 */
@Composable
fun FootballGameApp() {
    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }
    var selectedHomeTeam by remember { mutableStateOf("红队") }
    var selectedAwayTeam by remember { mutableStateOf("蓝队") }

    when (currentScreen) {
        Screen.MAIN_MENU -> {
            MainMenuScreen(
                onQuickMatch = {
                    currentScreen = Screen.MATCH
                },
                onLeagueMode = {
                    // TODO: 实现联赛模式
                    currentScreen = Screen.MATCH
                },
                onCupMode = {
                    // TODO: 实现杯赛模式
                    currentScreen = Screen.MATCH
                },
                onSettings = {
                    // TODO: 实现设置页面
                },
                onExit = {
                    // TODO: 退出应用
                }
            )
        }
        Screen.MATCH -> {
            MatchScreen(
                homeTeamName = selectedHomeTeam,
                awayTeamName = selectedAwayTeam,
                onMatchEnd = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }
        Screen.SETTINGS -> {
            // TODO: 设置页面
        }
        Screen.LEAGUE -> {
            // TODO: 联赛页面
        }
        Screen.CUP -> {
            // TODO: 杯赛页面
        }
    }
}

/**
 * 屏幕枚举
 */
enum class Screen {
    MAIN_MENU,
    MATCH,
    SETTINGS,
    LEAGUE,
    CUP
}

@Preview(showBackground = true)
@Composable
fun FootballGameAppPreview() {
    FootballGameTheme {
        FootballGameApp()
    }
}