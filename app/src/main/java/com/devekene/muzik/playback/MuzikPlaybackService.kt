package com.devekene.muzik.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.devekene.muzik.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MuzikPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val eqManager = AudioEngineLocator.getEqualizerManager()

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                eqManager.attachToAudioSession(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val crossfadeMs = AudioEngineLocator.crossfadeDurationMs
                if (crossfadeMs > 0 && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    applySmoothFadeIn(player, crossfadeMs.coerceAtMost(2500))
                } else {
                    player.volume = 1f
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        eqManager.attachToAudioSession(player.audioSessionId)
                    }
                    val crossfadeMs = AudioEngineLocator.crossfadeDurationMs
                    if (crossfadeMs > 0 && player.volume < 0.99f) {
                        applySmoothFadeIn(player, (crossfadeMs / 2).coerceIn(250, 1200))
                    }
                }
            }
        })

        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            eqManager.attachToAudioSession(player.audioSessionId)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build())
    }

    private fun applySmoothFadeIn(player: ExoPlayer, durationMs: Int) {
        fadeJob?.cancel()
        fadeJob = serviceScope.launch {
            val steps = 16
            val stepDelay = (durationMs / steps).toLong().coerceAtLeast(15L)
            for (step in 1..steps) {
                player.volume = (step.toFloat() / steps.toFloat()).coerceIn(0.05f, 1f)
                delay(stepDelay)
            }
            player.volume = 1f
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        fadeJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
