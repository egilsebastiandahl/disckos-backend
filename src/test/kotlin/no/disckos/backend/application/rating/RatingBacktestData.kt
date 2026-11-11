package no.disckos.backend.application.rating

import no.disckos.backend.application.admin.udiscimport.UdiscCsvParser
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs

/**
 * Real rounds for tuning and checking the rating model: the full UDisc history export in
 * src/test/resources/udisc-exports (118 cards, 2025-05 to 2026-10, about 25 courses).
 * Doubles and unfinished rounds are left out, like the importer and ranks do.
 */
object RatingBacktestData {

    private const val FILE = "/udisc-exports/2026-11-09-full-history.csv"
    private val OSLO = ZoneId.of("Europe/Oslo")

    data class Loaded(
        val rounds: List<RatingRound>,
        /** UDisc's own RoundRating per round, where the export has one. */
        val udiscRating: Map<UUID, Map<UUID, Int>>,
        /** Player and course names by id, for printing. */
        val names: Map<UUID, String>
    )

    /** @param byLayout learn one difficulty per UDisc layout instead of one per course. */
    fun load(byLayout: Boolean): Loaded {
        val csv = requireNotNull(javaClass.getResource(FILE)) { "missing fixture $FILE" }.readText()
        val names = mutableMapOf<UUID, String>()
        fun named(name: String): UUID = id(name).also { names[it] = name }
        val rounds = UdiscCsvParser.parse(csv).flatMap { card ->
            val started = card.startedAt ?: return@flatMap emptyList()
            if (card.pars.any { it == null }) return@flatMap emptyList()
            val par = card.pars.sumOf { it!! }
            val course = RatingCourse(named(card.courseName), if (byLayout) card.layoutName else null)
            card.players
                .filter { "+" !in it.name && it.throws.none { t -> t == null } }
                .map { p ->
                    RatingRound(
                        playerId = named(p.name.trim()),
                        roundId = id("${card.courseName}|${card.rawDate}"),
                        course = course,
                        playedAt = started.atZone(OSLO).toOffsetDateTime(),
                        toPar = p.throws.sumOf { it!! } - par,
                        holes = card.holeCount
                    )
                }
        }
        return Loaded(rounds, udiscRatings(csv), names)
    }

    fun id(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())

    /** roundId -> playerId -> UDisc RoundRating. Plain split is fine: no field in this file contains a comma. */
    private fun udiscRatings(csv: String): Map<UUID, Map<UUID, Int>> {
        val lines = csv.lines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        val (name, course, date, rating) = listOf("PlayerName", "CourseName", "StartDate", "RoundRating").map(header::indexOf)
        return lines.drop(1).map { it.split(",") }
            .filter { it[name] != "Par" && it[rating].isNotBlank() }
            .groupBy({ id("${it[course]}|${it[date]}") }, { id(it[name].trim()) to it[rating].toInt() })
            .mapValues { (_, v) -> v.toMap() }
    }

    // ---- Evaluation ---------------------------------------------------------------------

    /** What a predictor learned from some rounds. */
    class Trained(
        /** Expected score to par per 18 for a new round. */
        val predict: (RatingRound) -> Double,
        /** The player's current level, strokes per 18; used to measure how much it jumps per round. */
        val level: (UUID) -> Double
    )

    fun interface Predictor {
        fun train(rounds: List<RatingRound>): Trained
    }

    data class Result(
        /** Mean absolute error per round, strokes. */
        val mae: Double,
        /** Mean error: positive = predicted worse than the player actually played. */
        val bias: Double,
        /** Mean change in the player's level from one round to the next, in rating points. */
        val jump: Double,
        val predictions: Int
    ) {
        override fun toString() = "mae %.3f  bias %+.2f  jump %4.1f pts  n=%d".format(mae, bias, jump, predictions)
    }

    /**
     * Walks forward card by card. Every round is predicted from the rounds played strictly
     * before it, for players who already have [minHistory] rounds. Errors are in strokes for
     * the round as played (a 9-hole round's error is not doubled).
     */
    fun evaluate(
        rounds: List<RatingRound>,
        predictor: Predictor,
        minHistory: Int = 3,
        warmupCards: Int = 15
    ): Result {
        val times = rounds.map { it.playedAt }.distinct().sorted()
        var absSum = 0.0
        var sum = 0.0
        var count = 0
        val lastLevel = mutableMapOf<UUID, Double>()
        var jumpSum = 0.0
        var jumps = 0
        for (t in times.drop(warmupCards)) {
            val train = rounds.filter { it.playedAt < t }
            val history = train.groupingBy { it.playerId }.eachCount()
            val targets = rounds.filter { it.playedAt == t && (history[it.playerId] ?: 0) >= minHistory }
            if (targets.isEmpty()) continue
            val trained = predictor.train(train)
            for (r in targets) {
                val error = (trained.predict(r) - r.toParPer18) * r.holes / 18.0
                absSum += abs(error)
                sum += error
                count++
                val level = trained.level(r.playerId)
                lastLevel[r.playerId]?.let { jumpSum += abs(level - it) * RatingRules.POINTS_PER_STROKE; jumps++ }
                lastLevel[r.playerId] = level
            }
        }
        return Result(absSum / count, sum / count, jumpSum / jumps, count)
    }

    fun model(params: RatingParams) = Predictor { train ->
        val fit = RatingModel.fit(train, emptyMap(), params)
        Trained(
            predict = { r -> fit.expectedToParPer18(r.playerId, r.course) },
            level = { p -> fit.skill.getValue(p) }
        )
    }

    private fun averages(train: List<RatingRound>, pick: (List<RatingRound>) -> List<RatingRound>) =
        train.groupBy { it.playerId }.mapValues { (_, rs) -> pick(rs.sortedBy { it.playedAt }).map { it.toParPer18 }.average() }

    /** The player's plain average to par per 18, ignoring which course it was. */
    val playerAverage = Predictor { train ->
        val avg = averages(train) { it }
        Trained({ r -> avg.getValue(r.playerId) }, avg::getValue)
    }

    /** The player's average over their last 10 rounds. */
    val playerRecentAverage = Predictor { train ->
        val avg = averages(train) { it.takeLast(10) }
        Trained({ r -> avg.getValue(r.playerId) }, avg::getValue)
    }

    /** The player's average on this course, or their overall average on a new course. */
    val playerCourseAverage = Predictor { train ->
        val overall = averages(train) { it }
        val onCourse = train.groupBy { it.playerId to it.course }.mapValues { (_, rs) -> rs.map { it.toParPer18 }.average() }
        Trained({ r -> onCourse[r.playerId to r.course] ?: overall.getValue(r.playerId) }, overall::getValue)
    }
}
