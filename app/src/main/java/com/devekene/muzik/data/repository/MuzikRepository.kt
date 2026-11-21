package com.devekene.muzik.data.repository

import android.net.Uri
import com.devekene.muzik.data.local.MuzikDao
import com.devekene.muzik.data.local.PlaylistEntity
import com.devekene.muzik.data.local.EqPresetEntity
import com.devekene.muzik.data.local.SettingsDataStore
import com.devekene.muzik.data.local.UserAudioPreferences
import com.devekene.muzik.data.local.toDomain
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.data.model.PlaylistSummary
import com.devekene.muzik.data.model.ThemeMode
import com.devekene.muzik.data.scanner.LocalAudioScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class MuzikRepository(
    private val dao: MuzikDao,
    private val scanner: LocalAudioScanner,
    private val settingsStore: SettingsDataStore
) {
    val allTracks: Flow<List<AudioTrack>> = dao.observeAllTracks()
        .map { list -> list.map { it.toDomain() } }

    val recentlyPlayed: Flow<List<AudioTrack>> = dao.observeRecentlyPlayed()
        .map { list -> list.map { it.toDomain() } }

    val favoriteTracks: Flow<List<AudioTrack>> = dao.observeFavoriteTracks()
        .map { list -> list.map { it.toDomain() } }

    val losslessTracks: Flow<List<AudioTrack>> = dao.observeLosslessTracks()
        .map { list -> list.map { it.toDomain() } }

    val eqPresets: Flow<List<EqPreset>> = dao.observeEqPresets()
        .map { list -> list.map { it.toDomain() } }

    val userPreferences: Flow<UserAudioPreferences> = settingsStore.preferencesFlow

    val playlists: Flow<List<PlaylistSummary>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistCrossRefs(),
        dao.observeAllTracks()
    ) { playlistEntities, crossRefs, tracks ->
        val trackArtById = tracks.associate { it.id to it.albumArtUri }
        playlistEntities.map { pl ->
            val refsForPlaylist = crossRefs.filter { it.playlistId == pl.id }.sortedBy { it.position }
            val coverArt = refsForPlaylist.firstNotNullOfOrNull { trackArtById[it.trackId] }
            PlaylistSummary(
                id = pl.id,
                name = pl.name,
                description = pl.description,
                trackCount = refsForPlaylist.size,
                coverArtUri = coverArt,
                createdAt = pl.createdAt
            )
        }
    }

    fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>> =
        dao.observeTracksForPlaylist(playlistId).map { list -> list.map { it.toDomain() } }

    suspend fun scanDeviceLibrary(): Int {
        val existingById = dao.getAllTracksSnapshot().associateBy { it.id }
        val scanned = scanner.scanDeviceMediaStore()
        val merged = scanned.map { fresh ->
            val prev = existingById[fresh.id]
            if (prev != null) {
                fresh.copy(
                    isFavorite = prev.isFavorite,
                    lastPlayedAt = prev.lastPlayedAt,
                    playCount = prev.playCount
                )
            } else {
                fresh
            }
        }
        if (merged.isNotEmpty()) {
            dao.upsertTracks(merged)
        }
        return merged.size
    }

    suspend fun importDocumentUris(uris: List<Uri>): List<AudioTrack> {
        val existingById = dao.getAllTracksSnapshot().associateBy { it.id }
        val imported = uris.mapNotNull { uri ->
            val entity = scanner.extractFromDocumentUri(uri) ?: return@mapNotNull null
            val prev = existingById[entity.id]
            if (prev != null) {
                entity.copy(
                    isFavorite = prev.isFavorite,
                    lastPlayedAt = prev.lastPlayedAt,
                    playCount = prev.playCount
                )
            } else entity
        }
        if (imported.isNotEmpty()) {
            dao.upsertTracks(imported)
        }
        return imported.map { it.toDomain() }
    }

    suspend fun addStreamUrl(url: String, title: String?, artist: String?): AudioTrack {
        val entity = scanner.buildStreamTrack(url, title, artist)
        dao.upsertTrack(entity)
        return entity.toDomain()
    }

    suspend fun toggleFavorite(trackId: Long, newFavorite: Boolean) {
        dao.setFavorite(trackId, newFavorite)
    }

    suspend fun recordTrackPlayed(trackId: Long) {
        dao.markPlayed(trackId, System.currentTimeMillis())
        settingsStore.setLastPlayedTrackId(trackId)
    }

    suspend fun deleteTrack(trackId: Long) {
        dao.deleteTrack(trackId)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return dao.insertPlaylist(
            PlaylistEntity(name = name.trim(), description = description.trim())
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        dao.addTrackToPlaylistAtEnd(playlistId, trackId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun saveCustomEqPreset(
        name: String,
        bandLevelsMb: List<Int>,
        bassBoostStrength: Int,
        virtualizerStrength: Int
    ) {
        dao.insertEqPreset(
            EqPresetEntity(
                name = name.trim(),
                bandLevelsCsv = bandLevelsMb.joinToString(","),
                bassBoostStrength = bassBoostStrength,
                virtualizerStrength = virtualizerStrength,
                isBuiltIn = false
            )
        )
    }

    suspend fun deleteCustomEqPreset(presetId: Long) {
        dao.deleteCustomEqPreset(presetId)
    }

    suspend fun setThemeMode(mode: ThemeMode) = settingsStore.setThemeMode(mode)
    suspend fun setUseDynamicColor(enabled: Boolean) = settingsStore.setUseDynamicColor(enabled)

    suspend fun saveEqSettings(
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoost: Int,
        virtualizer: Int
    ) = settingsStore.setEqSettings(enabled, presetName, bandLevelsMb, bassBoost, virtualizer)

    suspend fun setCrossfadeDurationMs(durationMs: Int) = settingsStore.setCrossfadeDurationMs(durationMs)
    suspend fun setGaplessPlayback(enabled: Boolean) = settingsStore.setGaplessPlayback(enabled)
    suspend fun setBluetoothAutoResume(enabled: Boolean) = settingsStore.setBluetoothAutoResume(enabled)
    suspend fun setBluetoothAutoEq(enabled: Boolean, presetName: String) =
        settingsStore.setBluetoothAutoEq(enabled, presetName)
}
