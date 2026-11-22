package com.devekene.muzik.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.devekene.muzik.R
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.data.model.PlaylistSummary
import com.devekene.muzik.ui.components.AddStreamUrlDialog
import com.devekene.muzik.ui.components.AddToPlaylistBottomSheet
import com.devekene.muzik.ui.components.BluetoothSyncBottomSheet
import com.devekene.muzik.ui.components.CreatePlaylistDialog
import com.devekene.muzik.ui.components.MiniPlayerBar
import com.devekene.muzik.ui.components.QueueBottomSheet
import com.devekene.muzik.ui.components.RightSideMenuDrawer
import com.devekene.muzik.ui.components.TrackDetailsBottomSheet
import com.devekene.muzik.ui.navigation.AboutDeveloperRoute
import com.devekene.muzik.ui.navigation.AiStudioRoute
import com.devekene.muzik.ui.navigation.CollectionDetailRoute
import com.devekene.muzik.ui.navigation.HomeRoute
import com.devekene.muzik.ui.navigation.LibraryRoute
import com.devekene.muzik.ui.navigation.PlaylistsRoute
import com.devekene.muzik.ui.navigation.StudioRoute
import com.devekene.muzik.ui.screens.AboutDeveloperScreen
import com.devekene.muzik.ui.screens.AiStudioScreen
import com.devekene.muzik.ui.screens.CollectionDetailScreen
import com.devekene.muzik.ui.screens.HomeScreen
import com.devekene.muzik.ui.screens.LibraryScreen
import com.devekene.muzik.ui.screens.NowPlayingScreen
import com.devekene.muzik.ui.screens.PlaylistsScreen
import com.devekene.muzik.ui.screens.StudioScreen
import com.devekene.muzik.ui.theme.MuzikTheme
import com.devekene.muzik.ui.viewmodel.MuzikViewModel

private data class TopLevelDestination(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
    val tag: String
)

private val TOP_DESTINATIONS = listOf(
    TopLevelDestination(
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        route = HomeRoute,
        tag = "nav_home"
    ),
    TopLevelDestination(
        labelRes = R.string.nav_library,
        selectedIcon = Icons.Rounded.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
        route = LibraryRoute,
        tag = "nav_library"
    ),
    TopLevelDestination(
        labelRes = R.string.nav_playlists,
        selectedIcon = Icons.AutoMirrored.Rounded.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
        route = PlaylistsRoute,
        tag = "nav_playlists"
    ),
    TopLevelDestination(
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        route = AiStudioRoute,
        tag = "nav_studio"
    )
)

