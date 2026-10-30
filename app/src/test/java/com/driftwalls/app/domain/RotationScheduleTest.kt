package com.driftwalls.app.domain

import com.driftwalls.app.data.rotation.NetworkNeed
import com.driftwalls.app.data.rotation.RotationSettings
import com.driftwalls.app.data.rotation.RotationSource
import com.driftwalls.app.data.rotation.networkNeed
import com.driftwalls.app.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationScheduleTest {
    @Test
    fun `museum sources need unmetered network when wifi only`() {
        assertEquals(NetworkNeed.UNMETERED, RotationSettings(source = RotationSource.FAVORITES, wifiOnly = true).networkNeed())
        assertEquals(NetworkNeed.ANY, RotationSettings(source = RotationSource.PICKS, wifiOnly = false).networkNeed())
        assertEquals(NetworkNeed.ANY, RotationSettings(source = RotationSource.CATEGORY, category = Category.LANDSCAPES, wifiOnly = false).networkNeed())
    }

    @Test
    fun `generated category needs no network`() {
        val settings = RotationSettings(source = RotationSource.CATEGORY, category = Category.GENERATED, wifiOnly = true)
        assertEquals(NetworkNeed.NONE, settings.networkNeed())
    }

    @Test
    fun `generated category only matters when it is the source`() {
        val settings = RotationSettings(source = RotationSource.FAVORITES, category = Category.GENERATED, wifiOnly = true)
        assertEquals(NetworkNeed.UNMETERED, settings.networkNeed())
    }
}
