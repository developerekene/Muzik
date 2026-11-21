package com.devekene.muzik

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.data.scanner.LocalAudioScanner
import com.devekene.muzik.playback.EqualizerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Muzik`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Muzik", appName)
    }

    @Test
    fun `stream track builder identifies high-res and streaming formats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scanner = LocalAudioScanner(context)

        val flacTrack = scanner.buildStreamTrack("https://cdn.example.com/studio/master.flac", "Master", "Artist")
        assertEquals("FLAC", flacTrack.formatLabel)
        assertTrue(flacTrack.isLossless)
        assertFalse(flacTrack.isOfflineAvailable)

        val hlsTrack = scanner.buildStreamTrack("https://cdn.example.com/live/stream.m3u8", null, null)
        assertEquals("HLS", hlsTrack.formatLabel)
        assertEquals("application/x-mpegURL", hlsTrack.mimeType)
    }

    @Test
    fun `equalizer manager applies preset and updates band levels`() {
        val manager = EqualizerManager()
        val preset = EqPreset(
            id = 1L,
            name = "Warm Bass",
            bandLevelsMb = listOf(550, 350, 0, -100, 150),
            bassBoostStrength = 480,
            virtualizerStrength = 180,
            isBuiltIn = true
        )
        manager.applyPreset(preset)
        val state = manager.state.value
        assertEquals("Warm Bass", state.activePresetName)
        assertEquals(550, state.bands[0].levelMb)
        assertEquals(480, state.bassBoostStrength)
        assertEquals(180, state.virtualizerStrength)

        manager.setBandLevel(2, 400)
        assertEquals("Custom", manager.state.value.activePresetName)
        assertEquals(400, manager.state.value.bands[2].levelMb)
    }
}
