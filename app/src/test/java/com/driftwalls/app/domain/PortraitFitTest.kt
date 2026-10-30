package com.driftwalls.app.domain

import com.driftwalls.app.domain.image.PortraitFit
import com.driftwalls.app.testing.artwork
import org.junit.Assert.assertEquals
import org.junit.Test

class PortraitFitTest {
    @Test
    fun `puts portrait and unknown sizes first, keeping order within groups`() {
        val wide = artwork(id = "wide", width = 1600, height = 1000)
        val tall = artwork(id = "tall", width = 1000, height = 1600)
        val unknown = artwork(id = "unknown")
        val square = artwork(id = "square", width = 1000, height = 1050)
        val wide2 = artwork(id = "wide2", width = 1500, height = 1000)

        val ranked = PortraitFit.rank(listOf(wide, tall, unknown, wide2, square))

        assertEquals(listOf("tall", "unknown", "square", "wide", "wide2"), ranked.map { it.sourceId })
    }

    @Test
    fun `drops panoramas`() {
        val scroll = artwork(id = "scroll", width = 5000, height = 1000)
        val ok = artwork(id = "ok", width = 2000, height = 1000)
        assertEquals(listOf("ok"), PortraitFit.rank(listOf(scroll, ok)).map { it.sourceId })
    }

    @Test
    fun `ignores nonsense dimensions`() {
        val zero = artwork(id = "zero", width = 0, height = 0)
        assertEquals(listOf("zero"), PortraitFit.rank(listOf(zero)).map { it.sourceId })
    }
}
