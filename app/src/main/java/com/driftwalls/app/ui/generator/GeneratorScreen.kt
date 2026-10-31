package com.driftwalls.app.ui.generator

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.driftwalls.app.domain.generator.Harmony
import com.driftwalls.app.domain.generator.PatternStyle
import com.driftwalls.app.ui.components.TILE_ASPECT
import com.driftwalls.app.ui.components.WallpaperTargetSheet
import com.driftwalls.app.ui.components.doneMessage
import com.driftwalls.app.ui.detail.ApplyState

/** Preview size: sharp on phones without rendering a full-screen bitmap on every tweak. */
private const val PREVIEW_WIDTH = 540
private const val PREVIEW_HEIGHT = 960

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = hiltViewModel()) {
    val spec by viewModel.spec.collectAsStateWithLifecycle()
    val inRotation by viewModel.isInRotation.collectAsStateWithLifecycle()
    val apply by viewModel.apply.collectAsStateWithLifecycle()
    var chooseTarget by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(apply) {
        val message = when (val a = apply) {
            is ApplyState.Done -> a.target.doneMessage()
            is ApplyState.Failed -> a.message
            else -> null
        }
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.applyMessageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                GeneratedImage(
                    spec,
                    Modifier.aspectRatio(TILE_ASPECT).clip(RoundedCornerShape(16.dp)),
                    width = PREVIEW_WIDTH,
                    height = PREVIEW_HEIGHT,
                )
            }
            Text(
                spec.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            ChipRow(PatternStyle.entries, spec.style, { it.label }, viewModel::setStyle)
            ChipRow(Harmony.entries, spec.harmony, { it.label }, viewModel::setHarmony)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalIconButton(onClick = viewModel::reroll) {
                    Icon(Icons.Outlined.Casino, contentDescription = "New colours and shapes")
                }
                FilledTonalIconButton(onClick = viewModel::toggleRotation) {
                    Icon(
                        if (inRotation) Icons.Outlined.LibraryAddCheck else Icons.Outlined.AddToPhotos,
                        contentDescription = if (inRotation) "Remove from my rotation" else "Add to my rotation",
                    )
                }
                val applying = apply == ApplyState.Applying
                Button(onClick = { chooseTarget = true }, enabled = !applying, modifier = Modifier.weight(1f)) {
                    if (applying) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Wallpaper, contentDescription = null)
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(if (applying) "Setting…" else "Set wallpaper")
                }
            }
        }
    }

    if (chooseTarget) WallpaperTargetSheet(onDismiss = { chooseTarget = false }, onSelect = viewModel::setWallpaper)
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}
