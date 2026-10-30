package com.driftwalls.app.domain.model

/** Museums we pull public-domain artworks from. [key] prefixes wallpaper ids, e.g. "aic:27992". */
enum class Museum(val key: String, val displayName: String) {
    MET("met", "The Metropolitan Museum of Art"),
    AIC("aic", "The Art Institute of Chicago"),
    ;

    companion object {
        fun fromKey(key: String): Museum? = entries.firstOrNull { it.key == key }
    }
}

/** A public-domain museum artwork. Only mappers that have checked the public-domain flag create these. */
data class Artwork(
    val museum: Museum,
    val sourceId: String,
    val title: String,
    val artist: String?,
    val date: String?,
    /** Grid-sized image (~843 px wide). */
    val thumbnailUrl: String,
    /** Largest image we use for wallpapers. */
    val imageUrl: String,
    /** Original pixel size when the source reports it (AIC does, the Met does not). */
    val width: Int? = null,
    val height: Int? = null,
    val altText: String? = null,
    /** Public page for the object on the museum's website. */
    val webUrl: String? = null,
) {
    val id: String get() = wallpaperId(museum, sourceId)

    /** e.g. "Vincent van Gogh. The Bedroom, 1889. The Art Institute of Chicago." */
    val credit: String get() = Credit.format(artist, title, date, museum.displayName)

    companion object {
        fun wallpaperId(museum: Museum, sourceId: String) = "${museum.key}:$sourceId"

        /** Splits "aic:27992" into (AIC, "27992"), or null for ids that aren't museum artworks. */
        fun parseId(id: String): Pair<Museum, String>? {
            val museum = Museum.fromKey(id.substringBefore(':', "")) ?: return null
            val sourceId = id.substringAfter(':').takeIf { it.isNotBlank() } ?: return null
            return museum to sourceId
        }
    }
}

object Credit {
    fun format(artist: String?, title: String, date: String?, museum: String): String {
        val titlePart = buildString {
            append(title.ifBlank { "Untitled" }.trim())
            date?.trim()?.takeIf { it.isNotEmpty() }?.let { append(", ").append(it) }
        }
        return listOfNotNull(artist?.trim()?.takeIf { it.isNotEmpty() }, titlePart, museum)
            .joinToString(" ") { it.withFullStop() }
    }

    private fun String.withFullStop() = if (last() in ".?!") this else "$this."
}
