package com.driftwalls.app.data.generator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import com.driftwalls.app.domain.generator.GeneratorSpec
import com.driftwalls.app.domain.generator.Palette
import com.driftwalls.app.domain.generator.PatternStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** Draws a [GeneratorSpec]. The same spec gives the same picture at any size. */
object PatternRenderer {
    fun render(spec: GeneratorSpec, width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        // Geometry uses its own stream so changing the palette rules doesn't reshuffle shapes.
        val random = Random(spec.seed xor GEOMETRY_SALT)
        val colors = spec.palette
        val w = width.toFloat()
        val h = height.toFloat()
        when (spec.style) {
            PatternStyle.GRADIENT -> gradient(canvas, w, h, colors, random)
            PatternStyle.MESH -> mesh(canvas, w, h, colors, random)
            PatternStyle.WAVES -> waves(canvas, w, h, colors, random)
            PatternStyle.RINGS -> rings(canvas, w, h, colors, random)
            PatternStyle.STRIPES -> stripes(canvas, w, h, colors, random)
            PatternStyle.DOTS -> dots(canvas, w, h, colors, random)
        }
        vignette(canvas, w, h)
        return bitmap
    }

    private fun paint() = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private fun gradient(canvas: Canvas, w: Float, h: Float, colors: List<Int>, random: Random) {
        val angle = random.nextDouble(0.0, 2 * PI)
        val r = hypot(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f
        val dx = (cos(angle) * r).toFloat()
        val dy = (sin(angle) * r).toFloat()
        val ordered = colors.sortedBy { luminance(it) }.let { if (random.nextBoolean()) it else it.reversed() }
        canvas.drawRect(0f, 0f, w, h, paint().apply {
            shader = LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, ordered.toIntArray(), null, Shader.TileMode.CLAMP)
        })
        // A soft light spot keeps large gradients from looking flat.
        glow(canvas, random.nextFloat() * w, random.nextFloat() * h, maxOf(w, h) * 0.6f, Palette.withAlpha(colors[3], 90))
    }

    private fun mesh(canvas: Canvas, w: Float, h: Float, colors: List<Int>, random: Random) {
        canvas.drawColor(colors[0])
        repeat(6) { i ->
            val color = colors[1 + i % (colors.size - 1)]
            glow(canvas, random.nextFloat() * w, random.nextFloat() * h, maxOf(w, h) * (0.35f + random.nextFloat() * 0.4f), Palette.withAlpha(color, 200))
        }
    }

    private fun waves(canvas: Canvas, w: Float, h: Float, colors: List<Int>, random: Random) {
        val sky = colors.sortedBy { luminance(it) }
        canvas.drawRect(0f, 0f, w, h, paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h, sky[3], sky[1], Shader.TileMode.CLAMP)
        })
        val layers = 6
        for (i in 0 until layers) {
            val t = i / (layers - 1f)
            val baseY = h * (0.35f + 0.6f * t)
            val amplitude = h * (0.02f + random.nextFloat() * 0.04f)
            val frequency = (1.0 + random.nextDouble() * 2.0) * 2 * PI / w
            val phase = random.nextDouble(0.0, 2 * PI)
            val path = Path().apply {
                moveTo(0f, h)
                var x = 0f
                while (x <= w) {
                    lineTo(x, baseY + (sin(x * frequency + phase) * amplitude).toFloat())
                    x += w / 64f
                }
                lineTo(w, h)
                close()
            }
            canvas.drawPath(path, paint().apply { color = Palette.lerp(sky[2], sky[0], t) })
        }
    }

    private fun rings(canvas: Canvas, w: Float, h: Float, colors: List<Int>, random: Random) {
        val cx = w * (0.2f + random.nextFloat() * 0.6f)
        val cy = h * (0.2f + random.nextFloat() * 0.6f)
        val maxR = hypot(maxOf(cx, w - cx), maxOf(cy, h - cy))
        val count = 10 + random.nextInt(8)
        val p = paint()
        for (i in 0 until count) {
            val t = i / (count - 1f)
            // Walk the palette from outside in, blending between neighbours.
            val pos = t * (colors.size - 1)
            val idx = pos.toInt().coerceAtMost(colors.size - 2)
            p.color = Palette.lerp(colors[idx], colors[idx + 1], pos - idx)
            canvas.drawCircle(cx, cy, maxR * (1f - t * 0.95f), p)
        }
    }

    private fun stripes(canvas: Canvas, w: Float, h: Float, colors: List<Int>, random: Random) {
        canvas.drawColor(colors[0])
        val diagonal = hypot(w, h)
        canvas.withRotation(-60f + random.nextFloat() * 120f, w / 2f, h / 2f) {
            var x = -diagonal / 2f + w / 2f
            val p = paint()
            var i = 0
            while (x < w / 2f + diagonal / 2f) {
                val band = diagonal * (0.03f + random.nextFloat() * 0.09f)
                p.color = Palette.lerp(colors[i % colors.size], colors[(i + 1) % colors.size], random.nextFloat() * 0.5f)
                drawRect(x, h / 2f - diagonal / 2f, x + band, h / 2f + diagonal / 2f, p)
                x += band
                i++
            }
        }
    }

    private fun dots(canvas: Canvas, w: Float, h: Float, colors: List<Int>, random: Random) {
        canvas.drawRect(0f, 0f, w, h, paint().apply {
            shader = LinearGradient(0f, 0f, w, h, colors[0], colors[1], Shader.TileMode.CLAMP)
        })
        val cell = w / (14 + random.nextInt(10))
        val fx = random.nextFloat() * w
        val fy = random.nextFloat() * h
        val reach = hypot(w, h)
        val p = paint()
        var y = cell / 2f
        var row = 0
        while (y < h + cell) {
            // Offset every other row for a hexagonal halftone.
            var x = if (row % 2 == 0) cell / 2f else cell
            while (x < w + cell) {
                val t = (hypot(x - fx, y - fy) / reach).coerceIn(0f, 1f)
                p.color = Palette.lerp(colors[3], colors[2], t)
                canvas.drawCircle(x, y, cell * 0.45f * (1f - t * 0.8f), p)
                x += cell
            }
            y += cell * 0.866f
            row++
        }
    }

    private fun glow(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        canvas.drawCircle(x, y, radius, paint().apply {
            shader = RadialGradient(x, y, radius, color, Palette.withAlpha(color, 0), Shader.TileMode.CLAMP)
        })
    }

    /** Slight darkening at the edges so icons and the clock stay readable. */
    private fun vignette(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRect(0f, 0f, w, h, paint().apply {
            shader = RadialGradient(w / 2f, h / 2f, hypot(w, h) / 2f, intArrayOf(0x00000000, 0x00000000, 0x55000000), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        })
    }

    private fun luminance(color: Int): Float =
        0.2126f * ((color shr 16) and 0xFF) + 0.7152f * ((color shr 8) and 0xFF) + 0.0722f * (color and 0xFF)

    private const val GEOMETRY_SALT = 0x5DEECE66DL
}
