package com.driftwalls.app.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class WallpaperTarget(val label: String, val flags: Int) {
    HOME("Home screen", WallpaperManager.FLAG_SYSTEM),
    LOCK("Lock screen", WallpaperManager.FLAG_LOCK),
    BOTH("Home and lock", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK),
}

@Singleton
class WallpaperApplier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {
    /**
     * Downloads (or reads from cache) [url] and returns it cropped to the screen, made [widthFactor]
     * times wider than the screen when the caller pans it (live wallpaper parallax).
     */
    suspend fun loadScreenBitmap(url: String, widthFactor: Float = 1f): Bitmap {
        val screen = ScreenSize.of(context)
        val size = ScreenSize((screen.width * widthFactor).toInt(), screen.height)
        val request = ImageRequest.Builder(context)
            .data(url)
            // Decode at least screen-sized; we crop precisely ourselves.
            .size(size.width, size.height)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .build()
        return when (val result = imageLoader.execute(request)) {
            is SuccessResult -> withContext(Dispatchers.Default) { result.image.toBitmap().cropToScreen(size) }
            is ErrorResult -> throw IOException("Couldn't load $url", result.throwable)
        }
    }

    suspend fun apply(bitmap: Bitmap, target: WallpaperTarget) = withContext(Dispatchers.IO) {
        WallpaperManager.getInstance(context).setBitmap(bitmap, null, true, target.flags)
    }

    suspend fun apply(url: String, target: WallpaperTarget) = apply(loadScreenBitmap(url), target)
}
