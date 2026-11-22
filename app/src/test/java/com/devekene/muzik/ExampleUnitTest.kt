package com.devekene.muzik

import com.devekene.muzik.ai.AiSongBlueprint
import com.devekene.muzik.ai.AudioSynthEngine
import com.devekene.muzik.ai.StudioSoundPad
import com.devekene.muzik.ai.buildWavHeader
import com.devekene.muzik.ai.midiToFrequencyHz
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.playback.EqualizerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun wavHeaderAndSynthEngineRenderValidAudio() {
        assertEquals(440.0, midiToFrequencyHz(69), 0.01)
        val header = buildWavHeader(pcmDataSizeBytes = 1000, sampleRate = 44100)
        assertEquals(44, header.size)
        assertEquals('R'.code.toByte(), header[0])
        assertEquals('I'.code.toByte(), header[1])
        assertEquals('W'.code.toByte(), header[8])
        assertEquals('E'.code.toByte(), header[11])

        val engine = AudioSynthEngine()
        val blueprint = AiSongBlueprint(
            title = "Sunset Groove",
            style = "Afrobeats",
            bpm = 108,
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

    @Test
    fun equalizerManagerAppliesPresetsAndCustomBands() {
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
        assertEquals("Warm Bass", manager.state.value.activePresetName)
        assertEquals(550, manager.state.value.bands[0].levelMb)
        manager.setBandLevel(2, 400)
        assertEquals("Custom", manager.state.value.activePresetName)
        assertEquals(400, manager.state.value.bands[2].levelMb)
    }
}
