package com.devekene.muzik.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devekene.muzik.R
import com.devekene.muzik.data.model.AudioOutputType
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.BluetoothSyncState
import com.devekene.muzik.data.model.CategoryGroup
import com.devekene.muzik.data.model.PlayerState
import java.util.Locale

@Composable
fun MuzikWordmarkLogo(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displaySmall
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = "Muzik"
            }
            .testTag("muzik_wordmark_logo")
    ) {
        Text(
            text = "Muz",
            style = textStyle,
            color = textColor
        )
        Canvas(
            modifier = Modifier
                .padding(start = 1.dp, end = 1.dp, bottom = 5.dp)
                .size(width = 16.dp, height = 27.dp)
        ) {
            val w = size.width
            val h = size.height
            val stemX = w * 0.56f
            val headCenter = Offset(w * 0.34f, h * 0.80f)
            val headWidth = w * 0.62f
            val headHeight = h * 0.25f

            // Tilted oval notehead replacing the base of 'i'
            rotate(degrees = -22f, pivot = headCenter) {
                drawOval(
                    color = accentColor,
                    topLeft = Offset(
                        headCenter.x - headWidth / 2f,
                        headCenter.y - headHeight / 2f
                    ),
                    size = Size(headWidth, headHeight)
                )
            }

            // Crisp vertical stem
            drawLine(
                color = accentColor,
                start = Offset(stemX, headCenter.y - headHeight * 0.15f),
                end = Offset(stemX, h * 0.12f),
                strokeWidth = 2.8.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Sweeping eighth-note flag
            val flagPath = Path().apply {
                moveTo(stemX, h * 0.12f)
                cubicTo(
                    w * 1.05f, h * 0.22f,
                    w * 1.02f, h * 0.44f,
                    w * 0.78f, h * 0.52f
                )
            }
            drawPath(
                path = flagPath,
                color = accentColor,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "k",
            style = textStyle,
            color = textColor
        )
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "LIVE"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Composable
fun ArtworkImage(
    artUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    if (artUri.isNullOrBlank()) {
        Image(
            painter = painterResource(id = R.drawable.img_default_album_art_1795264895177),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artUri)
                .crossfade(true)
                .error(R.drawable.img_default_album_art_1795264895177)
                .fallback(R.drawable.img_default_album_art_1795264895177)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        )
    }
}

@Composable
fun FormatQualityBadge(
    formatLabel: String,
    isLossless: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isLossless) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isLossless) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = if (isLossless) "HI-RES $formatLabel" else formatLabel,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun BluetoothOutputPill(
    bluetoothState: BluetoothSyncState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = bluetoothState.activeDevice
    val isBt = active?.isBluetooth == true
    val containerColor by animateColorAsState(
        targetValue = if (isBt) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "bt_pill_bg"
    )
    val contentColor = if (isBt) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon = when (active?.type) {
        AudioOutputType.BLUETOOTH_A2DP, AudioOutputType.BLUETOOTH_LE -> Icons.Rounded.BluetoothConnected
        AudioOutputType.USB_DAC -> Icons.Rounded.Usb
        AudioOutputType.WIRED -> Icons.Rounded.Headphones
        else -> Icons.Rounded.Speaker
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = CircleShape,
        border = BorderStroke(
            1.dp,
            if (isBt) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("bluetooth_output_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.cd_bluetooth),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = active?.name ?: "Speaker",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(104.dp)
            )
        }
    }
}

@Composable
fun TrackRow(
    track: AudioTrack,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val rowContainer = if (isCurrentTrack) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = rowContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCurrentTrack) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("track_row_${track.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                ArtworkImage(
                    artUri = track.albumArtUri,
                    contentDescription = stringResource(R.string.cd_album_art),
                    modifier = Modifier.size(52.dp),
                    cornerRadius = 12.dp
                )
                if (isCurrentTrack) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FormatQualityBadge(
                        formatLabel = track.formatLabel,
                        isLossless = track.isLossless
                    )
                    if (track.isOfflineAvailable) {
                        Icon(
                            imageVector = Icons.Rounded.OfflinePin,
                            contentDescription = "Available offline",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = formatDuration(track.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_button_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("track_menu_${track.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Track options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to Up Next") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Audio Fidelity & Info") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShowDetails()
                        }
                    )
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from Playlist") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromPlaylist()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    group: CategoryGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("category_card_${group.name}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box {
                ArtworkImage(
                    artUri = group.coverArtUri,
                    contentDescription = group.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp),
                    cornerRadius = 12.dp
                )
                if (group.hasLossless) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "HI-RES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = group.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val progress = if (playerState.durationMs > 0L) {
        (playerState.positionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                ArtworkImage(
                    artUri = track.albumArtUri,
                    contentDescription = stringResource(R.string.cd_album_art),
                    modifier = Modifier.size(46.dp),
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        FormatQualityBadge(
                            formatLabel = track.formatLabel,
                            isLossless = track.isLossless
                        )
                    }
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            if (playerState.isPlaying) R.string.cd_pause else R.string.cd_play
                        ),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_next),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }
    }
}
