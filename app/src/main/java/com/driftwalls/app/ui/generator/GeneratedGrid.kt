package com.driftwalls.app.ui.generator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.driftwalls.app.domain.generator.GeneratorSpec
import com.driftwalls.app.ui.components.TILE_ASPECT
import kotlin.random.Random

private const val GRID_SIZE = 48

/** The "Abstract / Generated" browse tab: a batch of random designs drawn on the device. */
@Composable
fun GeneratedGrid(onOpen: (GeneratorSpec) -> Unit, modifier: Modifier = Modifier) {
    var batch by rememberSaveable { mutableLongStateOf(Random.nextLong()) }
    val specs = remember(batch) {
        val random = Random(batch)
        List(GRID_SIZE) { GeneratorSpec.random(random) }.distinctBy { it.id }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Drawn on your phone. Tap one to tweak it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { batch = Random.nextLong() }) {
                    Icon(Icons.Outlined.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Shuffle")
                }
            }
        }
        items(specs, key = { it.id }) { spec ->
            GeneratedImage(
                spec,
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(TILE_ASPECT)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen(spec) },
            )
        }
    }
}
