package com.driftwalls.app.domain

import com.driftwalls.app.domain.rotation.RotationOrder
import com.driftwalls.app.domain.rotation.RotationPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RotationPlannerTest {
    private val ids = listOf("a", "b", "c", "d", "e", "f")

    @Test
    fun `no candidates gives null`() {
        assertNull(RotationPlanner.next(emptyList(), listOf("a"), RotationOrder.SHUFFLE))
        assertNull(RotationPlanner.next(emptyList(), emptyList(), RotationOrder.IN_ORDER))
    }

    @Test
    fun `in order starts at the first and continues after the newest shown`() {
        assertEquals("a", RotationPlanner.next(ids, emptyList(), RotationOrder.IN_ORDER))
        assertEquals("d", RotationPlanner.next(ids, listOf("c", "a"), RotationOrder.IN_ORDER))
    }

    @Test
    fun `in order wraps around and skips history that left the list`() {
        assertEquals("a", RotationPlanner.next(ids, listOf("f"), RotationOrder.IN_ORDER))
        // "x" was removed from favourites; continue from "b", the newest one still present.
        assertEquals("c", RotationPlanner.next(ids, listOf("x", "b"), RotationOrder.IN_ORDER))
    }

    @Test
    fun `shuffle avoids the most recent half of the pool`() {
        val history = listOf("a", "b", "c")
        repeat(200) { seed ->
            val next = RotationPlanner.next(ids, history, RotationOrder.SHUFFLE, Random(seed))
            assertTrue("seed $seed picked $next", next in setOf("d", "e", "f"))
        }
    }

    @Test
    fun `shuffle never repeats the previous wallpaper when there are two`() {
        repeat(50) { seed ->
            assertEquals("b", RotationPlanner.next(listOf("a", "b"), listOf("a"), RotationOrder.SHUFFLE, Random(seed)))
        }
    }

    @Test
    fun `shuffle with a single candidate returns it`() {
        assertEquals("a", RotationPlanner.next(listOf("a"), listOf("a"), RotationOrder.SHUFFLE))
    }

    @Test
    fun `shuffle reaches every allowed candidate`() {
        val seen = (0 until 300).mapTo(HashSet()) { seed ->
            RotationPlanner.next(ids, listOf("a"), RotationOrder.SHUFFLE, Random(seed))
        }
        assertEquals(setOf("b", "c", "d", "e", "f"), seen)
    }
}
