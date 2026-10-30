package com.driftwalls.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object BrowseRoute
@Serializable data object GeneratorRoute
@Serializable data object FavoritesRoute
@Serializable data object RotationRoute
@Serializable data object AboutRoute
@Serializable data class DetailRoute(val wallpaperId: String)