@Composable
fun MuzikApp(viewModel: MuzikViewModel = viewModel()) {
    val context = LocalContext.current
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val losslessTracks by viewModel.losslessTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedLibraryTab by viewModel.selectedLibraryTab.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[audioPermission] == true
        hasAudioPermission = granted
        if (granted) {
            viewModel.scanDeviceLibrary()
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshBluetoothDevices()
    }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            viewModel.importDocumentUris(uris)
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.scanDeviceLibrary()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    var isMenuDrawerOpen by remember { mutableStateOf(false) }
    var isNowPlayingOpen by remember { mutableStateOf(false) }
    var showBluetoothSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackPendingPlaylistAdd by remember { mutableStateOf<AudioTrack?>(null) }
    var trackPendingDetails by remember { mutableStateOf<AudioTrack?>(null) }
    var showAddStreamDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun triggerAudioScanOrPermission() {
        if (hasAudioPermission) {
            viewModel.scanDeviceLibrary()
        } else {
            val perms = buildList {
                add(audioPermission)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            audioPermissionLauncher.launch(perms)
        }
    }

    fun openPlaylistDetail(playlist: PlaylistSummary) {
        navController.navigate(
            CollectionDetailRoute(
                title = playlist.name,
                subtitle = "Playlist",
                filterType = "PLAYLIST",
                playlistId = playlist.id
            )
        )
    }

    fun openSmartCollection(title: String, filterType: String) {
        navController.navigate(
            CollectionDetailRoute(
                title = title,
                subtitle = "Smart Collection",
                filterType = filterType
            )
        )
    }

    MuzikTheme(
        themeMode = preferences.themeMode,
        dynamicColor = preferences.useDynamicColor
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp
            val hasMiniPlayer = playerState.currentTrack != null
            val listBottomPadding = if (hasMiniPlayer) 96.dp else 20.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            TOP_DESTINATIONS.forEach { dest ->
                                val selected = currentDestination?.hasRoute(dest.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.weight(0.2f))
                            TOP_DESTINATIONS.forEach { dest ->
                                val selected = currentDestination?.hasRoute(dest.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 920.dp)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = HomeRoute
                            ) {
                                composable<HomeRoute> {
                                    HomeScreen(
                                        allTracks = allTracks,
                                        recentlyPlayed = recentlyPlayed,
                                        favoriteTracks = favoriteTracks,
                                        losslessTracks = losslessTracks,
                                        playlists = playlists,
                                        genres = genres,
                                        searchQuery = searchQuery,
                                        searchResults = searchResults,
                                        playerState = playerState,
                                        bluetoothState = bluetoothState,
                                        isScanning = isScanning,
                                        hasAudioPermission = hasAudioPermission,
                                        bottomContentPadding = listBottomPadding,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onOpenMenu = { isMenuDrawerOpen = true },
                                        onOpenBluetoothSheet = { showBluetoothSheet = true },
                                        onScanDeviceAudio = { triggerAudioScanOrPermission() },
                                        onImportFiles = { importFilesLauncher.launch(arrayOf("audio/*")) },
                                        onOpenAddStreamDialog = { showAddStreamDialog = true },
                                        onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                        onShuffleAll = { viewModel.playQueue(allTracks, 0, shuffle = true) },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onAddToQueue = viewModel::addToQueue,
                                        onAddToPlaylist = { trackPendingPlaylistAdd = it },
                                        onShowTrackDetails = { trackPendingDetails = it },
                                        onOpenPlaylistsTab = { navigateTopLevel(PlaylistsRoute) },
                                        onOpenSmartCollection = ::openSmartCollection,
                                        onOpenPlaylist = ::openPlaylistDetail,
                                        onOpenGenre = { genre ->
                                            navController.navigate(
                                                CollectionDetailRoute(
                                                    title = genre.name,
                                                    subtitle = "Genre",
                                                    filterType = "GENRE",
                                                    filterKey = genre.name
                                                )
                                            )
                                        },
                                        onOpenArtist = { artist ->
                                            navController.navigate(
                                                CollectionDetailRoute(
                                                    title = artist.name,
                                                    subtitle = "Artist",
                                                    filterType = "ARTIST",
                                                    filterKey = artist.name
                                                )
                                            )
                                        }
                                    )
                                }

                                composable<LibraryRoute> {
                                    LibraryScreen(
                                        selectedTab = selectedLibraryTab,
                                        allTracks = allTracks,
                                        losslessTracks = losslessTracks,
                                        artists = artists,
                                        albums = albums,
                                        genres = genres,
                                        playerState = playerState,
                                        bottomContentPadding = listBottomPadding,
                                        onSelectTab = viewModel::selectLibraryTab,
                                        onScanDeviceAudio = { triggerAudioScanOrPermission() },
                                        onImportFiles = { importFilesLauncher.launch(arrayOf("audio/*")) },
                                        onAddStreamUrl = { showAddStreamDialog = true },
                                        onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                                        onPlayQueue = { tracks, shuffle -> viewModel.playQueue(tracks, 0, shuffle) },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onAddToQueue = viewModel::addToQueue,
                                        onAddToPlaylist = { trackPendingPlaylistAdd = it },
                                        onShowTrackDetails = { trackPendingDetails = it },
                                        onOpenArtist = { group ->
                                            navController.navigate(
                                                CollectionDetailRoute(
                                                    title = group.name,
                                                    subtitle = "Artist",
                                                    filterType = "ARTIST",
                                                    filterKey = group.name
                                                )
                                            )
                                        },
                                        onOpenAlbum = { group ->
                                            navController.navigate(
                                                CollectionDetailRoute(
                                                    title = group.name,
                                                    subtitle = "Album",
                                                    filterType = "ALBUM",
                                                    filterKey = group.name
                                                )
                                            )
                                        },
                                        onOpenGenre = { group ->
                                            navController.navigate(
                                                CollectionDetailRoute(
                                                    title = group.name,
                                                    subtitle = "Genre",
                                                    filterType = "GENRE",
                                                    filterKey = group.name
                                                )
                                            )
                                        }
                                    )
                                }

                                composable<PlaylistsRoute> {
                                    PlaylistsScreen(
                                        playlists = playlists,
                                        favoritesCount = favoriteTracks.size,
                                        losslessCount = losslessTracks.size,
                                        offlineCount = allTracks.count { it.isOfflineAvailable },
                                        recentCount = recentlyPlayed.size,
                                        bottomContentPadding = listBottomPadding,
                                        onCreatePlaylist = { showCreatePlaylistDialog = true },
                                        onDeletePlaylist = viewModel::deletePlaylist,
                                        onOpenPlaylist = ::openPlaylistDetail,
                                        onOpenSmartCollection = ::openSmartCollection
                                    )
                                }

                                composable<AiStudioRoute> {
                                    AiStudioScreen(
                                        bottomContentPadding = listBottomPadding,
                                        onPlayTrackInPlayer = { track ->
                                            viewModel.playTrack(track, listOf(track) + allTracks.filter { it.id != track.id })
                                        },
                                        onStatusMessage = viewModel::showStatusMessage
                                    )
                                }

                                composable<StudioRoute> {
                                    StudioScreen(
                                        eqState = equalizerState,
                                        bluetoothState = bluetoothState,
                                        preferences = preferences,
                                        bottomContentPadding = listBottomPadding,
                                        onToggleEqEnabled = viewModel::setEqEnabled,
                                        onChangeBandLevel = viewModel::setEqBandLevel,
                                        onChangeBassBoost = viewModel::setBassBoost,
                                        onChangeVirtualizer = viewModel::setVirtualizer,
                                        onApplyPreset = viewModel::applyEqPreset,
                                        onSaveCustomPreset = viewModel::saveCustomEqPreset,
                                        onDeleteCustomPreset = viewModel::deleteCustomEqPreset,
                                        onSelectCrossfadeMs = viewModel::setCrossfadeDurationMs,
                                        onToggleGapless = viewModel::setGaplessPlayback,
                                        onOpenBluetoothSheet = { showBluetoothSheet = true },
                                        onToggleBluetoothAutoResume = viewModel::setBluetoothAutoResume,
                                        onToggleBluetoothAutoEq = viewModel::setBluetoothAutoEq,
                                        onSelectThemeMode = viewModel::setThemeMode,
                                        onToggleDynamicColor = viewModel::setUseDynamicColor,
                                        onScanAudio = { triggerAudioScanOrPermission() },
                                        onImportAudio = { importFilesLauncher.launch(arrayOf("audio/*")) },
                                        onOpenAddStream = { showAddStreamDialog = true },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable<AboutDeveloperRoute> {
                                    AboutDeveloperScreen(
                                        bottomContentPadding = listBottomPadding,
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable<CollectionDetailRoute> { entry ->
                                    val route = entry.toRoute<CollectionDetailRoute>()
                                    val playlistTracks by if (route.filterType == "PLAYLIST" && route.playlistId > 0L) {
                                        viewModel.observePlaylistTracks(route.playlistId)
                                            .collectAsStateWithLifecycle(initialValue = emptyList())
                                    } else {
                                        remember { mutableStateOf(emptyList()) }
                                    }

                                    val resolvedTracks = when (route.filterType) {
                                        "PLAYLIST" -> playlistTracks
                                        "ARTIST" -> allTracks.filter { it.artist == route.filterKey }
                                        "ALBUM" -> allTracks.filter { it.album == route.filterKey }
                                        "GENRE" -> allTracks.filter { it.genre == route.filterKey }
                                        "FAVORITES" -> favoriteTracks
                                        "LOSSLESS" -> losslessTracks
                                        "OFFLINE" -> allTracks.filter { it.isOfflineAvailable }
                                        "RECENT" -> recentlyPlayed
                                        else -> allTracks
                                    }

                                    CollectionDetailScreen(
                                        title = route.title,
                                        subtitle = route.subtitle,
                                        tracks = resolvedTracks,
                                        playerState = playerState,
                                        isUserPlaylist = route.filterType == "PLAYLIST",
                                        bottomContentPadding = listBottomPadding,
                                        onBack = { navController.popBackStack() },
                                        onPlayTrack = { track -> viewModel.playTrack(track, resolvedTracks) },
                                        onPlayAll = { viewModel.playQueue(resolvedTracks, 0, shuffle = false) },
                                        onShuffleAll = { viewModel.playQueue(resolvedTracks, 0, shuffle = true) },
                                        onToggleFavorite = viewModel::toggleFavorite,
                                        onAddToQueue = viewModel::addToQueue,
                                        onAddToPlaylist = { trackPendingPlaylistAdd = it },
                                        onShowDetails = { trackPendingDetails = it },
                                        onRemoveFromPlaylist = if (route.filterType == "PLAYLIST") {
                                            { track -> viewModel.removeTrackFromPlaylist(route.playlistId, track.id) }
                                        } else null
                                    )
                                }
                            }

                            // Floating Mini Player anchored above Bottom Navigation / bottom edge
                            if (hasMiniPlayer) {
                                MiniPlayerBar(
                                    playerState = playerState,
                                    onExpand = { isNowPlayingOpen = true },
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onNext = viewModel::skipNext,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Full-Screen Immersive Now Playing Overlay
            AnimatedVisibility(
                visible = isNowPlayingOpen && playerState.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    playerState = playerState,
                    bluetoothState = bluetoothState,
                    onCollapse = { isNowPlayingOpen = false },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo,
                    onSkipNext = viewModel::skipNext,
                    onSkipPrevious = viewModel::skipPrevious,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onOpenBluetoothSheet = { showBluetoothSheet = true },
                    onOpenEqualizer = {
                        isNowPlayingOpen = false
                        navController.navigate(StudioRoute) { launchSingleTop = true }
                    },
                    onOpenQueue = { showQueueSheet = true },
                    onShowDetails = { trackPendingDetails = it }
                )
            }

            // Right-side Sliding Hamburger Menu Drawer
            RightSideMenuDrawer(
                visible = isMenuDrawerOpen,
                themeMode = preferences.themeMode,
                bluetoothDeviceName = bluetoothState.activeDevice?.name,
                onDismiss = { isMenuDrawerOpen = false },
                onToggleTheme = viewModel::cycleLightDarkTheme,
                onSelectThemeMode = viewModel::setThemeMode,
                onOpenSettings = { navController.navigate(StudioRoute) { launchSingleTop = true } },
                onOpenBluetoothSync = { showBluetoothSheet = true },
                onScanDeviceAudio = { triggerAudioScanOrPermission() },
                onImportAudioFiles = { importFilesLauncher.launch(arrayOf("audio/*")) },
                onAddStreamUrl = { showAddStreamDialog = true },
                onOpenAboutDeveloper = {
                    navController.navigate(AboutDeveloperRoute) { launchSingleTop = true }
                }
            )
        }

        // Modals & Bottom Sheets
        if (showBluetoothSheet) {
            BluetoothSyncBottomSheet(
                bluetoothState = bluetoothState,
                eqPresets = equalizerState.presets,
                onDismiss = { showBluetoothSheet = false },
                onRequestBluetoothPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                },
                onOpenBluetoothSettings = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                },
                onToggleAutoResume = viewModel::setBluetoothAutoResume,
                onToggleAutoEq = viewModel::setBluetoothAutoEq,
                onRefresh = viewModel::refreshBluetoothDevices
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name, desc ->
                    viewModel.createPlaylist(name, desc, trackPendingPlaylistAdd)
                    trackPendingPlaylistAdd = null
                }
            )
        }

        trackPendingPlaylistAdd?.let { track ->
            if (!showCreatePlaylistDialog) {
                AddToPlaylistBottomSheet(
                    track = track,
                    playlists = playlists,
                    onDismiss = { trackPendingPlaylistAdd = null },
                    onSelectPlaylist = { pl ->
                        viewModel.addTrackToPlaylist(pl.id, pl.name, track)
                        trackPendingPlaylistAdd = null
                    },
                    onCreateNewPlaylist = {
                        showCreatePlaylistDialog = true
                    }
                )
            }
        }

        if (showAddStreamDialog) {
            AddStreamUrlDialog(
                onDismiss = { showAddStreamDialog = false },
                onAddStream = viewModel::addStreamUrl
            )
        }

        trackPendingDetails?.let { track ->
            TrackDetailsBottomSheet(
                track = track,
                onDismiss = { trackPendingDetails = null },
                onPlay = { viewModel.playTrack(track, allTracks) }
            )
        }

        if (showQueueSheet) {
            QueueBottomSheet(
                queue = playerState.queue,
                currentIndex = playerState.currentIndex,
                onDismiss = { showQueueSheet = false },
                onSelectIndex = viewModel::playQueueIndex
            )
        }
    }
}
