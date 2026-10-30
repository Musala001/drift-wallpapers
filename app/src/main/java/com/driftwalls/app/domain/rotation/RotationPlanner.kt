package com.driftwalls.app.domain.rotation

import kotlin.random.Random

enum class RotationOrder(val label: String) {
    SHUFFLE("Shuffle"),
    IN_ORDER("In order"),
}

/** Chooses the next wallpaper. Pure, so the rotation rules are unit-tested without Android. */
object RotationPlanner {
    /** Shuffle never avoids more than this many recent wallpapers, however large the pool. */
    const val MAX_AVOID = 50

    /**
     * @param candidates wallpaper ids in the source's own order.
     * @param history ids already shown, newest first.
     * @return the next id, or null when there are no candidates.
     */
    fun next(
        candidates: List<String>,
        history: List<String>,
        order: RotationOrder,
        random: Random = Random,
    ): String? {
        if (candidates.isEmpty()) return null
        return when (order) {
            RotationOrder.IN_ORDER -> {
                // Continue after the newest wallpaper that is still in the list; start over otherwise.
                val last = history.firstOrNull { it in candidates }
                candidates[(candidates.indexOf(last) + 1).mod(candidates.size)]
            }
            RotationOrder.SHUFFLE -> {
                // Skip the most recent half of the pool so repeats are spread out without the
                // order becoming a predictable cycle.
                val avoid = history.asSequence().distinct().take(minOf(candidates.size / 2, MAX_AVOID)).toSet()
                candidates.filterNot { it in avoid }.ifEmpty { candidates }.random(random)
            }
        }
    }
}
