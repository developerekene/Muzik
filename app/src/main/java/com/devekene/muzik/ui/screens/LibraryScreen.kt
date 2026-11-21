package com.devekene.muzik.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.CategoryGroup
import com.devekene.muzik.data.model.LibraryTab
import com.devekene.muzik.data.model.PlayerState
import com.devekene.muzik.ui.components.CategoryCard
import com.devekene.muzik.ui.components.TrackRow

@Composable
fun LibraryScreen(
    selectedTab: LibraryTab,
    allTracks: List<AudioTrack>,
    losslessTracks: List<AudioTrack>,
    artists: List<CategoryGroup>,
    albums: List<CategoryGroup>,
    genres: List<CategoryGroup>,
    playerState: PlayerState,
    bottomContentPadding: Dp,
    onSelectTab: (LibraryTab) -> Unit,
    onScanDeviceAudio: () -> Unit,
    onImportFiles: () -> Unit,
    onAddStreamUrl: () -> Unit,
    onPlayTrack: (AudioTrack, List<AudioTrack>) -> Unit,
    onPlayQueue: (List<AudioTrack>, Boolean) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onShowTrackDetails: (AudioTrack) -> Unit,
    onOpenArtist: (CategoryGroup) -> Unit,
    onOpenAlbum: (CategoryGroup) -> Unit,
    onOpenGenre: (CategoryGroup) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header + Utility Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text("Library", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${allTracks.size} tracks • ${artists.size} artists • ${genres.size} genres",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onScanDeviceAudio, modifier = Modifier.testTag("library_scan_button")) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Scan local audio")
                }
                IconButton(onClick = onImportFiles, modifier = Modifier.testTag("library_import_button")) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Import audio files")
                }
                IconButton(onClick = onAddStreamUrl, modifier = Modifier.testTag("library_stream_button")) {
                    Icon(Icons.Rounded.AddLink, contentDescription = "Add stream URL")
                }
            }
        }

        // Category Filter Tabs (Tracks / Artists / Albums / Genres / Hi-Fi)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.entries) { tab ->
                FilterChip(
                    selected = tab == selectedTab,
                    onClick = { onSelectTab(tab) },
                    label = { Text(tab.label) },
                    modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            LibraryTab.TRACKS -> {
                TrackListContent(
                    tracks = allTracks,
                    emptyMessage = "No tracks indexed yet. Scan your device or import offline FLAC/WAV/MP3 files.",
                    playerState = playerState,
                    bottomContentPadding = bottomContentPadding,
                    onPlayAll = { onPlayQueue(allTracks, false) },
                    onShuffleAll = { onPlayQueue(allTracks, true) },
                    onPlayTrack = { onPlayTrack(it, allTracks) },
                    onToggleFavorite = onToggleFavorite,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowTrackDetails = onShowTrackDetails
                )
            }
            LibraryTab.HIFI -> {
                TrackListContent(
                    tracks = losslessTracks,
                    emptyMessage = "No lossless Hi-Fi tracks (FLAC, WAV, ALAC, 48kHz+) found yet.",
                    playerState = playerState,
                    bottomContentPadding = bottomContentPadding,
                    onPlayAll = { onPlayQueue(losslessTracks, false) },
                    onShuffleAll = { onPlayQueue(losslessTracks, true) },
                    onPlayTrack = { onPlayTrack(it, losslessTracks) },
                    onToggleFavorite = onToggleFavorite,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onShowTrackDetails = onShowTrackDetails
                )
            }
            LibraryTab.ARTISTS -> {
                CategoryGridContent(
                    groups = artists,
                    emptyMessage = "Artists will automatically appear as soon as local audio metadata is scanned.",
                    bottomContentPadding = bottomContentPadding,
                    onClickGroup = onOpenArtist
                )
            }
            LibraryTab.ALBUMS -> {
                CategoryGridContent(
                    groups = albums,
                    emptyMessage = "Albums will automatically group here from your audio metadata.",
                    bottomContentPadding = bottomContentPadding,
                    onClickGroup = onOpenAlbum
                )
            }
            LibraryTab.GENRES -> {
                CategoryGridContent(
                    groups = genres,
                    emptyMessage = "Genres will automatically categorize from audio tags and formats.",
                    bottomContentPadding = bottomContentPadding,
                    onClickGroup = onOpenGenre
                )
            }
        }
    }
}

@Composable
private fun TrackListContent(
    tracks: List<AudioTrack>,
    emptyMessage: String,
    playerState: PlayerState,
    bottomContentPadding: Dp,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onPlayTrack: (AudioTrack) -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onShowTrackDetails: (AudioTrack) -> Unit
) {
    if (tracks.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All")
                }
                OutlinedButton(onClick = onShuffleAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
        }
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                isCurrentTrack = playerState.currentTrack?.id == track.id,
                isPlaying = playerState.isPlaying,
                onClick = { onPlayTrack(track) },
                onToggleFavorite = { onToggleFavorite(track) },
                onAddToQueue = { onAddToQueue(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
                onShowDetails = { onShowTrackDetails(track) }
            )
        }
    }
}

@Composable
private fun CategoryGridContent(
    groups: List<CategoryGroup>,
    emptyMessage: String,
    bottomContentPadding: Dp,
    onClickGroup: (CategoryGroup) -> Unit
) {
    if (groups.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = bottomContentPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups, key = { it.name }) { group ->
                CategoryCard(
                    group = group,
                    onClick = { onClickGroup(group) }
                )
            }
        }
    }
}
