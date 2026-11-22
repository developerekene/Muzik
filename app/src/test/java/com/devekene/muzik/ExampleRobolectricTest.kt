package com.devekene.muzik

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.devekene.muzik.ai.AiSongBlueprint
import com.devekene.muzik.ai.AudioSynthEngine
import com.devekene.muzik.ai.StudioSoundPad
import com.devekene.muzik.ai.buildWavHeader
import com.devekene.muzik.ai.midiToFrequencyHz
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

    @Test
    fun `strings include AI Studio tab and About Developer label`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("AI Studio", context.getString(R.string.nav_studio))
        assertEquals("About Developer", context.getString(R.string.about_developer))
    }

    @Test
    fun `wav header and synth engine render valid multi-track audio`() {
        assertEquals(440.0, midiToFrequencyHz(69), 0.01)
        val header = buildWavHeader(pcmDataSizeBytes = 1000, sampleRate = 44100)
        assertEquals(44, header.size)
        assertEquals('R'.code.toByte(), header[0])
        assertEquals('W'.code.toByte(), header[8])

        val engine = AudioSynthEngine()
        val blueprint = AiSongBlueprint(
            title = "Test Groove",
            style = "Afrobeats",
            bpm = 110,
            musicalKey = "C Minor",
            chordMidiNotes = listOf(48, 53, 55, 51),
            melodyMidiNotes = listOf(60, 0, 63, 67, 65, 0, 63, 60, 67, 0, 70, 67, 65, 63, 60, 0),
            kickSteps = listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0),
            snareSteps = listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0),
            hiHatSteps = listOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1),
            lyrics = "Instrumental test",
            productionNotes = "Test mix"
        )
        val pcm = engine.renderMixPcm16(
            blueprint = blueprint,
            selectedPads = setOf(StudioSoundPad.WARM_RHODES, StudioSoundPad.LOFI_KIT),
            voicePcm22k = ShortArray(2000) { 1200 }
        )
        assertTrue(pcm.size > 44100 * 10)
        assertTrue(pcm.any { it != 0.toShort() })
        engine.release()
    }
}
