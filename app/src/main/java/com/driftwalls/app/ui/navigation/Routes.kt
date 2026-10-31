package com.driftwalls.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object BrowseRoute
/** [spec] opens the generator on a design, e.g. one tapped in the Generated browse tab. */
@Serializable data class GeneratorRoute(val spec: String? = null)
@Serializable data object FavoritesRoute
@Serializable data object RotationRoute
@Serializable data object AboutRoute
@Serializable data class DetailRoute(val wallpaperId: String)
