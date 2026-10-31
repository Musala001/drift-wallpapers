package com.driftwalls.app.wallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.driftwalls.app.data.rotation.RotationEngine
import com.driftwalls.app.data.wallpaper.LiveWallpaperStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Shows the rotation feed with crossfades and launcher parallax; double-tap moves to the next wallpaper. */
@AndroidEntryPoint
class DriftWallpaperService : WallpaperService() {
    @Inject lateinit var store: LiveWallpaperStore
    @Inject lateinit var rotation: RotationEngine

    override fun onCreateEngine(): Engine = DriftEngine()

    private inner class DriftEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        private var current: Bitmap? = null
        private var previous: Bitmap? = null
        /** 0..1 progress of the crossfade from [previous] to [current]. */
        private var fade = 1f
        private var xOffset = 0.5f
        private var fadeJob: Job? = null
        private var rotateJob: Job? = null

        private val gestures = GestureDetector(
            this@DriftWallpaperService,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (!isPreview) rotateNext()
                    return true
                }
            },
        )

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            scope.launch {
                var first = true
                store.version.collect {
                    val bitmap = withContext(Dispatchers.IO) { decodeCurrent() }
                    when {
                        bitmap != null -> show(bitmap, animate = !first)
                        // Just chosen as the wallpaper with nothing rotated yet: fetch the first one.
                        first && !isPreview -> rotateNext()
                    }
                    first = false
                }
            }
        }

        override fun onDestroy() {
            scope.cancel()
            super.onDestroy()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) draw()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            draw()
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            this.xOffset = xOffset
            draw()
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestures.onTouchEvent(event)
        }

        private fun rotateNext() {
            if (rotateJob?.isActive == true) return
            rotateJob = scope.launch {
                try {
                    rotation.rotateNow(forceLive = true)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Offline or nothing to show: keep the current image. The scheduler retries later.
                }
            }
        }

        private fun decodeCurrent(): Bitmap? =
            store.currentFile.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }

        private fun show(bitmap: Bitmap, animate: Boolean) {
            fadeJob?.cancel()
            previous = if (animate) current else null
            current = bitmap
            if (!animate || !isVisible) {
                fade = 1f
                previous = null
                draw()
                return
            }
            fadeJob = scope.launch {
                val start = SystemClock.uptimeMillis()
                do {
                    fade = ((SystemClock.uptimeMillis() - start) / FADE_MS.toFloat()).coerceAtMost(1f)
                    draw()
                    delay(FRAME_MS)
                } while (fade < 1f)
                previous = null
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = try {
                holder.lockCanvas()
            } catch (_: Exception) {
                null
            } ?: return
            try {
                val old = previous
                val now = current
                if (now == null) {
                    drawPlaceholder(canvas)
                } else {
                    if (old != null && fade < 1f) drawImage(canvas, old, 255)
                    else canvas.drawColor(Color.BLACK)
                    drawImage(canvas, now, if (old != null) (fade * 255).toInt() else 255)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        /** Scales to the surface height and pans horizontally with the launcher's page offset. */
        private fun drawImage(canvas: Canvas, bitmap: Bitmap, alpha: Int) {
            val scale = maxOf(canvas.height / bitmap.height.toFloat(), canvas.width / bitmap.width.toFloat())
            val w = bitmap.width * scale
            val h = bitmap.height * scale
            val left = -(w - canvas.width) * xOffset
            val top = -(h - canvas.height) / 2f
            paint.alpha = alpha
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), paint)
        }

        private fun drawPlaceholder(canvas: Canvas) {
            val gradient = Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, canvas.height.toFloat(), 0xFF1B2A3A.toInt(), 0xFF0B0F14.toInt(), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), gradient)
        }
    }

    private companion object {
        const val FADE_MS = 900L
        const val FRAME_MS = 16L
    }
}
