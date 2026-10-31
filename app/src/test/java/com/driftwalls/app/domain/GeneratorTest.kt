package com.driftwalls.app.domain

import com.driftwalls.app.domain.generator.GeneratorSpec
import com.driftwalls.app.domain.generator.Harmony
import com.driftwalls.app.domain.generator.Palette
import com.driftwalls.app.domain.generator.PatternStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GeneratorTest {
    @Test
    fun `spec id round trips`() {
        repeat(100) { seed ->
            val spec = GeneratorSpec.random(Random(seed))
            assertEquals(spec, GeneratorSpec.parseId(spec.id))
            assertTrue(GeneratorSpec.isGeneratedId(spec.id))
        }
        assertEquals("gen:waves:triadic:42", GeneratorSpec(PatternStyle.WAVES, Harmony.TRIADIC, 42).id)
    }

    @Test
    fun `parse rejects ids that are not generated wallpapers`() {
        listOf("aic:27992", "gen:waves:triadic", "gen:nope:mono:1", "gen:mesh:nope:1", "gen:mesh:mono:x", "", "gen:mesh:mono:1:2")
            .forEach { assertNull(it, GeneratorSpec.parseId(it)) }
        assertFalse(GeneratorSpec.isGeneratedId("met:1"))
    }

    @Test
    fun `palette is deterministic per seed and opaque`() {
        Harmony.entries.forEach { harmony ->
            val a = Palette.generate(harmony, 7)
            assertEquals(a, Palette.generate(harmony, 7))
            assertEquals(Palette.SIZE, a.size)
            a.forEach { assertEquals(0xFF, (it ushr 24) and 0xFF) }
        }
        assertNotEquals(Palette.generate(Harmony.TRIADIC, 1), Palette.generate(Harmony.TRIADIC, 2))
    }

    @Test
    fun `hsl converts primary colours`() {
        assertEquals(0xFFFF0000.toInt(), Palette.hsl(0f, 1f, 0.5f))
        assertEquals(0xFF00FF00.toInt(), Palette.hsl(120f, 1f, 0.5f))
        assertEquals(0xFF0000FF.toInt(), Palette.hsl(240f, 1f, 0.5f))
        assertEquals(0xFFFF0000.toInt(), Palette.hsl(360f, 1f, 0.5f))
        assertEquals(0xFF808080.toInt(), Palette.hsl(77f, 0f, 0.5f))
        assertEquals(0xFFFFFFFF.toInt(), Palette.hsl(10f, 0.3f, 1f))
    }

    @Test
    fun `lerp blends every channel`() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        assertEquals(black, Palette.lerp(black, white, 0f))
        assertEquals(white, Palette.lerp(black, white, 1f))
        assertEquals(0xFF808080.toInt(), Palette.lerp(black, white, 0.5f))
        assertEquals(0x80FF0000.toInt(), Palette.lerp(0x00FF0000, 0xFFFF0000.toInt(), 0.5f))
    }

    @Test
    fun `mono palette gets lighter step by step`() {
        val palette = Palette.generate(Harmony.MONO, 3)
        val lum = palette.map { ((it shr 16) and 0xFF) + ((it shr 8) and 0xFF) + (it and 0xFF) }
        assertEquals(lum.sorted(), lum)
    }
}
