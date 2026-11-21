package com.devekene.muzik.data.model

import androidx.compose.runtime.Immutable

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

enum class RepeatModeState {
    OFF,
    ALL,
    ONE
}

enum class LibraryTab(val label: String) {
    TRACKS("Tracks"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    GENRES("Genres"),
    HIFI("Hi-Fi")
}

enum class AudioOutputType(val label: String) {
    SPEAKER("Device Speaker"),
    WIRED("Wired Audio"),
    USB_DAC("USB Hi-Fi DAC"),
    BLUETOOTH_A2DP("Bluetooth Audio"),
    BLUETOOTH_LE("Bluetooth LE Audio")
}

@Immutable
data class AudioTrack(
    val id: Long,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val albumArtUri: String?,
    val mimeType: String,
    val formatLabel: String,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val isLossless: Boolean,
    val isOfflineAvailable: Boolean,
    val isFavorite: Boolean,
    val lastPlayedAt: Long?,
    val playCount: Int,
    val dateAdded: Long
) {
    val qualitySummary: String
        get() = buildString {
            append(formatLabel)
            if (sampleRateHz > 0) {
                val khz = sampleRateHz / 1000.0
                val formattedKhz = if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format("%.1f kHz", khz)
                append(" • $formattedKhz")
            }
            if (bitrateKbps > 0 && !isLossless) {
                append(" • $bitrateKbps kbps")
            } else if (isLossless) {
                append(" • Lossless")
            }
        }
}

@Immutable
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val description: String,
    val trackCount: Int,
    val coverArtUri: String?,
    val createdAt: Long
)

@Immutable
data class CategoryGroup(
    val name: String,
    val subtitle: String,
    val trackCount: Int,
    val coverArtUri: String?,
    val hasLossless: Boolean
)

@Immutable
data class EqBandUi(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val levelMb: Int
)

@Immutable
data class EqPreset(
    val id: Long,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val isBuiltIn: Boolean
)

@Immutable
data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val type: AudioOutputType,
    val isBluetooth: Boolean,
    val supportedSampleRates: List<Int>,
    val isActive: Boolean
)

@Immutable
data class BluetoothSyncState(
    val activeDevice: AudioOutputDevice? = null,
    val connectedDevices: List<AudioOutputDevice> = emptyList(),
    val isBluetoothConnected: Boolean = false,
    val hasBluetoothConnectPermission: Boolean = false,
    val autoResumeOnConnect: Boolean = true,
    val autoEqOnBluetooth: Boolean = true,
    val bluetoothEqPresetName: String = "Acoustic Clarity",
    val lastSyncEventMessage: String? = null
)

@Immutable
data class EqualizerState(
    val isEnabled: Boolean = true,
    val isHardwareAttached: Boolean = false,
    val activePresetName: String = "Studio Flat",
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bands: List<EqBandUi> = defaultFiveBands(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val presets: List<EqPreset> = emptyList()
) {
    companion object {
        fun defaultFiveBands(levels: List<Int> = listOf(0, 0, 0, 0, 0)): List<EqBandUi> {
            val freqs = listOf(60, 230, 910, 3600, 14000)
            val labels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
            return freqs.indices.map { i ->
                EqBandUi(
                    index = i,
                    centerFreqHz = freqs[i],
                    label = labels[i],
                    levelMb = levels.getOrElse(i) { 0 }
                )
            }
        }
    }
}

@Immutable
data class PlayerState(
    val currentTrack: AudioTrack? = null,
    val queue: List<AudioTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val crossfadeDurationMs: Int = 2000,
    val gaplessPlayback: Boolean = true,
    val isBuffering: Boolean = false
)
