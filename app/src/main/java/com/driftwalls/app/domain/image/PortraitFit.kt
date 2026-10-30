package com.driftwalls.app.domain.image

import com.driftwalls.app.domain.model.Artwork

/**
 * Prefers artworks that suit a portrait phone screen. Everything else is still shown and
 * center-cropped later, except extreme panoramas where a crop would keep only a sliver.
 */
object PortraitFit {
    /** Width / height at or below this counts as portrait-friendly (allows near-square works). */
    const val PORTRAIT_MAX_RATIO = 1.1f

    /** Wider than this (e.g. handscrolls) and a phone crop keeps under ~20% of the image. */
    const val PANORAMA_MIN_RATIO = 2.2f

    fun ratio(artwork: Artwork): Float? {
        val w = artwork.width ?: return null
        val h = artwork.height ?: return null
        return if (w > 0 && h > 0) w.toFloat() / h else null
    }

    /** Drops panoramas and moves portrait-friendly works first, keeping relative order otherwise. */
    fun rank(items: List<Artwork>): List<Artwork> {
        val kept = items.filter { (ratio(it) ?: 1f) < PANORAMA_MIN_RATIO }
        // Unknown size sorts with the portrait group so the Met's results aren't all pushed back.
        return kept.sortedBy { (ratio(it) ?: 0f) > PORTRAIT_MAX_RATIO }
    }
}
