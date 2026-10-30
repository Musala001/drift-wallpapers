package com.driftwalls.app.data.remote.met

import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Museum

object MetMapper {
    /** Returns null unless the object is public domain and has a primary image. */
    fun toArtwork(dto: MetObjectDto): Artwork? {
        if (!dto.isPublicDomain) return null
        val image = dto.primaryImage.trim().toHttps().ifEmpty { return null }
        return Artwork(
            museum = Museum.MET,
            sourceId = dto.objectID.toString(),
            title = dto.title.trim().ifEmpty { "Untitled" },
            artist = dto.artistDisplayName.trim().ifEmpty { null },
            date = dto.objectDate.trim().ifEmpty { null },
            thumbnailUrl = dto.primaryImageSmall.trim().toHttps().ifEmpty { image },
            imageUrl = image,
            webUrl = dto.objectURL.trim().ifEmpty { null },
        )
    }

    private fun String.toHttps() = if (startsWith("http://")) "https://" + removePrefix("http://") else this
}
