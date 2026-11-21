package com.devekene.muzik.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.devekene.muzik.data.model.AudioOutputType
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.BluetoothSyncState
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.data.model.PlaylistSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSyncBottomSheet(
    bluetoothState: BluetoothSyncState,
    eqPresets: List<EqPreset>,
    onDismiss: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onToggleAutoEq: (Boolean, String) -> Unit,
    onRefresh: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.BluetoothConnected,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Bluetooth & Audio Sync",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh audio outputs")
                }
            }

            bluetoothState.lastSyncEventMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "ACTIVE & CONNECTED OUTPUTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (bluetoothState.connectedDevices.isEmpty()) {
                Text(
                    text = "Device Speaker active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                bluetoothState.connectedDevices.forEach { device ->
                    val icon = when (device.type) {
                        AudioOutputType.BLUETOOTH_A2DP, AudioOutputType.BLUETOOTH_LE -> Icons.Rounded.Bluetooth
                        AudioOutputType.USB_DAC -> Icons.Rounded.Usb
                        AudioOutputType.WIRED -> Icons.Rounded.Headphones
                        AudioOutputType.SPEAKER -> Icons.Rounded.Speaker
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (device.isActive) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (device.isActive) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(icon, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${device.type.label} • ${
                                        device.supportedSampleRates.maxOrNull()?.let { "${it / 1000} kHz max" } ?: "48 kHz"
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (device.isActive) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Active output",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-resume on Bluetooth connect", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Continue playback seamlessly when wireless headphones pair",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bluetoothState.autoResumeOnConnect,
                    onCheckedChange = onToggleAutoResume
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bluetooth Auto-EQ Profile", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Apply ${bluetoothState.bluetoothEqPresetName} automatically over Bluetooth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bluetoothState.autoEqOnBluetooth,
                    onCheckedChange = { enabled ->
                        onToggleAutoEq(enabled, bluetoothState.bluetoothEqPresetName)
                    }
                )
            }

            if (bluetoothState.autoEqOnBluetooth && eqPresets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    eqPresets.take(3).forEach { preset ->
                        FilterChip(
                            selected = preset.name == bluetoothState.bluetoothEqPresetName,
                            onClick = { onToggleAutoEq(true, preset.name) },
                            label = { Text(preset.name, maxLines = 1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothState.hasBluetoothConnectPermission) {
                    OutlinedButton(
                        onClick = onRequestBluetoothPermission,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sync Device Names")
                    }
                }
                Button(
                    onClick = onOpenBluetoothSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pair Bluetooth Device")
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("create_playlist_confirm")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistBottomSheet(
    track: AudioTrack,
    playlists: List<PlaylistSummary>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (PlaylistSummary) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${track.title} • ${track.artist}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onCreateNewPlaylist)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Create New Playlist",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = "No custom playlists yet. Create one above to organize your tracks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(playlists, key = { it.id }) { pl ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPlaylist(pl) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${pl.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStreamUrlDialog(
    onDismiss: () -> Unit,
    onAddStream: (url: String, title: String?, artist: String?) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Hi-Res Audio Stream") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Supports HLS (.m3u8), DASH (.mpd), FLAC, AAC, OPUS, and MP3 streams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream URL (https://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Station / Track Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist / Broadcaster (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onAddStream(url.trim(), title.ifBlank { null }, artist.ifBlank { null })
                        onDismiss()
                    }
                },
                enabled = url.startsWith("http://") || url.startsWith("https://")
            ) {
                Text("Play Stream")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsBottomSheet(
    track: AudioTrack,
    onDismiss: () -> Unit,
    onPlay: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtworkImage(
                    artUri = track.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    cornerRadius = 14.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            DetailSpecRow("Audio Format", track.formatLabel)
            DetailSpecRow("Sample Rate", if (track.sampleRateHz > 0) "${track.sampleRateHz} Hz" else "Adaptive")
            DetailSpecRow("Bitrate", if (track.bitrateKbps > 0) "${track.bitrateKbps} kbps" else "Variable / Lossless")
            DetailSpecRow("Fidelity Class", if (track.isLossless) "Hi-Res / Lossless" else "Standard Compressed")
            DetailSpecRow("Genre", track.genre)
            DetailSpecRow("Playback Mode", if (track.isOfflineAvailable) "Local Offline Storage" else "Network Stream")

            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {
                    onPlay()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Track")
            }
        }
    }
}

@Composable
private fun DetailSpecRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<AudioTrack>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelectIndex: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Up Next (${queue.size})", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(queue, key = { index, item -> "${index}_${item.id}" }) { idx, track ->
                    val isCurrent = idx == currentIndex
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectIndex(idx)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            ArtworkImage(
                                artUri = track.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                cornerRadius = 8.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    "${track.artist} • ${track.formatLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (track.isOfflineAvailable) {
                                Icon(
                                    Icons.Rounded.OfflinePin,
                                    contentDescription = "Offline",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
