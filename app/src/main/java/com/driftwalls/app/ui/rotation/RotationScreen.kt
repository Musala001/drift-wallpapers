package com.driftwalls.app.ui.rotation

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.driftwalls.app.data.local.RotationHistoryItem
import com.driftwalls.app.data.rotation.RotationInterval
import com.driftwalls.app.data.rotation.RotationSettings
import com.driftwalls.app.data.rotation.RotationSource
import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.generator.GeneratorSpec
import com.driftwalls.app.domain.model.Category
import com.driftwalls.app.domain.rotation.RotationOrder
import com.driftwalls.app.ui.components.LoadingState
import com.driftwalls.app.ui.components.TILE_ASPECT
import com.driftwalls.app.ui.generator.GeneratedImage
import com.driftwalls.app.wallpaper.DriftWallpaperService
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationScreen(viewModel: RotationViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favoriteCount by viewModel.favoriteCount.collectAsStateWithLifecycle()
    val pickCount by viewModel.pickCount.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val rotateNow by viewModel.rotateNow.collectAsStateWithLifecycle()
    val nextRun by viewModel.nextRunMillis.collectAsStateWithLifecycle()
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

            SectionTitle("Automatic")
            SwitchRow("Change automatically", nextRunText(s.enabled, nextRun), s.enabled) { on ->
                viewModel.update { it.copy(enabled = on) }
            }
            if (s.enabled) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RotationInterval.entries) { interval ->
                        FilterChip(
                            selected = interval == s.interval,
                            onClick = { viewModel.update { it.copy(interval = interval) } },
                            label = { Text(interval.label) },
                        )
                    }
                }
                SwitchRow("Wi-Fi only", "Download new museum images only on unmetered networks", s.wifiOnly) { on ->
                    viewModel.update { it.copy(wifiOnly = on) }
                }
            }

            SectionTitle("Live wallpaper")
            LiveWallpaperRow(viewModel::isLiveActive)

            SectionTitle("Source")
            SourceRow(RotationSource.FAVORITES, "Favourites", "$favoriteCount wallpapers", s) { viewModel.update { it.copy(source = RotationSource.FAVORITES) } }
            SourceRow(RotationSource.PICKS, "My rotation", "$pickCount wallpapers you added", s) { viewModel.update { it.copy(source = RotationSource.PICKS) } }
            SourceRow(RotationSource.CATEGORY, "Category", s.category.displayName, s) { viewModel.update { it.copy(source = RotationSource.CATEGORY) } }
            if (s.source == RotationSource.CATEGORY) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Category.entries) { category ->
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

private fun nextRunText(enabled: Boolean, nextRun: Long?): String = when {
    !enabled -> "Off"
    nextRun == null -> "Scheduling…"
    // WorkManager may run a little later (Doze, battery and network constraints).
    else -> "Next change around " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(nextRun))
}

@Composable
internal fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        // The row handles the toggle so the whole line is one touch target.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun LiveWallpaperRow(isActive: () -> Boolean) {
    val context = LocalContext.current
    var active by remember { mutableStateOf(isActive()) }
    // The user returns from the system picker; check again.
    LifecycleResumeEffect(Unit) {
        active = isActive()
        onPauseOrDispose { }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (active) "Live wallpaper is on" else "Use the live wallpaper", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Crossfades between wallpapers and pans as you swipe home screens. Double-tap it for the next one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        OutlinedButton(onClick = { openLiveWallpaperPicker(context) }) { Text(if (active) "Change" else "Set up") }
    }
}

private fun openLiveWallpaperPicker(context: Context) {
    val preview = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, DriftWallpaperService::class.java))
    try {
        context.startActivity(preview)
    } catch (_: ActivityNotFoundException) {
        // Some launchers only offer the generic chooser.
        try {
            context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        } catch (_: ActivityNotFoundException) {
        }
    }
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
    val generated = remember(item.wallpaperId) { GeneratorSpec.parseId(item.wallpaperId) }
    if (generated != null) {
        GeneratedImage(generated, modifier)
        return
    }
    AsyncImage(
        model = item.thumbnailUrl,
        contentDescription = item.title,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}
