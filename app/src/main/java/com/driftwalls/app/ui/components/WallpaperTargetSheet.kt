package com.driftwalls.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driftwalls.app.data.wallpaper.WallpaperTarget

/** Asks where to set the wallpaper: home, lock or both. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperTargetSheet(onDismiss: () -> Unit, onSelect: (WallpaperTarget) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Set as wallpaper on", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        WallpaperTarget.entries.forEach { target ->
            ListItem(
                headlineContent = { Text(target.label) },
                modifier = Modifier.fillMaxWidth().clickable {
                    onDismiss()
                    onSelect(target)
                },
            )
        }
        Spacer(Modifier.navigationBarsPadding().padding(bottom = 16.dp))
    }
}

/** Snackbar text for a finished apply. */
fun WallpaperTarget.doneMessage() = "Wallpaper set on ${label.lowercase()}"
