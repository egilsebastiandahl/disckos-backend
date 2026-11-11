package no.disckos.backend.application.rating

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.math.sqrt

/**
 * Tuning run for [RatingRules.PARAMS]. Slow (a grid of settings, each refitted card by card),
 * so it only runs on request:
 *
 *     RATING_BACKTEST=1 ./gradlew test --tests '*RatingBacktest*' -i
 *
 * Prints the baselines and the best settings by mean absolute error per round.
 */
@EnabledIfEnvironmentVariable(named = "RATING_BACKTEST", matches = "1")
class RatingBacktest {

    @Test
    fun `grid search`() {
        val inf = Double.POSITIVE_INFINITY
        val decays = listOf(30.0, 60.0, 90.0, 180.0).map { it to inf } +
            listOf(5.0, 10.0, 15.0, 25.0).map { inf to it } +
            listOf(90.0, 180.0).flatMap { d -> listOf(10.0, 20.0).map { d to it } }

        for (byLayout in listOf(true, false)) {
            val rounds = RatingBacktestData.load(byLayout).rounds
            println("\n=== One difficulty per ${if (byLayout) "layout" else "course"} (${rounds.size} rounds) ===")
            println("baseline player average          ${RatingBacktestData.evaluate(rounds, RatingBacktestData.playerAverage)}")
            println("baseline player last-10 average  ${RatingBacktestData.evaluate(rounds, RatingBacktestData.playerRecentAverage)}")
            println("baseline player course average   ${RatingBacktestData.evaluate(rounds, RatingBacktestData.playerCourseAverage)}")
            println("RatingRules.PARAMS               ${RatingBacktestData.evaluate(rounds, RatingBacktestData.model(RatingRules.PARAMS))}")
            if (!byLayout) continue

            val results = buildList {
                for ((days, roundsHalfLife) in decays)
                    for (playerPrior in listOf(0.05, 0.1, 0.25, 0.5))
                        for (coursePrior in listOf(0.1, 0.25, 0.5, 1.0))
                            for (outlier in listOf(null, 2.5)) {
                                val params = RatingParams(days, roundsHalfLife, playerPrior, coursePrior, outlier)
                                add(params to RatingBacktestData.evaluate(rounds, RatingBacktestData.model(params)))
                            }
            }.sortedBy { it.second.mae }
            results.take(20).forEach { (p, r) -> println("  $r  <- $p") }

            println("\nBest per decay setting:")
            results.groupBy { it.first.halfLifeDays to it.first.halfLifeRounds }.forEach { (decay, rs) ->
                val (p, r) = rs.first()
                println("  days %-8s rounds %-8s  $r  playerPrior=${p.playerPrior} coursePrior=${p.coursePrior} outlier=${p.outlierSd}".format(decay.first, decay.second))
            }
            fun bestBy(label: String, key: (RatingParams) -> Any?) =
                println("  best by $label: " + results.groupBy { key(it.first) }.mapValues { (_, v) -> "%.3f".format(v.first().second.mae) })
            bestBy("playerPrior") { it.playerPrior }
            bestBy("coursePrior") { it.coursePrior }
            bestBy("outlierSd") { it.outlierSd }
        }
    }

    /**
     * Sanity check against UDisc's own round ratings, which adjust for the course from UDisc's
     * global data. Course-adjusted scores should track them better than raw scores do.
     */
    @Test
    fun `round ratings track UDisc's`() {
        val data = RatingBacktestData.load(byLayout = true)
        val fit = RatingModel.fit(data.rounds)
        val pairs = data.rounds.mapNotNull { r ->
            val udisc = data.udiscRating[r.roundId]?.get(r.playerId) ?: return@mapNotNull null
            Triple(r.toParPer18, RatingRules.rating(fit.adjustedToParPer18(r)), udisc.toDouble())
        }
        println("\nRounds with a UDisc rating: ${pairs.size}")
        println("corr(raw to par per 18, UDisc)   = %.3f".format(corr(pairs.map { it.first }, pairs.map { it.third })))
        println("corr(Disckos round rating, UDisc) = %.3f".format(corr(pairs.map { it.second }, pairs.map { it.third })))
        println("\nFitted course difficulty (strokes per 18 over par-fair):")
        fit.difficulty.entries.sortedBy { it.value }.forEach { (c, d) ->
            println("  %+5.1f  %3d rounds  %s | %s".format(d, data.rounds.count { it.course == c }, data.names[c.locationId], c.layout))
        }
        println("\nPlayers with 5+ rounds:")
        data.rounds.groupBy { it.playerId }.filter { it.value.size >= 5 }.forEach { (p, rs) ->
            val skill = fit.skill.getValue(p)
            println(
                "  rating %4.0f  skill %+5.1f  raw avg %+5.1f  rounds %3d  %s"
                    .format(RatingRules.rating(skill), skill, rs.map { it.toParPer18 }.average(), rs.size, data.names[p])
            )
        }
    }

    private fun corr(a: List<Double>, b: List<Double>): Double {
        val ma = a.average()
        val mb = b.average()
        val cov = a.indices.sumOf { (a[it] - ma) * (b[it] - mb) }
        val va = a.sumOf { (it - ma) * (it - ma) }
        val vb = b.sumOf { (it - mb) * (it - mb) }
        return cov / sqrt(va * vb)
    }
}
