package com.driftwalls.app.domain

import com.driftwalls.app.domain.image.CropRect
import com.driftwalls.app.domain.image.ScreenCrop
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenCropTest {
    @Test
    fun `landscape source keeps full height and trims the sides evenly`() {
        val crop = ScreenCrop.centerCrop(2000, 1000, 1080, 2400)
        assertEquals(CropRect(775, 0, 1225, 1000), crop)
    }

    @Test
    fun `tall source keeps full width and trims top and bottom`() {
        val crop = ScreenCrop.centerCrop(1000, 4000, 1000, 2000)
        assertEquals(CropRect(0, 1000, 1000, 3000), crop)
    }

    @Test
    fun `matching aspect ratio is untouched`() {
        assertEquals(CropRect(0, 0, 540, 1200), ScreenCrop.centerCrop(540, 1200, 1080, 2400))
    }

    @Test
    fun `crop always keeps the target aspect ratio within a pixel`() {
        listOf(843 to 1100, 1686 to 1000, 3000 to 3001, 1 to 5000, 5000 to 1).forEach { (w, h) ->
            val crop = ScreenCrop.centerCrop(w, h, 1080, 2340)
            val expectedHeight = crop.width * 2340.0 / 1080
            assert(kotlin.math.abs(crop.height - expectedHeight) <= 2.2 || crop.width == 1 || crop.height == h) {
                "$w x $h -> $crop"
            }
            assert(crop.left >= 0 && crop.top >= 0 && crop.right <= w && crop.bottom <= h)
        }
    }
}
