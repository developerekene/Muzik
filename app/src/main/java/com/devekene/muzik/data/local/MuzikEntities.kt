package com.devekene.muzik.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.EqPreset

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
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
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bandLevelsCsv: String,
    val bassBoostStrength: Int,
    val virtualizerStrength: Int,
    val isBuiltIn: Boolean
)

fun TrackEntity.toDomain(): AudioTrack = AudioTrack(
    id = id,
    uriString = uriString,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    durationMs = durationMs,
    albumArtUri = albumArtUri,
    mimeType = mimeType,
    formatLabel = formatLabel,
    sampleRateHz = sampleRateHz,
    bitrateKbps = bitrateKbps,
    isLossless = isLossless,
    isOfflineAvailable = isOfflineAvailable,
    isFavorite = isFavorite,
    lastPlayedAt = lastPlayedAt,
    playCount = playCount,
    dateAdded = dateAdded
)

fun AudioTrack.toEntity(): TrackEntity = TrackEntity(
    id = id,
    uriString = uriString,
    title = title,
    artist = artist,
    album = album,
    genre = genre,
    durationMs = durationMs,
    albumArtUri = albumArtUri,
    mimeType = mimeType,
    formatLabel = formatLabel,
    sampleRateHz = sampleRateHz,
    bitrateKbps = bitrateKbps,
    isLossless = isLossless,
    isOfflineAvailable = isOfflineAvailable,
    isFavorite = isFavorite,
    lastPlayedAt = lastPlayedAt,
    playCount = playCount,
    dateAdded = dateAdded
)

fun EqPresetEntity.toDomain(): EqPreset = EqPreset(
    id = id,
    name = name,
    bandLevelsMb = bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() },
    bassBoostStrength = bassBoostStrength,
    virtualizerStrength = virtualizerStrength,
    isBuiltIn = isBuiltIn
)
