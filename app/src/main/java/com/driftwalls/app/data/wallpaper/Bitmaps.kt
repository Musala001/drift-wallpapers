package com.driftwalls.app.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.graphics.createBitmap
import com.driftwalls.app.domain.image.ScreenCrop

data class ScreenSize(val width: Int, val height: Int) {
    companion object {
        /** Full physical screen in portrait orientation, including system bars. */
        fun of(context: Context): ScreenSize {
            val wm = context.getSystemService(WindowManager::class.java)
            val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = wm.maximumWindowMetrics.bounds
                bounds.width() to bounds.height()
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(metrics)
                metrics.widthPixels to metrics.heightPixels
            }
            return ScreenSize(minOf(w, h).coerceAtLeast(1), maxOf(w, h).coerceAtLeast(1))
        }
    }
}

/** Center-crops and scales [source] to exactly [size]. Returns [source] itself if it already fits. */
fun Bitmap.cropToScreen(size: ScreenSize): Bitmap {
    if (width == size.width && height == size.height) return this
    val crop = ScreenCrop.centerCrop(width, height, size.width, size.height)
    val out = createBitmap(size.width, size.height)
    Canvas(out).drawBitmap(
        this,
        Rect(crop.left, crop.top, crop.right, crop.bottom),
        Rect(0, 0, size.width, size.height),
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
    )
    return out
}
