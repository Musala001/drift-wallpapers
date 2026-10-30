package com.driftwalls.app.ui.favorites

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.driftwalls.app.ui.components.ComingSoon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(onOpenWallpaper: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Favourites") }) }) { padding ->
        ComingSoon("Favourites", "Your saved wallpapers will appear here.", Modifier.padding(padding))
    }
}
