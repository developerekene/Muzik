package com.devekene.muzik.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MuzikDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSnapshot(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 25")
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLossless = 1 ORDER BY sampleRateHz DESC, title COLLATE NOCASE ASC")
    fun observeLosslessTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Long, isFavorite: Boolean)

    @Query("UPDATE tracks SET lastPlayedAt = :timestamp, playCount = playCount + 1 WHERE id = :trackId")
    suspend fun markPlayed(trackId: Long, timestamp: Long)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, description = :description WHERE id = :playlistId")
    suspend fun updatePlaylist(playlistId: Long, name: String, description: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks")
    fun observeAllPlaylistCrossRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC, pt.addedAt ASC
        """
    )
    fun observeTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getNextPlaylistPosition(playlistId: Long): Int

    @Transaction
    suspend fun addTrackToPlaylistAtEnd(playlistId: Long, trackId: Long) {
        val nextPos = getNextPlaylistPosition(playlistId)
        addTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                position = nextPos
            )
        )
    }

    @Query("SELECT * FROM eq_presets ORDER BY isBuiltIn DESC, id ASC")
    fun observeEqPresets(): Flow<List<EqPresetEntity>>

    @Query("SELECT COUNT(*) FROM eq_presets")
    suspend fun getEqPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqPresets(presets: List<EqPresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqPreset(preset: EqPresetEntity): Long

    @Query("DELETE FROM eq_presets WHERE id = :presetId AND isBuiltIn = 0")
    suspend fun deleteCustomEqPreset(presetId: Long)
}
