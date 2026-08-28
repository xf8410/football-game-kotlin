package com.football.game.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 存档管理器
 * 使用 SharedPreferences + JSON 存储游戏数据
 */
class SaveManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("football_save", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PLAYER_DATA = "player_data"
        private const val KEY_MATCH_HISTORY = "match_history"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_STATS = "stats"
    }

    /**
     * 玩家存档数据
     */
    data class PlayerData(
        var coins: Int = 1000,
        var gems: Int = 50,
        var level: Int = 1,
        var experience: Int = 0,
        var selectedTeamId: String = "real_madrid",
        var ownedPlayerIds: MutableList<String> = mutableListOf(),
        var formations: MutableList<String> = mutableListOf("4-4-2", "4-3-3"),
        var selectedFormation: String = "4-4-2"
    )

    /**
     * 比赛历史记录
     */
    data class MatchHistory(
        val homeTeamName: String,
        val awayTeamName: String,
        val homeScore: Int,
        val awayScore: Int,
        val isWin: Boolean,
        val isDraw: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 游戏统计
     */
    data class GameStats(
        var totalMatches: Int = 0,
        var totalWins: Int = 0,
        var totalDraws: Int = 0,
        var totalLosses: Int = 0,
        var totalGoals: Int = 0,
        var totalAssists: Int = 0,
        var totalTackles: Int = 0,
        var totalShots: Int = 0,
        var totalPasses: Int = 0,
        var cleanSheets: Int = 0,
        var hatTricks: Int = 0
    )

    /**
     * 游戏设置
     */
    data class GameSettings(
        var soundEnabled: Boolean = true,
        var musicEnabled: Boolean = true,
        var vibrationEnabled: Boolean = true,
        var difficulty: String = "NORMAL",
        var language: String = "zh"
    )

    // ==================== 玩家数据 ====================

    fun savePlayerData(data: PlayerData) {
        val json = gson.toJson(data)
        prefs.edit().putString(KEY_PLAYER_DATA, json).apply()
    }

    fun loadPlayerData(): PlayerData {
        val json = prefs.getString(KEY_PLAYER_DATA, null)
        return if (json != null) {
            gson.fromJson(json, PlayerData::class.java)
        } else {
            PlayerData()
        }
    }

    // ==================== 比赛历史 ====================

    fun addMatchHistory(match: MatchHistory) {
        val history = loadMatchHistory().toMutableList()
        history.add(0, match)  // 添加到开头
        if (history.size > 100) {
            history.removeAt(history.lastIndex)  // 保留最近100条
        }
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_MATCH_HISTORY, json).apply()
    }

    fun loadMatchHistory(): List<MatchHistory> {
        val json = prefs.getString(KEY_MATCH_HISTORY, null)
        return if (json != null) {
            val type = object : TypeToken<List<MatchHistory>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // ==================== 游戏统计 ====================

    fun saveStats(stats: GameStats) {
        val json = gson.toJson(stats)
        prefs.edit().putString(KEY_STATS, json).apply()
    }

    fun loadStats(): GameStats {
        val json = prefs.getString(KEY_STATS, null)
        return if (json != null) {
            gson.fromJson(json, GameStats::class.java)
        } else {
            GameStats()
        }
    }

    fun updateMatchResult(won: Boolean, drawn: Boolean, goalsFor: Int, goalsAgainst: Int) {
        val stats = loadStats()
        stats.totalMatches++
        when {
            won -> stats.totalWins++
            drawn -> stats.totalDraws++
            else -> stats.totalLosses++
        }
        stats.totalGoals += goalsFor
        if (goalsAgainst == 0) stats.cleanSheets++
        saveStats(stats)
    }

    // ==================== 设置 ====================

    fun saveSettings(settings: GameSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_SETTINGS, json).apply()
    }

    fun loadSettings(): GameSettings {
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            gson.fromJson(json, GameSettings::class.java)
        } else {
            GameSettings()
        }
    }

    // ==================== 通用 ====================

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun hasSaveData(): Boolean {
        return prefs.contains(KEY_PLAYER_DATA)
    }
}