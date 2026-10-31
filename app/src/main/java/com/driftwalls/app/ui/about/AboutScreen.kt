package com.driftwalls.app.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.driftwalls.app.BuildConfig
import com.driftwalls.app.domain.model.Museum

private data class Source(val name: String, val rule: String, val licence: String, val url: String)

private val sources = listOf(
    Source(
        Museum.MET.displayName,
        "Only objects the Met marks as public domain, with an image.",
        "Open Access images, CC0.",
        "https://www.metmuseum.org/about-the-met/policies-and-documents/open-access",
    ),
    Source(
        Museum.AIC.displayName,
        "Only artworks the museum marks as public domain, with an image.",
        "Public-domain images; collection data CC0.",
        "https://www.artic.edu/open-access/open-access-images",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & credits") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Drift ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text(
                "Drift only shows images that are free to use: public-domain works from museum open-access " +
                    "collections, and wallpapers drawn on your phone. Every museum image keeps its credit line " +
                    "on the detail screen.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Heading("Image sources")
            sources.forEach { source ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(source.name, style = MaterialTheme.typography.titleSmall)
                    Body(source.rule)
                    Body(source.licence)
                    TextButton(onClick = { uriHandler.openUri(source.url) }) {
                        Text("Open-access policy")
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Generated wallpapers", style = MaterialTheme.typography.titleSmall)
                Body("Gradients and patterns drawn on the device from a random seed. No licence needed.")
            }

            Heading("Privacy")
            Body(
                "Drift has no account and no analytics. It only contacts the two museums, to search their collections and " +
                    "download images from their servers. Favourites, rotation lists and history stay on this phone.",
            )

            Heading("Being a good API citizen")
            Body(
                "Requests to the Art Institute of Chicago identify the app and stay under one per second; requests " +
                    "to the Met stay far below their limit. Responses are cached for an hour.",
            )
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
