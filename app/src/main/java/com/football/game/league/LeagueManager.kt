package com.football.game.league

import com.football.game.model.League
import com.football.game.model.Team
import kotlin.math.exp
import kotlin.random.Random

/**
 * 联赛模拟引擎
 * 负责：双循环赛程生成、比赛比分模拟、积分榜维护、冠军判定
 */
class LeagueManager(val league: League) {

    /** 联赛中的球队（可变副本，统计随比赛更新） */
    val teams: List<Team> = league.teams.map { it.copy() }

    /** 双循环赛程：schedule[roundIndex] = 本轮所有比赛 */
    val schedule: List<List<Fixture>> = generateSchedule()

    /** 联赛总轮数（双循环：2 * (n - 1)） */
    val totalRounds: Int = 2 * (teams.size - 1)

    /** 已完成的轮数（0 表示尚未开始） */
    var completedRounds: Int = 0
        private set

    /** 赛季是否已结束 */
    val isFinished: Boolean get() = completedRounds >= totalRounds

    /** 下一轮（待踢）的比赛 */
    val nextFixtures: List<Fixture>
        get() = schedule.getOrNull(completedRounds) ?: emptyList()

    /**
     * 积分榜：按 积分 → 净胜球 → 进球数 → 队名 排序
     */
    fun standings(): List<Team> {
        return teams.sortedWith(
            compareByDescending<Team> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenBy { it.name }
        )
    }

    /**
     * 联赛冠军（赛季结束后）
     */
    fun champion(): Team? {
        return if (isFinished) standings().firstOrNull() else null
    }

    /**
     * 模拟并完成当前轮的所有比赛，返回本轮已完成的比赛
     */
    fun playRound(rng: Random = Random.Default): List<Fixture> {
        if (isFinished) return emptyList()
        val fixtures = nextFixtures
        for (fixture in fixtures) {
            val (homeGoals, awayGoals) = simulateScore(fixture.home, fixture.away, rng)
            fixture.homeGoals = homeGoals
            fixture.awayGoals = awayGoals
            fixture.played = true
            fixture.home.recordResult(homeGoals, awayGoals)
            fixture.away.recordResult(awayGoals, homeGoals)
        }
        completedRounds++
        return fixtures
    }

    /**
     * 重置整个赛季
     */
    fun reset() {
        completedRounds = 0
        for (team in teams) {
            team.gamesPlayed = 0
            team.wins = 0
            team.draws = 0
            team.losses = 0
            team.goalsFor = 0
            team.goalsAgainst = 0
        }
        for (round in schedule) {
            for (fixture in round) {
                fixture.played = false
                fixture.homeGoals = 0
                fixture.awayGoals = 0
            }
        }
    }

    // ==================== 内部实现 ====================

    /**
     * 圆桌法生成双循环赛程
     * 首回合 teams[0] 固定，其余队伍轮转；次回合主客对调
     */
    private fun generateSchedule(): List<List<Fixture>> {
        val n = teams.size
        if (n < 2) return emptyList()

        val rotation = teams.drop(1).toMutableList()
        val firstHalf = mutableListOf<List<Fixture>>()

        for (round in 0 until n - 1) {
            val arr = listOf(teams[0]) + rotation
            val fixtures = mutableListOf<Fixture>()
            for (i in 0 until n / 2) {
                val a = arr[i]
                val b = arr[n - 1 - i]
                // 交替主客，均衡各队主客场次数
                val (home, away) = if ((round + i) % 2 == 0) a to b else b to a
                fixtures.add(Fixture(home = home, away = away))
            }
            firstHalf.add(fixtures)
            // 左旋一位
            rotation.add(rotation.removeAt(0))
        }

        // 次回合：主客对调
        val secondHalf = firstHalf.map { round -> round.map { Fixture(home = it.away, away = it.home) } }
        return firstHalf + secondHalf
    }

    /**
     * 基于球队攻防属性模拟比分（主场有优势）
     */
    private fun simulateScore(home: Team, away: Team, rng: Random): Pair<Int, Int> {
        val homeAttack = home.attack + 3f  // 主场优势
        val homeExpected = expectedGoals(homeAttack, away.defense, away.goalkeeper)
        val awayExpected = expectedGoals(away.attack.toFloat(), home.defense, home.goalkeeper)

        val homeGoals = poisson(homeExpected, rng)
        val awayGoals = poisson(awayExpected, rng)
        return homeGoals to awayGoals
    }

    /**
     * 期望进球：攻防差映射到泊松 λ，典型区间约 0.2 ~ 3.8
     */
    private fun expectedGoals(attack: Float, defense: Int, goalkeeper: Int): Float {
        val defRating = (defense + goalkeeper) / 2f
        val lambda = 1.35f + (attack - defRating) * 0.06f
        return lambda.coerceIn(0.2f, 3.8f)
    }

    /**
     * 泊松分布采样（Knuth 算法），k 上限 15 防止极端值
     */
    private fun poisson(lambda: Float, rng: Random): Int {
        val l = exp(-lambda)
        var k = 0
        var p = 1f
        do {
            k++
            p *= rng.nextFloat()
        } while (p > l && k < 15)
        return k - 1
    }
}

/**
 * 赛程中的一场比赛
 */
data class Fixture(
    val home: Team,
    val away: Team,
    var played: Boolean = false,
    var homeGoals: Int = 0,
    var awayGoals: Int = 0
) {
    /** "RMA 2 - 1 BAR" 形式的比分文本 */
    val scoreLine: String
        get() = if (played) "$homeGoals - $awayGoals" else "vs"
}
