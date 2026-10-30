package com.driftwalls.app.data.remote.met

import com.driftwalls.app.domain.model.Museum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MetMapperTest {
    private val sunflowers = MetObjectDto(
        objectID = 436524,
        isPublicDomain = true,
        primaryImage = "https://images.metmuseum.org/CRDImages/ep/original/DP-41223-001.jpg",
        primaryImageSmall = "https://images.metmuseum.org/CRDImages/ep/web-large/DP-41223-001.jpg",
        title = "Sunflowers",
        artistDisplayName = "Vincent van Gogh",
        objectDate = "1887",
        objectURL = "https://www.metmuseum.org/art/collection/search/436524",
    )

    @Test
    fun `maps a public-domain object with an image`() {
        val art = MetMapper.toArtwork(sunflowers)!!
        assertEquals("met:436524", art.id)
        assertEquals(Museum.MET, art.museum)
        assertEquals(sunflowers.primaryImage, art.imageUrl)
        assertEquals(sunflowers.primaryImageSmall, art.thumbnailUrl)
        assertEquals("Vincent van Gogh. Sunflowers, 1887. The Metropolitan Museum of Art.", art.credit)
    }

    @Test
    fun `rejects objects that are not public domain`() {
        assertNull(MetMapper.toArtwork(sunflowers.copy(isPublicDomain = false)))
    }

    @Test
    fun `rejects public-domain objects without a primary image`() {
        assertNull(MetMapper.toArtwork(sunflowers.copy(primaryImage = "")))
        assertNull(MetMapper.toArtwork(sunflowers.copy(primaryImage = "   ")))
    }

    @Test
    fun `falls back to the full image when there is no small one`() {
        val art = MetMapper.toArtwork(sunflowers.copy(primaryImageSmall = ""))!!
        assertEquals(sunflowers.primaryImage, art.thumbnailUrl)
    }

    @Test
    fun `upgrades http image urls and tolerates missing artist and date`() {
        val art = MetMapper.toArtwork(
            sunflowers.copy(primaryImage = "http://images.metmuseum.org/x.jpg", artistDisplayName = "", objectDate = ""),
        )
        assertNotNull(art)
        assertEquals("https://images.metmuseum.org/x.jpg", art!!.imageUrl)
        assertNull(art.artist)
        assertEquals("Sunflowers. The Metropolitan Museum of Art.", art.credit)
    }
}
