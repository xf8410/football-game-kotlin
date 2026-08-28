package com.football.game.data

import com.football.game.model.Player
import com.football.game.model.Team

/**
 * 球员数据库 - 整合所有球员数据
 */
object PlayerDatabase {

    /**
     * 获取所有球员（当前 + 传奇）
     */
    fun getAllPlayers(): List<Player> {
        return FamousPlayers.ALL_FAMOUS_PLAYERS + LegendPlayers.ALL_LEGEND_PLAYERS
    }

    /**
     * 根据ID获取球员
     */
    fun getPlayer(playerId: String): Player? {
        return FamousPlayers.getPlayer(playerId) ?: LegendPlayers.getPlayer(playerId)
    }

    /**
     * 根据ID获取球员姓名
     */
    fun getPlayerName(playerId: String): String {
        return getPlayer(playerId)?.name ?: "未知球员"
    }

    /**
     * 获取球员短姓名
     */
    fun getPlayerShortName(playerId: String): String {
        val name = getPlayerName(playerId)
        return name.split(" ").lastOrNull() ?: name
    }

    /**
     * 获取球队所有球员
     */
    fun getTeamPlayers(teamId: String): List<Player> {
        return getAllPlayers().filter { it.teamId == teamId }
    }

    /**
     * 根据时代获取球员
     */
    fun getPlayersByEra(era: LegendPlayers.Era): List<Player> {
        return LegendPlayers.getPlayersByEra(era)
    }

    /**
     * 获取所有时代
     */
    fun getAllEras(): List<LegendPlayers.Era> = LegendPlayers.getAllEras()

    /**
     * 获取球队信息
     */
    fun getTeam(teamId: String): Team? = LeagueDatabase.getTeam(teamId)

    /**
     * 获取所有球队
     */
    fun getAllTeams(): List<Team> = LeagueDatabase.getAllTeams()

    /**
     * 搜索球员
     */
    fun searchPlayers(query: String): List<Player> {
        val lowerQuery = query.lowercase()
        return getAllPlayers().filter {
            it.name.lowercase().contains(lowerQuery) ||
                    it.id.lowercase().contains(lowerQuery)
        }
    }

    /**
     * 搜索球队
     */
    fun searchTeams(query: String): List<Team> = LeagueDatabase.searchTeams(query)
}