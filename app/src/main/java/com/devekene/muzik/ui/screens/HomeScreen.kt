package com.devekene.muzik.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devekene.muzik.R
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.BluetoothSyncState
import com.devekene.muzik.data.model.CategoryGroup
import com.devekene.muzik.data.model.PlayerState
import com.devekene.muzik.data.model.PlaylistSummary
import com.devekene.muzik.ui.components.ArtworkImage
import com.devekene.muzik.ui.components.BluetoothOutputPill
import com.devekene.muzik.ui.components.FormatQualityBadge
import com.devekene.muzik.ui.components.MuzikWordmarkLogo
import com.devekene.muzik.ui.components.TrackRow
import com.devekene.muzik.ui.viewmodel.SearchResultsUi

@Composable
fun HomeScreen(
    allTracks: List<AudioTrack>,
    recentlyPlayed: List<AudioTrack>,
    favoriteTracks: List<AudioTrack>,
    losslessTracks: List<AudioTrack>,
    playlists: List<PlaylistSummary>,
    genres: List<CategoryGroup>,
    searchQuery: String,
    searchResults: SearchResultsUi,
    playerState: PlayerState,
    bluetoothState: BluetoothSyncState,
    isScanning: Boolean,
    hasAudioPermission: Boolean,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onSearchQueryChange: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenBluetoothSheet: () -> Unit,
    onScanDeviceAudio: () -> Unit,
    onImportFiles: () -> Unit,
    onOpenAddStreamDialog: () -> Unit,
    onPlayTrack: (AudioTrack, List<AudioTrack>) -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onShowTrackDetails: (AudioTrack) -> Unit,
    onOpenPlaylistsTab: () -> Unit,
    onOpenSmartCollection: (title: String, filterType: String) -> Unit,
    onOpenPlaylist: (PlaylistSummary) -> Unit,
    onOpenGenre: (CategoryGroup) -> Unit,
    onOpenArtist: (CategoryGroup) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Editorial Top Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MuzikWordmarkLogo()
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.tagline_tailored),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BluetoothOutputPill(
                        bluetoothState = bluetoothState,
                        onClick = onOpenBluetoothSheet
                    )
                    IconButton(
                        onClick = onOpenMenu,
                        modifier = Modifier.testTag("hamburger_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.cd_open_menu),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        // Unified Search Bar + Quick Access Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar")
                )

                // Quick Access to Playlists & Recently Played
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = onOpenPlaylistsTab,
                            label = { Text("Playlists (${playlists.size})") },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("quick_access_playlists")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onOpenSmartCollection("Recently Played", "RECENT") },
                            label = { Text("Recently Played (${recentlyPlayed.size})") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("quick_access_recent")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onOpenSmartCollection("Favorites", "FAVORITES") },
                            label = { Text("Favorites (${favoriteTracks.size})") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { onOpenSmartCollection("Hi-Fi Lossless", "LOSSLESS") },
                            label = { Text("Hi-Fi (${losslessTracks.size})") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.HighQuality,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }
        }

        // Live Search Results Mode
        if (searchQuery.isNotBlank()) {
            val hasAnyResult = searchResults.tracks.isNotEmpty() ||
                searchResults.playlists.isNotEmpty() ||
                searchResults.artists.isNotEmpty() ||
                searchResults.genres.isNotEmpty()

            if (!hasAnyResult) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No matches for \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Try searching by track title, artist, genre, or format (FLAC, MP3).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                if (searchResults.playlists.isNotEmpty()) {
                    item {
                        Text("Matching Playlists", style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(searchResults.playlists, key = { it.id }) { pl ->
                                PlaylistQuickCard(playlist = pl, onClick = { onOpenPlaylist(pl) })
                            }
                        }
                    }
                }

                if (searchResults.artists.isNotEmpty() || searchResults.genres.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchResults.artists, key = { "a_${it.name}" }) { artist ->
                                AssistChip(
                                    onClick = { onOpenArtist(artist) },
                                    label = { Text("Artist: ${artist.name}") }
                                )
                            }
                            items(searchResults.genres, key = { "g_${it.name}" }) { genre ->
                                AssistChip(
                                    onClick = { onOpenGenre(genre) },
                                    label = { Text("Genre: ${genre.name}") }
                                )
                            }
                        }
                    }
                }

                if (searchResults.tracks.isNotEmpty()) {
                    item {
                        Text(
                            "Tracks (${searchResults.tracks.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(searchResults.tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            isCurrentTrack = playerState.currentTrack?.id == track.id,
                            isPlaying = playerState.isPlaying,
                            onClick = { onPlayTrack(track, searchResults.tracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            onAddToQueue = { onAddToQueue(track) },
                            onAddToPlaylist = { onAddToPlaylist(track) },
                            onShowDetails = { onShowTrackDetails(track) }
                        )
                    }
                }
            }
        } else {
            // Empty Library Onboarding Card OR Offline Summary Strip
            if (allTracks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_muzik_logo_1795264893809),
                                    contentDescription = stringResource(R.string.cd_app_icon),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .testTag("home_app_icon")
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.empty_library_title),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.empty_library_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onScanDeviceAudio,
                                    enabled = !isScanning,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scan_device_audio_button")
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (hasAudioPermission) "Scan Audio" else "Grant & Scan")
                                }
                                OutlinedButton(
                                    onClick = onImportFiles,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("import_audio_files_button")
                                ) {
                                    Icon(
                                        Icons.Rounded.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Files")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onOpenAddStreamDialog,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Rounded.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.add_hires_stream))
                            }
                        }
                    }
                }
            } else {
                // Offline Ready Summary Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_muzik_logo_1795264893809),
                                contentDescription = stringResource(R.string.cd_app_icon),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .testTag("home_app_icon")
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${allTracks.count { it.isOfflineAvailable }} Offline Tracks Ready",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${losslessTracks.size} Hi-Res lossless • ${genres.size} genres • Gapless enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onShuffleAll, modifier = Modifier.testTag("home_shuffle_all")) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle library",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onImportFiles) {
                                Icon(
                                    Icons.Rounded.FolderOpen,
                                    contentDescription = stringResource(R.string.cd_import_audio)
                                )
                            }
                        }
                    }
                }
            }

            // Recently Played Horizontal Strip
            if (recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recently Played",
                        actionLabel = "See all",
                        onAction = { onOpenSmartCollection("Recently Played", "RECENT") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recentlyPlayed.take(10), key = { it.id }) { track ->
                            RecentTrackCard(
                                track = track,
                                isPlayingNow = playerState.currentTrack?.id == track.id && playerState.isPlaying,
                                onClick = { onPlayTrack(track, recentlyPlayed) }
                            )
                        }
                    }
                }
            }

            // Playlists Quick Access Cards
            item {
                SectionHeader(
                    title = "Quick Playlists",
                    actionLabel = "Manage",
                    onAction = onOpenPlaylistsTab
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        SmartQuickPlaylistCard(
                            title = "Favorites",
                            subtitle = "${favoriteTracks.size} tracks",
                            icon = Icons.Rounded.Favorite,
                            onClick = { onOpenSmartCollection("Favorites", "FAVORITES") }
                        )
                    }
                    item {
                        SmartQuickPlaylistCard(
                            title = "Hi-Fi Lossless",
                            subtitle = "${losslessTracks.size} tracks",
                            icon = Icons.Rounded.HighQuality,
                            onClick = { onOpenSmartCollection("Hi-Fi Lossless", "LOSSLESS") }
                        )
                    }
                    items(playlists, key = { it.id }) { pl ->
                        PlaylistQuickCard(playlist = pl, onClick = { onOpenPlaylist(pl) })
                    }
                }
            }

            // Auto-categorized Genres Strip
            if (genres.isNotEmpty()) {
                item {
                    SectionHeader(title = "Genres", actionLabel = null, onAction = {})
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(genres, key = { it.name }) { genre ->
                            AssistChip(
                                onClick = { onOpenGenre(genre) },
                                label = { Text("${genre.name} (${genre.trackCount})") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }

            // Recent / All Library Preview
            if (allTracks.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Your Library", style = MaterialTheme.typography.titleLarge)
                        Row {
                            IconButton(onClick = onScanDeviceAudio) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cd_scan_library))
                            }
                            IconButton(onClick = onOpenAddStreamDialog) {
                                Icon(Icons.Rounded.AddLink, contentDescription = stringResource(R.string.add_hires_stream))
                            }
                        }
                    }
                }
                items(allTracks.take(15), key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        isCurrentTrack = playerState.currentTrack?.id == track.id,
                        isPlaying = playerState.isPlaying,
                        onClick = { onPlayTrack(track, allTracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) },
                        onShowDetails = { onShowTrackDetails(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun RecentTrackCard(
    track: AudioTrack,
    isPlayingNow: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(152.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                ArtworkImage(
                    artUri = track.albumArtUri,
                    contentDescription = track.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp),
                    cornerRadius = 12.dp
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlayingNow) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            FormatQualityBadge(formatLabel = track.formatLabel, isLossless = track.isLossless)
        }
    }
}

@Composable
private fun SmartQuickPlaylistCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlaylistQuickCard(
    playlist: PlaylistSummary,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            ArtworkImage(
                artUri = playlist.coverArtUri,
                contentDescription = playlist.name,
                modifier = Modifier.size(40.dp),
                cornerRadius = 10.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
