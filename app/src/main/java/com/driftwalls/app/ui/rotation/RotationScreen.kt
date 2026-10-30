package com.driftwalls.app.ui.rotation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.driftwalls.app.data.local.RotationHistoryItem
import com.driftwalls.app.data.rotation.RotationSettings
import com.driftwalls.app.data.rotation.RotationSource
import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.rotation.RotationOrder
import com.driftwalls.app.ui.components.LoadingState
import com.driftwalls.app.ui.components.TILE_ASPECT

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationScreen(viewModel: RotationViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val pickCount by viewModel.pickCount.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val rotateNow by viewModel.rotateNow.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(rotateNow) {
        (rotateNow as? RotateNowState.Message)?.let {
            snackbar.showSnackbar(it.text)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Rotation") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = settings
        if (s == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = viewModel::next,
                enabled = rotateNow != RotateNowState.Running,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                if (rotateNow == RotateNowState.Running) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.SkipNext, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Next wallpaper now")
            }

            RecentlyShown(history)

            SectionTitle("Source")
            SourceRow(RotationSource.FAVORITES, "Favourites", "$favoriteCount wallpapers", s) { viewModel.update { it.copy(source = RotationSource.FAVORITES) } }
            SourceRow(RotationSource.PICKS, "My rotation", "$pickCount wallpapers you added", s) { viewModel.update { it.copy(source = RotationSource.PICKS) } }
            SourceRow(RotationSource.CATEGORY, "Category", s.category.displayName, s) { viewModel.update { it.copy(source = RotationSource.CATEGORY) } }
            if (s.source == RotationSource.CATEGORY) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rotationCategories) { category ->
                        FilterChip(
                            selected = category == s.category,
                            onClick = { viewModel.update { it.copy(category = category) } },
                            label = { Text(category.displayName) },
                        )
                    }
                }
            }

            SectionTitle("Order")
            Choices(RotationOrder.entries, s.order, { it.label }) { order -> viewModel.update { it.copy(order = order) } }

            SectionTitle("Apply to")
            Choices(WallpaperTarget.entries, s.target, { it.shortLabel }) { target -> viewModel.update { it.copy(target = target) } }
        }
    }
}

/** Categories offered as a rotation source. */
internal val rotationCategories = Category.entries.filterNot { it.isGenerated }

private val WallpaperTarget.shortLabel
    get() = when (this) {
        WallpaperTarget.HOME -> "Home"
        WallpaperTarget.LOCK -> "Lock"
        WallpaperTarget.BOTH -> "Both"
    }

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
    )
}

@Composable
private fun SourceRow(source: RotationSource, title: String, detail: String, settings: RotationSettings, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = settings.source == source, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> Choices(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Composable
private fun RecentlyShown(history: List<RotationHistoryItem>) {
    if (history.isEmpty()) return
    SectionTitle("Recently shown")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(history, key = { "${it.wallpaperId}@${it.appliedAt}" }) { item ->
            Box(
                Modifier
                    .width(72.dp)
                    .aspectRatio(TILE_ASPECT)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                HistoryThumbnail(item, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(item: RotationHistoryItem, modifier: Modifier) {
    AsyncImage(
        model = item.thumbnailUrl,
        contentDescription = item.title,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
