package com.driftwalls.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AddToPhotos
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.driftwalls.app.data.wallpaper.WallpaperTarget
import com.driftwalls.app.domain.model.Artwork
import com.driftwalls.app.ui.components.LoadingState
import com.driftwalls.app.ui.components.MessageState
import com.driftwalls.app.ui.components.WallpaperTargetSheet
import com.driftwalls.app.ui.components.doneMessage

@Composable
fun DetailScreen(onBack: () -> Unit, viewModel: DetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val apply by viewModel.apply.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val inRotation by viewModel.isInRotation.collectAsStateWithLifecycle()
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
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.Black,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                DetailState.Loading -> LoadingState()
                DetailState.NotFound -> MessageState(
                    "Not available",
                    "This wallpaper couldn't be found, or it's no longer public domain.",
                )
                is DetailState.Failed -> MessageState("Couldn't load", s.message, actionLabel = "Retry", onAction = viewModel::load)
                is DetailState.Loaded -> ArtworkDetail(
                    artwork = s.artwork,
                    isFavorite = isFavorite,
                    inRotation = inRotation,
                    applying = apply == ApplyState.Applying,
                    onFavorite = viewModel::toggleFavorite,
                    onRotation = viewModel::toggleRotation,
                    onSet = viewModel::setWallpaper,
                )
            }
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f), contentColor = Color.White),
                modifier = Modifier.statusBarsPadding().padding(8.dp),
            ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        }
    }
}

@Composable
private fun ArtworkDetail(
    artwork: Artwork,
    isFavorite: Boolean,
    inRotation: Boolean,
    applying: Boolean,
    onFavorite: () -> Unit,
    onRotation: () -> Unit,
    onSet: (WallpaperTarget) -> Unit,
) {
    var chooseTarget by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(artwork.imageUrl)
                // Show the grid thumbnail (already in memory) while the large image loads.
                .placeholderMemoryCacheKey(artwork.thumbnailUrl)
                .build(),
            contentDescription = artwork.altText ?: artwork.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(artwork.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                listOfNotNull(artwork.artist, artwork.date).joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            // The full credit line, as required for attribution.
            Text(artwork.credit, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Public domain", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                artwork.webUrl?.let { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) {
                        Text("Museum page", color = Color.White)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalIconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                    )
                }
                FilledTonalIconButton(onClick = onRotation) {
                    Icon(
                        if (inRotation) Icons.Outlined.LibraryAddCheck else Icons.Outlined.AddToPhotos,
                        contentDescription = if (inRotation) "Remove from my rotation" else "Add to my rotation",
                    )
                }
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

    if (chooseTarget) WallpaperTargetSheet(onDismiss = { chooseTarget = false }, onSelect = onSet)
}
