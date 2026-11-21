package com.devekene.muzik.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devekene.muzik.R
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.BluetoothSyncState
import com.devekene.muzik.data.model.PlayerState
import com.devekene.muzik.data.model.RepeatModeState
import com.devekene.muzik.ui.components.ArtworkImage
import com.devekene.muzik.ui.components.FormatQualityBadge
import com.devekene.muzik.ui.components.formatDuration

@Composable
fun NowPlayingScreen(
    playerState: PlayerState,
    bluetoothState: BluetoothSyncState,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (AudioTrack) -> Unit,
    onOpenBluetoothSheet: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    onShowDetails: (AudioTrack) -> Unit
) {
    BackHandler(onBack = onCollapse)

    val track = playerState.currentTrack ?: return
    val artScale by animateFloatAsState(
        targetValue = if (playerState.isPlaying) 1f else 0.94f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "art_scale"
    )

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    val totalDuration = playerState.durationMs.coerceAtLeast(1L)
    val liveFraction = (playerState.positionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val sliderValue = if (isScrubbing) scrubProgress else liveFraction
    val displayPositionMs = if (isScrubbing) (scrubProgress * totalDuration).toLong() else playerState.positionMs

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp)
            ) {
                // Top Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.testTag("now_playing_collapse")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse player",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (track.isOfflineAvailable) "OFFLINE PLAYBACK" else "HI-RES STREAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onShowDetails(track) }) {
                        Icon(Icons.Rounded.Info, contentDescription = "Track fidelity info")
                    }
                }

                // Tactile Artwork
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 12.dp)
                ) {
                    ArtworkImage(
                        artUri = track.albumArtUri,
                        contentDescription = stringResource(R.string.cd_album_art),
                        cornerRadius = 26.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1f)
                            .scale(artScale)
                            .shadow(16.dp, RoundedCornerShape(26.dp))
                    )
                }

                // Track Metadata + Favorite + Quality Badge
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${track.artist} • ${track.genre}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(track) }) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite),
                                tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormatQualityBadge(
                            formatLabel = track.qualitySummary,
                            isLossless = track.isLossless
                        )
                        if (track.isOfflineAvailable) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.OfflinePin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("OFFLINE", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seek Scrubber
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            isScrubbing = true
                            scrubProgress = it
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            onSeekTo((scrubProgress * totalDuration).toLong())
                        },
                        enabled = playerState.durationMs > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("now_playing_slider")
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatDuration(displayPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(playerState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Transport Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.testTag("now_playing_shuffle")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.cd_shuffle),
                                tint = if (playerState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("now_playing_previous")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_previous),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onTogglePlayPause)
                                .testTag("now_playing_play_pause")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(
                                        if (playerState.isPlaying) R.string.cd_pause else R.string.cd_play
                                    ),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("now_playing_next")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.cd_next),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier.testTag("now_playing_repeat")
                        ) {
                            val icon = if (playerState.repeatMode == RepeatModeState.ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.cd_repeat),
                                tint = if (playerState.repeatMode != RepeatModeState.OFF) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Audio Output, EQ, and Queue Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val activeDevice = bluetoothState.activeDevice
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (activeDevice?.isBluetooth == true) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenBluetoothSheet)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeDevice?.isBluetooth == true) {
                                        Icons.Rounded.BluetoothConnected
                                    } else {
                                        Icons.Rounded.Speaker
                                    },
                                    contentDescription = stringResource(R.string.cd_bluetooth),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeDevice?.name ?: "Speaker",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row {
                            IconButton(onClick = onOpenEqualizer) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = stringResource(R.string.cd_equalizer)
                                )
                            }
                            IconButton(onClick = onOpenQueue) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = stringResource(R.string.cd_queue)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
