package com.driftwalls.app.domain.generator

import kotlin.math.abs
import kotlin.random.Random

enum class PatternStyle(val key: String, val label: String) {
    GRADIENT("gradient", "Gradient"),
    MESH("mesh", "Mesh"),
    WAVES("waves", "Waves"),
    RINGS("rings", "Rings"),
    STRIPES("stripes", "Stripes"),
    DOTS("dots", "Dots"),
}

enum class Harmony(val key: String, val label: String) {
    ANALOGOUS("analogous", "Analogous"),
    COMPLEMENTARY("complementary", "Complementary"),
    TRIADIC("triadic", "Triadic"),
    MONO("mono", "Mono"),
}

/**
 * Everything needed to redraw a generated wallpaper. Its [id] ("gen:waves:triadic:42") is stored
 * like a museum id, so generated wallpapers can sit in history and rotation lists.
 */
data class GeneratorSpec(val style: PatternStyle, val harmony: Harmony, val seed: Long) {
    val id: String get() = "$PREFIX:${style.key}:${harmony.key}:$seed"

    val title: String get() = "${style.label}, ${harmony.label.lowercase()} #${abs(seed % 10_000)}"

    val palette: List<Int> get() = Palette.generate(harmony, seed)

    companion object {
        const val PREFIX = "gen"

        fun isGeneratedId(id: String) = id.startsWith("$PREFIX:")

        fun parseId(id: String): GeneratorSpec? {
            val parts = id.split(':')
            if (parts.size != 4 || parts[0] != PREFIX) return null
            val style = PatternStyle.entries.firstOrNull { it.key == parts[1] } ?: return null
            val harmony = Harmony.entries.firstOrNull { it.key == parts[2] } ?: return null
            val seed = parts[3].toLongOrNull() ?: return null
            return GeneratorSpec(style, harmony, seed)
        }

        fun random(random: Random = Random): GeneratorSpec = GeneratorSpec(
            style = PatternStyle.entries.random(random),
            harmony = Harmony.entries.random(random),
            // Non-negative and short, so ids and titles stay readable.
            seed = random.nextLong(0, 1_000_000_000L),
        )
    }
}

/** Colour palettes from a seed, as ARGB ints. Pure Kotlin so it is unit-tested on the JVM. */
object Palette {
    const val SIZE = 4

    fun generate(harmony: Harmony, seed: Long): List<Int> {
        val random = Random(seed)
        val baseHue = random.nextFloat() * 360f
        val saturation = 0.45f + random.nextFloat() * 0.35f
        val hueOffsets = when (harmony) {
            Harmony.ANALOGOUS -> listOf(0f, 25f, 50f, -25f)
            Harmony.COMPLEMENTARY -> listOf(0f, 180f, 20f, 200f)
            Harmony.TRIADIC -> listOf(0f, 120f, 240f, 60f)
            Harmony.MONO -> listOf(0f, 0f, 8f, -8f)
        }
        // Mostly deep tones: wallpapers sit behind white icons and text.
        val lightness = listOf(0.16f, 0.32f, 0.5f, 0.68f)
        return hueOffsets.mapIndexed { i, offset ->
            val s = if (harmony == Harmony.MONO) saturation * (1f - i * 0.15f) else saturation
            hsl(hue = offset + baseHue, saturation = s, lightness = lightness[i])
        }
    }

    /** HSL to opaque ARGB. [hue] in degrees (any value, wrapped), the rest in 0..1. */
    fun hsl(hue: Float, saturation: Float, lightness: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(255, channel(r + m), channel(g + m), channel(b + m))
    }

    /** Linear blend of two ARGB colours, [t] in 0..1. */
    fun lerp(from: Int, to: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        fun mix(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * f + 0.5f).toInt()
        }
        return argb(mix(24), mix(16), mix(8), mix(0))
    }

    fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    private fun channel(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()

    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
}
