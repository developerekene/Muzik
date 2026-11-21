package com.devekene.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devekene.muzik.R
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.PlayerState
import com.devekene.muzik.ui.components.ArtworkImage
import com.devekene.muzik.ui.components.TrackRow

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    tracks: List<AudioTrack>,
    playerState: PlayerState,
    isUserPlaylist: Boolean,
    bottomContentPadding: Dp,
    onBack: () -> Unit,
    onPlayTrack: (AudioTrack) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onAddToQueue: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onShowDetails: (AudioTrack) -> Unit,
    onRemoveFromPlaylist: ((AudioTrack) -> Unit)? = null
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ArtworkImage(
                    artUri = tracks.firstNotNullOfOrNull { it.albumArtUri },
                    contentDescription = title,
                    modifier = Modifier.size(86.dp),
                    cornerRadius = 18.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${tracks.size} ${if (tracks.size == 1) "track" else "tracks"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (tracks.isNotEmpty()) {
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
                    onShowDetails = { onShowDetails(track) },
                    onRemoveFromPlaylist = if (isUserPlaylist && onRemoveFromPlaylist != null) {
                        { onRemoveFromPlaylist(track) }
                    } else null
                )
            }
        } else {
            item {
                Text(
                    text = "No tracks in this collection yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
