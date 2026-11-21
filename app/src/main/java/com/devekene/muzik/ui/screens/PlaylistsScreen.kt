package com.devekene.muzik.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devekene.muzik.data.model.PlaylistSummary
import com.devekene.muzik.ui.components.ArtworkImage

@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistSummary>,
    favoritesCount: Int,
    losslessCount: Int,
    offlineCount: Int,
    recentCount: Int,
    bottomContentPadding: Dp,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onOpenPlaylist: (PlaylistSummary) -> Unit,
    onOpenSmartCollection: (title: String, filterType: String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Playlists", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Smart collections & custom mixes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New")
                }
            }
        }

        item {
            Text(
                "SMART COLLECTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmartPlaylistRow(
                    title = "Favorites",
                    subtitle = "$favoritesCount loved tracks",
                    icon = Icons.Rounded.Favorite,
                    onClick = { onOpenSmartCollection("Favorites", "FAVORITES") }
                )
                SmartPlaylistRow(
                    title = "Hi-Fi Lossless",
                    subtitle = "$losslessCount studio-grade tracks",
                    icon = Icons.Rounded.HighQuality,
                    onClick = { onOpenSmartCollection("Hi-Fi Lossless", "LOSSLESS") }
                )
                SmartPlaylistRow(
                    title = "Offline Ready",
                    subtitle = "$offlineCount local tracks playable anywhere",
                    icon = Icons.Rounded.OfflinePin,
                    onClick = { onOpenSmartCollection("Offline Ready", "OFFLINE") }
                )
                SmartPlaylistRow(
                    title = "Recently Played",
                    subtitle = "$recentCount recent listens",
                    icon = Icons.Rounded.History,
                    onClick = { onOpenSmartCollection("Recently Played", "RECENT") }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "YOUR PLAYLISTS (${playlists.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (playlists.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "No custom playlists yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Create a playlist to group your favorite lossless albums or workout tracks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(playlists, key = { it.id }) { pl ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenPlaylist(pl) }
                        .testTag("playlist_row_${pl.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        ArtworkImage(
                            artUri = pl.coverArtUri,
                            contentDescription = pl.name,
                            modifier = Modifier.size(54.dp),
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.titleMedium)
                            val desc = pl.description.ifBlank { "${pl.trackCount} tracks" }
                            Text(
                                text = if (pl.description.isNotBlank()) "${pl.trackCount} tracks • $desc" else desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
