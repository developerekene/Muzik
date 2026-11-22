package com.devekene.muzik.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.devekene.muzik.data.local.UserAudioPreferences
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.BluetoothSyncState
import com.devekene.muzik.data.model.CategoryGroup
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.data.model.EqualizerState
import com.devekene.muzik.data.model.LibraryTab
import com.devekene.muzik.data.model.PlayerState
import com.devekene.muzik.data.model.PlaylistSummary
import com.devekene.muzik.data.model.RepeatModeState
import com.devekene.muzik.data.model.ThemeMode
import com.devekene.muzik.playback.AudioEngineLocator
import com.devekene.muzik.playback.MuzikPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResultsUi(
    val tracks: List<AudioTrack> = emptyList(),
    val artists: List<CategoryGroup> = emptyList(),
    val genres: List<CategoryGroup> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList()
)

class MuzikViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = AudioEngineLocator.getRepository(appContext)
    private val equalizerManager = AudioEngineLocator.getEqualizerManager()
    private val bluetoothManager = AudioEngineLocator.getBluetoothAudioManager(appContext)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var eqHydrated = false

    val preferences: StateFlow<UserAudioPreferences> = repository.userPreferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserAudioPreferences())

    val allTracks: StateFlow<List<AudioTrack>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<AudioTrack>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<AudioTrack>> = repository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val losslessTracks: StateFlow<List<AudioTrack>> = repository.losslessTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equalizerState: StateFlow<EqualizerState> = equalizerManager.state
    val bluetoothState: StateFlow<BluetoothSyncState> = bluetoothManager.state

    val artists: StateFlow<List<CategoryGroup>> = allTracks.map { tracks ->
        tracks.groupBy { it.artist }
            .entries
            .sortedBy { it.key.lowercase() }
            .map { (artist, list) ->
                val albumCount = list.map { it.album }.distinct().size
                CategoryGroup(
                    name = artist,
                    subtitle = "$albumCount ${if (albumCount == 1) "album" else "albums"} • ${list.size} ${if (list.size == 1) "track" else "tracks"}",
                    trackCount = list.size,
                    coverArtUri = list.firstNotNullOfOrNull { it.albumArtUri },
                    hasLossless = list.any { it.isLossless }
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<CategoryGroup>> = allTracks.map { tracks ->
        tracks.groupBy { it.album }
            .entries
            .sortedBy { it.key.lowercase() }
            .map { (album, list) ->
                val primaryArtist = list.firstOrNull()?.artist ?: "Various Artists"
                CategoryGroup(
                    name = album,
                    subtitle = "$primaryArtist • ${list.size} ${if (list.size == 1) "track" else "tracks"}",
                    trackCount = list.size,
                    coverArtUri = list.firstNotNullOfOrNull { it.albumArtUri },
                    hasLossless = list.any { it.isLossless }
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genres: StateFlow<List<CategoryGroup>> = allTracks.map { tracks ->
        tracks.groupBy { it.genre }
            .entries
            .sortedBy { it.key.lowercase() }
            .map { (genre, list) ->
                CategoryGroup(
                    name = genre,
                    subtitle = "${list.size} ${if (list.size == 1) "track" else "tracks"}",
                    trackCount = list.size,
                    coverArtUri = list.firstNotNullOfOrNull { it.albumArtUri },
                    hasLossless = list.any { it.isLossless }
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLibraryTab = MutableStateFlow(LibraryTab.TRACKS)
    val selectedLibraryTab: StateFlow<LibraryTab> = _selectedLibraryTab.asStateFlow()

    val searchResults: StateFlow<SearchResultsUi> = combine(
        _searchQuery,
        allTracks,
        artists,
        genres,
        playlists
    ) { query, trackList, artistList, genreList, plList ->
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            SearchResultsUi()
        } else {
            SearchResultsUi(
                tracks = trackList.filter {
                    it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.genre.lowercase().contains(q) ||
                        it.formatLabel.lowercase().contains(q)
                },
                artists = artistList.filter { it.name.lowercase().contains(q) },
                genres = genreList.filter { it.name.lowercase().contains(q) },
                playlists = plList.filter { it.name.lowercase().contains(q) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultsUi())

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        connectMediaController()
        observeSettingsAndPresets()
        observeBluetoothSyncEvents()
    }

    private fun observeSettingsAndPresets() {
        viewModelScope.launch {
            repository.eqPresets.collect { presets ->
                equalizerManager.setPresets(presets)
            }
        }
        viewModelScope.launch {
            repository.userPreferences.collect { prefs ->
                AudioEngineLocator.crossfadeDurationMs = prefs.crossfadeDurationMs
                bluetoothManager.updateSyncPreferences(
                    autoResumeOnConnect = prefs.bluetoothAutoResume,
                    autoEqOnBluetooth = prefs.bluetoothAutoEq,
                    bluetoothEqPresetName = prefs.bluetoothEqPresetName
                )
                _playerState.update {
                    it.copy(
                        crossfadeDurationMs = prefs.crossfadeDurationMs,
                        gaplessPlayback = prefs.gaplessPlayback
                    )
                }
                if (!eqHydrated) {
                    eqHydrated = true
                    equalizerManager.hydrateFromPreferences(
                        enabled = prefs.eqEnabled,
                        presetName = prefs.activeEqPresetName,
                        bandLevelsMb = prefs.customBandLevelsMb,
                        bassBoostStrength = prefs.bassBoostStrength,
                        virtualizerStrength = prefs.virtualizerStrength
                    )
                }
            }
        }
    }

    private fun observeBluetoothSyncEvents() {
        viewModelScope.launch {
            bluetoothManager.bluetoothConnectedEvents.collect { device ->
                val prefs = preferences.value
                if (prefs.bluetoothAutoEq) {
                    val targetPreset = equalizerState.value.presets
                        .firstOrNull { it.name == prefs.bluetoothEqPresetName }
                    if (targetPreset != null) {
                        applyEqPreset(targetPreset)
                    }
                }
                if (prefs.bluetoothAutoResume) {
                    val controller = mediaController
                    if (controller != null && controller.mediaItemCount > 0 && !controller.isPlaying) {
                        controller.play()
                    }
                }
                _statusBanner.value = "Bluetooth synced with ${device.name}"
            }
        }
    }

    private fun connectMediaController() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MuzikPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    attachPlayerListener(controller)
                    syncPlayerStateFromController(controller)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncPlayerStateFromController(controller)
                if (isPlaying) {
                    startPositionTicker()
                    val currentTrack = _playerState.value.currentTrack
                    if (currentTrack != null) {
                        viewModelScope.launch {
                            repository.recordTrackPlayed(currentTrack.id)
                        }
                    }
                } else {
                    positionTickerJob?.cancel()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncPlayerStateFromController(controller)
                val trackId = mediaItem?.mediaId?.toLongOrNull()
                if (trackId != null) {
                    viewModelScope.launch {
                        repository.recordTrackPlayed(trackId)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncPlayerStateFromController(controller)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                syncPlayerStateFromController(controller)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                syncPlayerStateFromController(controller)
            }
        })
    }

    private fun syncPlayerStateFromController(controller: MediaController) {
        val mediaId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        val currentQueue = _playerState.value.queue
        val matchedTrack = currentQueue.firstOrNull { it.id == mediaId }
            ?: allTracks.value.firstOrNull { it.id == mediaId }
            ?: _playerState.value.currentTrack

        val repeatModeState = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatModeState.ALL
            Player.REPEAT_MODE_ONE -> RepeatModeState.ONE
            else -> RepeatModeState.OFF
        }

        val controllerDuration = controller.duration.takeIf { it > 0L }
            ?: matchedTrack?.durationMs
            ?: 0L

        _playerState.update { state ->
            state.copy(
                currentTrack = matchedTrack,
                currentIndex = controller.currentMediaItemIndex,
                isPlaying = controller.isPlaying,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controllerDuration,
                shuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = repeatModeState,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING
            )
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    val dur = controller.duration.takeIf { it > 0L }
                        ?: _playerState.value.currentTrack?.durationMs
                        ?: 0L
                    _playerState.update {
                        it.copy(
                            positionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = dur,
                            isPlaying = controller.isPlaying
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectLibraryTab(tab: LibraryTab) {
        _selectedLibraryTab.value = tab
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showStatusMessage(message: String) {
        _statusBanner.value = message
    }

    fun scanDeviceLibrary() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            val count = runCatching { repository.scanDeviceLibrary() }.getOrDefault(0)
            _isScanning.value = false
            _statusBanner.value = if (count > 0) {
                "Synced $count local audio tracks"
            } else {
                "No local audio files found in MediaStore. Import files or add a Hi-Res stream."
            }
        }
    }

    fun importDocumentUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isScanning.value = true
            val imported = repository.importDocumentUris(uris)
            _isScanning.value = false
            if (imported.isNotEmpty()) {
                _statusBanner.value = "Imported ${imported.size} offline audio ${if (imported.size == 1) "file" else "files"}"
                if (_playerState.value.currentTrack == null) {
                    playQueue(imported, 0)
                }
            }
        }
    }

    fun addStreamUrl(url: String, title: String?, artist: String?) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val track = repository.addStreamUrl(url, title, artist)
            _statusBanner.value = "Added ${track.formatLabel} stream: ${track.title}"
            playTrack(track, listOf(track) + allTracks.value.filter { it.id != track.id })
        }
    }

    fun playTrack(track: AudioTrack, queueContext: List<AudioTrack> = allTracks.value) {
        val queue = if (queueContext.any { it.id == track.id }) queueContext else listOf(track) + queueContext
        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playQueue(queue, startIndex)
    }

    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val controller = mediaController ?: return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)

        val mediaItems = tracks.map { track ->
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setGenre(track.genre)
                .apply {
                    track.albumArtUri?.let { setArtworkUri(Uri.parse(it)) }
                }
                .build()
            MediaItem.Builder()
                .setMediaId(track.id.toString())
                .setUri(track.uriString)
                .setMimeType(track.mimeType)
                .setMediaMetadata(metadata)
                .build()
        }

        _playerState.update {
            it.copy(
                queue = tracks,
                currentTrack = tracks[safeIndex],
                currentIndex = safeIndex
            )
        }

        controller.shuffleModeEnabled = shuffle
        controller.setMediaItems(mediaItems, safeIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun addToQueue(track: AudioTrack) {
        val controller = mediaController ?: return
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setGenre(track.genre)
            .apply {
                track.albumArtUri?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uriString)
            .setMimeType(track.mimeType)
            .setMediaMetadata(metadata)
            .build()

        if (controller.mediaItemCount == 0) {
            playQueue(listOf(track), 0)
        } else {
            controller.addMediaItem(mediaItem)
            _playerState.update { it.copy(queue = it.queue + track) }
            _statusBanner.value = "Added \"${track.title}\" to Up Next"
        }
    }

    fun playQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
            controller.play()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            val fallbackQueue = recentlyPlayed.value.ifEmpty { allTracks.value }
            if (fallbackQueue.isNotEmpty()) {
                playQueue(fallbackQueue, 0)
            }
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _playerState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun skipNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleFavorite(track: AudioTrack) {
        val newValue = !track.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(track.id, newValue)
            _playerState.update { state ->
                val updatedCurrent = if (state.currentTrack?.id == track.id) {
                    state.currentTrack.copy(isFavorite = newValue)
                } else state.currentTrack
                val updatedQueue = state.queue.map {
                    if (it.id == track.id) it.copy(isFavorite = newValue) else it
                }
                state.copy(currentTrack = updatedCurrent, queue = updatedQueue)
            }
        }
    }

    fun deleteTrackFromLibrary(track: AudioTrack) {
        viewModelScope.launch {
            repository.deleteTrack(track.id)
            _statusBanner.value = "Removed \"${track.title}\" from Muzik library"
        }
    }

    fun createPlaylist(name: String, description: String = "", initialTrack: AudioTrack? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(name, description)
            if (initialTrack != null) {
                repository.addTrackToPlaylist(id, initialTrack.id)
            }
            _statusBanner.value = "Created playlist \"$name\""
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, playlistName: String, track: AudioTrack) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track.id)
            _statusBanner.value = "Added to \"$playlistName\""
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun observePlaylistTracks(playlistId: Long): Flow<List<AudioTrack>> =
        repository.observePlaylistTracks(playlistId)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun cycleLightDarkTheme() {
        val next = if (preferences.value.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(next)
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setUseDynamicColor(enabled) }
    }

    fun setEqEnabled(enabled: Boolean) {
        equalizerManager.setEnabled(enabled)
        persistCurrentEqState()
    }

    fun setEqBandLevel(bandIndex: Int, levelMb: Int) {
        equalizerManager.setBandLevel(bandIndex, levelMb)
        persistCurrentEqState()
    }

    fun setBassBoost(strength: Int) {
        equalizerManager.setBassBoost(strength)
        persistCurrentEqState()
    }

    fun setVirtualizer(strength: Int) {
        equalizerManager.setVirtualizer(strength)
        persistCurrentEqState()
    }

    fun applyEqPreset(preset: EqPreset) {
        equalizerManager.applyPreset(preset)
        persistCurrentEqState()
    }

    fun saveCustomEqPreset(name: String) {
        if (name.isBlank()) return
        val eq = equalizerManager.state.value
        viewModelScope.launch {
            repository.saveCustomEqPreset(
                name = name,
                bandLevelsMb = eq.bands.map { it.levelMb },
                bassBoostStrength = eq.bassBoostStrength,
                virtualizerStrength = eq.virtualizerStrength
            )
            val savedPreset = repository.eqPresets.first().firstOrNull { it.name == name.trim() }
            if (savedPreset != null) {
                equalizerManager.applyPreset(savedPreset)
                persistCurrentEqState()
            }
            _statusBanner.value = "Saved EQ preset \"$name\""
        }
    }

    fun deleteCustomEqPreset(preset: EqPreset) {
        if (preset.isBuiltIn) return
        viewModelScope.launch {
            repository.deleteCustomEqPreset(preset.id)
        }
    }

    private fun persistCurrentEqState() {
        val eq = equalizerManager.state.value
        viewModelScope.launch {
            repository.saveEqSettings(
                enabled = eq.isEnabled,
                presetName = eq.activePresetName,
                bandLevelsMb = eq.bands.map { it.levelMb },
                bassBoost = eq.bassBoostStrength,
                virtualizer = eq.virtualizerStrength
            )
        }
    }

    fun setCrossfadeDurationMs(durationMs: Int) {
        viewModelScope.launch { repository.setCrossfadeDurationMs(durationMs) }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { repository.setGaplessPlayback(enabled) }
    }

    fun setBluetoothAutoResume(enabled: Boolean) {
        viewModelScope.launch { repository.setBluetoothAutoResume(enabled) }
    }

    fun setBluetoothAutoEq(enabled: Boolean, presetName: String) {
        viewModelScope.launch { repository.setBluetoothAutoEq(enabled, presetName) }
    }

    fun refreshBluetoothDevices() {
        bluetoothManager.refreshDevices(emitConnectEvent = false)
    }

    override fun onCleared() {
        positionTickerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
