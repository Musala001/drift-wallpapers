package com.driftwalls.app.data.remote.aic

import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum

object AicMapper {
    /** Grid size: the docs' recommended (and most-cached) IIIF size. */
    const val THUMBNAIL_WIDTH = 843

    /** Largest size the docs allow, and only for public-domain images. */
    const val WALLPAPER_WIDTH = 1686

    /** Returns null unless the artwork is public domain and has an image. */
    fun toArtwork(dto: AicArtworkDto, iiifUrl: String?): Artwork? {
        if (dto.isPublicDomain != true) return null
        val imageId = dto.imageId?.trim()?.ifEmpty { null } ?: return null
        val base = (iiifUrl?.ifBlank { null } ?: AicApi.FALLBACK_IIIF_URL).trimEnd('/')
        return Artwork(
            museum = Museum.AIC,
            sourceId = dto.id.toString(),
            title = dto.title?.trim()?.ifEmpty { null } ?: "Untitled",
            artist = artistName(dto),
            date = dto.dateDisplay?.trim()?.ifEmpty { null },
            thumbnailUrl = iiif(base, imageId, THUMBNAIL_WIDTH),
            imageUrl = iiif(base, imageId, WALLPAPER_WIDTH),
            width = dto.thumbnail?.width,
            height = dto.thumbnail?.height,
            altText = dto.thumbnail?.altText?.trim()?.ifEmpty { null },
            webUrl = "https://www.artic.edu/artworks/${dto.id}",
        )
    }

    fun iiif(base: String, imageId: String, width: Int) = "$base/$imageId/full/$width,/0/default.jpg"

    /** artist_title is the clean name; artist_display adds nationality and dates on later lines. */
    private fun artistName(dto: AicArtworkDto): String? =
        dto.artistTitle?.trim()?.ifEmpty { null }
            ?: dto.artistDisplay?.lineSequence()?.firstOrNull()?.trim()?.ifEmpty { null }
}
