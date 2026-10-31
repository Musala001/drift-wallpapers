package com.driftwalls.app.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands rotated images to [com.driftwalls.app.wallpaper.DriftWallpaperService]. The current image is
 * kept on disk so the live wallpaper survives process death; [version] tells a running engine to reload.
 */
@Singleton
class LiveWallpaperStore @Inject constructor(@ApplicationContext private val context: Context) {
    // Excluded from cloud backup in data_extraction_rules.xml.
    private val dir = File(context.filesDir, "rotation_cache")
    val currentFile = File(dir, "live.jpg")

    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()

    /** True when Drift's live wallpaper is the current home screen wallpaper. */
    fun isActive(): Boolean =
        WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName

    suspend fun publish(bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            // Write then rename, so the engine never decodes a half-written file.
            val tmp = File(dir, "live.jpg.tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (!tmp.renameTo(currentFile)) {
                tmp.copyTo(currentFile, overwrite = true)
                tmp.delete()
            }
        }
        _version.value = System.currentTimeMillis()
    }

    companion object {
        /** The live image is wider than the screen so it can pan with the launcher's pages. */
        const val PARALLAX_WIDTH = 1.2f
        private const val JPEG_QUALITY = 92
    }
}
