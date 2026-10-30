package com.driftwalls.app.data.remote.aic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AicMapperTest {
    private val iiif = "https://www.artic.edu/iiif/2"
    private val bedroom = AicArtworkDto(
        id = 28560,
        title = "The Bedroom",
        artistTitle = "Vincent van Gogh",
        artistDisplay = "Vincent van Gogh\nDutch, 1853–1890",
        dateDisplay = "1889",
        imageId = "25c31d8d-21a4-9ea1-1d73-6a2eca4dda7e",
        isPublicDomain = true,
        thumbnail = AicThumbnailDto(width = 3000, height = 2400, altText = "A bedroom."),
    )

    @Test
    fun `maps a public-domain artwork with IIIF urls`() {
        val art = AicMapper.toArtwork(bedroom, iiif)!!
        assertEquals("aic:28560", art.id)
        assertEquals("$iiif/${bedroom.imageId}/full/843,/0/default.jpg", art.thumbnailUrl)
        assertEquals("$iiif/${bedroom.imageId}/full/1686,/0/default.jpg", art.imageUrl)
        assertEquals(3000, art.width)
        assertEquals("Vincent van Gogh. The Bedroom, 1889. The Art Institute of Chicago.", art.credit)
    }

    @Test
    fun `rejects artworks that are not public domain or have an unknown status`() {
        assertNull(AicMapper.toArtwork(bedroom.copy(isPublicDomain = false), iiif))
        assertNull(AicMapper.toArtwork(bedroom.copy(isPublicDomain = null), iiif))
    }

    @Test
    fun `rejects artworks without an image id`() {
        assertNull(AicMapper.toArtwork(bedroom.copy(imageId = null), iiif))
        assertNull(AicMapper.toArtwork(bedroom.copy(imageId = ""), iiif))
    }

    @Test
    fun `uses the first line of artist_display when artist_title is missing`() {
        val art = AicMapper.toArtwork(bedroom.copy(artistTitle = null), iiif)!!
        assertEquals("Vincent van Gogh", art.artist)
    }

    @Test
    fun `uses the response iiif base and tolerates a trailing slash or a missing one`() {
        assertEquals(
            "https://iiif.example/2/${bedroom.imageId}/full/843,/0/default.jpg",
            AicMapper.toArtwork(bedroom, "https://iiif.example/2/")!!.thumbnailUrl,
        )
        assertEquals(
            "${AicApi.FALLBACK_IIIF_URL}/${bedroom.imageId}/full/843,/0/default.jpg",
            AicMapper.toArtwork(bedroom, null)!!.thumbnailUrl,
        )
    }
}
