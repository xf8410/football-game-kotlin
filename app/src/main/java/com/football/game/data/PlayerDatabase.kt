package com.football.game.data

import com.football.game.model.Player
import com.football.game.model.Team

/**
 * 球员数据库
 * 整合所有球员数据
 */
object PlayerDatabase {

    /**
     * 获取所有球员
     */
    fun getAllPlayers(): List<Player> {
        return FamousPlayers.ALL_FAMOUS_PLAYERS
    }

    /**
     * 根据ID获取球员
     */
    fun getPlayer(playerId: String): Player? {
        return FamousPlayers.getPlayer(playerId)
    }

    /**
     * 根据ID获取球员姓名
     */
    fun getPlayerName(playerId: String): String {
        return getPlayer(playerId)?.name ?: "未知球员"
    }

    /**
     * 获取球员短姓名（姓）
     */
    fun getPlayerShortName(playerId: String): String {
        val name = getPlayerName(playerId)
        return name.split(" ").lastOrNull() ?: name
    }

    /**
     * 获取球队所有球员
     */
    fun getTeamPlayers(teamId: String): List<Player> {
        return FamousPlayers.getPlayersByTeam(teamId)
    }

    /**
     * 获取球队信息
     */
    fun getTeam(teamId: String): Team? {
        return LeagueDatabase.getTeam(teamId)
    }

    /**
     * 获取所有球队
     */
    fun getAllTeams(): List<Team> {
        return LeagueDatabase.getAllTeams()
    }

    /**
     * 获取联赛信息
     */
    fun getLeague(leagueId: String) = LeagueDatabase.getLeague(leagueId)

    /**
     * 根据联赛名称获取球队
     */
    fun getTeamsByLeague(leagueName: String) = LeagueDatabase.getTeamsByLeague(leagueName)

    /**
     * 搜索球员
     */
    fun searchPlayers(query: String) = FamousPlayers.searchPlayers(query)

    /**
     * 搜索球队
     */
    fun searchTeams(query: String) = LeagueDatabase.searchTeams(query)
}