package no.disckos.backend.application.rank

/**
 * One step on the rank ladder.
 *
 * [maxToParPer18] is the threshold: a player whose (offset-adjusted) average is at or below it
 * reaches this tier. Null only for the bottom tier, which everyone placed reaches.
 * [group] is a stable key the frontend uses for the badge's colour and icon.
 */
data class RankTier(
    val index: Int,
    val name: String,
    val group: String,
    val maxToParPer18: Double?
)

/**
 * The ladder and the rules for climbing it. Everything about ranks is tuned here.
 *
 * Thresholds are average strokes to par per 18 holes and were set (2026-11-08) from the club's
 * rounds at the time: most members played +14 to +39, the best around par. Steps are wider at
 * the bottom, where beginners improve fast, and narrow towards the top, where each stroke is hard.
 */
object RankLadder {
    /** Complete rounds on a course before a player gets a rank there. */
    const val PLACEMENT_ROUNDS = 3

    /** Only the most recent rounds on a course are considered... */
    const val WINDOW_ROUNDS = 10

    /** ...and of those, the best ones are averaged. Bad days barely hurt, grinding pays off. */
    const val COUNTED_ROUNDS = 5

    /** A rank is shown greyed out after this long without a round on the course. */
    const val INACTIVE_AFTER_DAYS = 90L

    val tiers: List<RankTier> = listOf(
        RankTier(0, "Tree Hugger I", "tree-hugger", null),
        RankTier(1, "Tree Hugger II", "tree-hugger", 36.0),
        RankTier(2, "Tree Hugger III", "tree-hugger", 32.0),
        RankTier(3, "Tree Hugger IV", "tree-hugger", 28.0),
        RankTier(4, "Fairway Finder I", "fairway-finder", 25.0),
        RankTier(5, "Fairway Finder II", "fairway-finder", 22.0),
        RankTier(6, "Fairway Finder III", "fairway-finder", 19.0),
        RankTier(7, "Circle Crusher I", "circle-crusher", 16.0),
        RankTier(8, "Circle Crusher II", "circle-crusher", 13.0),
        RankTier(9, "Circle Crusher III", "circle-crusher", 11.0),
        RankTier(10, "Chain Master I", "chain-master", 9.0),
        RankTier(11, "Chain Master II", "chain-master", 7.0),
        RankTier(12, "Chain Master III", "chain-master", 5.0),
        RankTier(13, "Eagle Eye I", "eagle-eye", 3.0),
        RankTier(14, "Eagle Eye II", "eagle-eye", 1.0),
        RankTier(15, "Albatross", "albatross", -1.0),
        RankTier(16, "Ace Master", "ace-master", -4.0),
        RankTier(17, "Disckos Elite", "disckos-elite", -7.0),
    )

    /** Guards integer thresholds against floating-point noise in averages like 23/5. */
    private const val EPSILON = 1e-9

    /** The highest tier whose threshold [adjustedToParPer18] meets. */
    fun tierFor(adjustedToParPer18: Double): RankTier =
        tiers.last { it.maxToParPer18 == null || adjustedToParPer18 <= it.maxToParPer18 + EPSILON }

    fun next(tier: RankTier): RankTier? = tiers.getOrNull(tier.index + 1)
}
