package com.driftwalls.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.driftwalls.app.ui.about.AboutScreen
import com.driftwalls.app.ui.components.ComingSoon
import com.driftwalls.app.ui.detail.DetailScreen
import com.driftwalls.app.ui.favorites.FavoritesScreen
import com.driftwalls.app.ui.generator.GeneratorScreen
import com.driftwalls.app.ui.home.HomeScreen
import com.driftwalls.app.ui.rotation.RotationScreen
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val label: String, val icon: ImageVector)

private val topLevels = listOf(
    TopLevel(BrowseRoute, BrowseRoute::class, "Browse", Icons.Outlined.Collections),
    TopLevel(GeneratorRoute, GeneratorRoute::class, "Create", Icons.Outlined.AutoAwesome),
    TopLevel(FavoritesRoute, FavoritesRoute::class, "Favourites", Icons.Outlined.FavoriteBorder),
    TopLevel(RotationRoute, RotationRoute::class, "Rotation", Icons.Outlined.Sync),
)

@Composable
fun DriftNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = topLevels.any { top -> destination?.hasRoute(top.routeClass) == true }

    Scaffold(
        // Each screen handles its own insets; this Scaffold only reserves room for the bottom bar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    topLevels.forEach { top ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(top.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(top.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(top.icon, contentDescription = null) },
                            label = { Text(top.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        // Screens draw their own top bars / edge-to-edge content; only the bottom inset is applied here.
        NavHost(
            navController = navController,
            startDestination = BrowseRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<BrowseRoute> {
                HomeScreen(
                    onOpenWallpaper = { navController.navigate(DetailRoute(it)) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    generatedContent = { modifier ->
                        ComingSoon("Generated", "Gradients and patterns arrive with the generator.", modifier)
                    },
                )
            }
            composable<GeneratorRoute> { GeneratorScreen() }
            composable<FavoritesRoute> {
                FavoritesScreen(onOpenWallpaper = { navController.navigate(DetailRoute(it)) })
            }
            composable<RotationRoute> { RotationScreen() }
            composable<AboutRoute> { AboutScreen(onBack = navController::popBackStack) }
            composable<DetailRoute> { DetailScreen(onBack = navController::popBackStack) }
        }
    }
}
