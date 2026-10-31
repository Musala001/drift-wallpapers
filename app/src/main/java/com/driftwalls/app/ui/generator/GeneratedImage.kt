package com.driftwalls.app.ui.generator

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.driftwalls.app.data.generator.PatternRenderer
import com.driftwalls.app.domain.generator.GeneratorSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Grid and history tiles; 9:16 like the museum tiles. */
const val THUMB_WIDTH = 216
const val THUMB_HEIGHT = 384

private object GeneratedCache : LruCache<String, Bitmap>(32 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

/** Draws [spec] at [width] x [height] off the main thread and shows it scaled to the modifier's size. */
@Composable
fun GeneratedImage(spec: GeneratorSpec, modifier: Modifier = Modifier, width: Int = THUMB_WIDTH, height: Int = THUMB_HEIGHT) {
    val key = "${spec.id}@${width}x$height"
    val bitmap by produceState(GeneratedCache.get(key), key) {
        if (value == null) {
            value = withContext(Dispatchers.Default) { PatternRenderer.render(spec, width, height) }
                .also { GeneratedCache.put(key, it) }
        }
    }
    val image = bitmap
    if (image == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    } else {
        Image(image.asImageBitmap(), contentDescription = spec.title, contentScale = ContentScale.Crop, modifier = modifier)
    }
}
