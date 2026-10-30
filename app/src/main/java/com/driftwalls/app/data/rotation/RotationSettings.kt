package com.driftwalls.app.data.rotation

import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.rotation.RotationOrder

enum class RotationSource(val label: String) {
    FAVORITES("Favourites"),
    PICKS("My rotation"),
    CATEGORY("Category"),
}

/** WorkManager's minimum periodic interval is 15 minutes. */
enum class RotationInterval(val minutes: Long, val label: String) {
    MIN_15(15, "15 min"),
    HOUR_1(60, "1 hour"),
    HOUR_3(3 * 60, "3 hours"),
    HOUR_6(6 * 60, "6 hours"),
    HOUR_12(12 * 60, "12 hours"),
    DAY_1(24 * 60, "Daily"),
}

data class RotationSettings(
    val source: RotationSource = RotationSource.CATEGORY,
    /** Used when [source] is [RotationSource.CATEGORY]. */
    val category: Category = Category.IMPRESSIONISM,
    val order: RotationOrder = RotationOrder.SHUFFLE,
    val target: WallpaperTarget = WallpaperTarget.BOTH,
    /** Scheduled rotation on or off. */
    val enabled: Boolean = false,
    val interval: RotationInterval = RotationInterval.HOUR_6,
    /** Only download new museum images on unmetered networks. */
    val wifiOnly: Boolean = true,
)
