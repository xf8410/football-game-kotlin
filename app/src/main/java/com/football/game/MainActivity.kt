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