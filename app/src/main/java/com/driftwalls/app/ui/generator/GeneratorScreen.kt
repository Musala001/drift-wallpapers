package com.driftwalls.app.ui.generator

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
fun GeneratorScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Create") }) }) { padding ->
        ComingSoon("Create", "Gradient and pattern generator arrives in phase 7.", Modifier.padding(padding))
    }
}
