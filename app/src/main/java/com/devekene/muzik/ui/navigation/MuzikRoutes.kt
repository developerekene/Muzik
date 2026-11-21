package com.devekene.muzik.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object LibraryRoute

@Serializable
data object PlaylistsRoute

@Serializable
data object StudioRoute

@Serializable
data class CollectionDetailRoute(
    val title: String,
    val subtitle: String,
    val filterType: String,
    val filterKey: String = "",
    val playlistId: Long = -1L
)
