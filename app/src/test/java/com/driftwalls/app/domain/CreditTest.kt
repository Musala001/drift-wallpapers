package com.driftwalls.app.domain

import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.domain.model.Credit
import com.driftwalls.app.domain.model.Museum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditTest {
    @Test
    fun `formats artist, title, date and museum`() {
        assertEquals(
            "Vincent van Gogh. The Bedroom, 1889. The Art Institute of Chicago.",
            Credit.format("Vincent van Gogh", "The Bedroom", "1889", "The Art Institute of Chicago"),
        )
    }

    @Test
    fun `omits missing artist and date`() {
        assertEquals("Water Lilies. The Metropolitan Museum of Art.", Credit.format(null, "Water Lilies", " ", "The Metropolitan Museum of Art"))
    }

    @Test
    fun `blank title becomes Untitled`() {
        assertEquals("Anon. Untitled, ca. 1800. Museum.", Credit.format("Anon", "", "ca. 1800", "Museum"))
    }

    @Test
    fun `does not double existing terminal punctuation`() {
        assertEquals("Jr. Who Goes There?, 1900. Museum.", Credit.format("Jr.", "Who Goes There?", "1900", "Museum"))
        assertEquals("Artist. What Is It? Museum.", Credit.format("Artist", "What Is It?", null, "Museum"))
    }

    @Test
    fun `artwork id round-trips`() {
        assertEquals(Museum.AIC to "27992", Artwork.parseId("aic:27992"))
        assertNull(Artwork.parseId("gen:abc"))
        assertNull(Artwork.parseId("met:"))
        assertNull(Artwork.parseId("nonsense"))
    }
}
