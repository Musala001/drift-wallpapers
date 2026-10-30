package com.driftwalls.app.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.driftwalls.app.ui.components.ComingSoon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenWallpaper: (String) -> Unit, onOpenAbout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drift") },
                actions = {
                    IconButton(onClick = onOpenAbout) { Icon(Icons.Outlined.Info, contentDescription = "About and credits") }
                },
            )
        },
    ) { padding ->
        ComingSoon("Browse", "Public-domain art from The Met and the Art Institute of Chicago lands here in phase 3.", Modifier.padding(padding))
    }
}
