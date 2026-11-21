package com.devekene.muzik.playback

import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.devekene.muzik.data.model.EqBandUi
import com.devekene.muzik.data.model.EqPreset
import com.devekene.muzik.data.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class EqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = AudioManager.ERROR

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId == AudioManager.ERROR || audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null) return

        releaseEffectsOnly()
        currentSessionId = audioSessionId

        val currentBands = _state.value.bands
        var attached = false

        runCatching {
            val eq = Equalizer(0, audioSessionId)
            val numBands = eq.numberOfBands.toInt().coerceAtLeast(1)
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range.getOrNull(1)?.toInt() ?: 1500

            val bands = (0 until numBands).map { i ->
                val bandIdx = i.toShort()
                val centerHz = (eq.getCenterFreq(bandIdx) / 1000).coerceAtLeast(20)
                val targetLevel = currentBands.getOrNull(i)?.levelMb?.coerceIn(minMb, maxMb) ?: 0
                runCatching { eq.setBandLevel(bandIdx, targetLevel.toShort()) }
                EqBandUi(
                    index = i,
                    centerFreqHz = centerHz,
                    label = formatFreqLabel(centerHz),
                    levelMb = targetLevel
                )
            }
            eq.enabled = _state.value.isEnabled
            equalizer = eq
            attached = true

            _state.update {
                it.copy(
                    isHardwareAttached = true,
                    minBandLevelMb = minMb,
                    maxBandLevelMb = maxMb,
                    bands = bands
                )
            }
        }

        runCatching {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bb.setStrength(_state.value.bassBoostStrength.toShort())
            }
            bb.enabled = _state.value.isEnabled && _state.value.bassBoostStrength > 0
            bassBoost = bb
        }

        runCatching {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.setStrength(_state.value.virtualizerStrength.toShort())
            }
            virt.enabled = _state.value.isEnabled && _state.value.virtualizerStrength > 0
            virtualizer = virt
        }

        if (!attached) {
            _state.update { it.copy(isHardwareAttached = false) }
        }
    }

    fun setPresets(presets: List<EqPreset>) {
        _state.update { it.copy(presets = presets) }
    }

    fun hydrateFromPreferences(
        enabled: Boolean,
        presetName: String,
        bandLevelsMb: List<Int>,
        bassBoostStrength: Int,
        virtualizerStrength: Int
    ) {
        _state.update { current ->
            val updatedBands = current.bands.mapIndexed { idx, band ->
                val lvl = bandLevelsMb.getOrElse(idx) { band.levelMb }
                    .coerceIn(current.minBandLevelMb, current.maxBandLevelMb)
                runCatching { equalizer?.setBandLevel(idx.toShort(), lvl.toShort()) }
                band.copy(levelMb = lvl)
            }
            current.copy(
                isEnabled = enabled,
                activePresetName = presetName,
                bands = updatedBands,
                bassBoostStrength = bassBoostStrength.coerceIn(0, 1000),
                virtualizerStrength = virtualizerStrength.coerceIn(0, 1000)
            )
        }
        syncEffectsEnabledState()
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
        syncEffectsEnabledState()
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val current = _state.value
        val clamped = levelMb.coerceIn(current.minBandLevelMb, current.maxBandLevelMb)
        runCatching {
            equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        }
        _state.update { state ->
            state.copy(
                activePresetName = "Custom",
                bands = state.bands.map { band ->
                    if (band.index == bandIndex) band.copy(levelMb = clamped) else band
                }
            )
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) bb.setStrength(clamped.toShort())
                bb.enabled = _state.value.isEnabled && clamped > 0
            }
        }
        _state.update { it.copy(bassBoostStrength = clamped) }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) virt.setStrength(clamped.toShort())
                virt.enabled = _state.value.isEnabled && clamped > 0
            }
        }
        _state.update { it.copy(virtualizerStrength = clamped) }
    }

    fun applyPreset(preset: EqPreset) {
        val current = _state.value
        val updatedBands = current.bands.mapIndexed { idx, band ->
            val rawLevel = preset.bandLevelsMb.getOrElse(idx) { 0 }
            val clamped = rawLevel.coerceIn(current.minBandLevelMb, current.maxBandLevelMb)
            runCatching { equalizer?.setBandLevel(idx.toShort(), clamped.toShort()) }
            band.copy(levelMb = clamped)
        }
        val clampedBass = preset.bassBoostStrength.coerceIn(0, 1000)
        val clampedVirt = preset.virtualizerStrength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) bb.setStrength(clampedBass.toShort())
            }
            virtualizer?.let { virt ->
                if (virt.strengthSupported) virt.setStrength(clampedVirt.toShort())
            }
        }
        _state.update {
            it.copy(
                activePresetName = preset.name,
                bands = updatedBands,
                bassBoostStrength = clampedBass,
                virtualizerStrength = clampedVirt
            )
        }
        syncEffectsEnabledState()
    }

    private fun syncEffectsEnabledState() {
        val state = _state.value
        runCatching { equalizer?.enabled = state.isEnabled }
        runCatching {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) bb.setStrength(state.bassBoostStrength.toShort())
                bb.enabled = state.isEnabled && state.bassBoostStrength > 0
            }
        }
        runCatching {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) virt.setStrength(state.virtualizerStrength.toShort())
                virt.enabled = state.isEnabled && state.virtualizerStrength > 0
            }
        }
    }

    private fun releaseEffectsOnly() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    fun release() {
        releaseEffectsOnly()
        currentSessionId = AudioManager.ERROR
    }

    private fun formatFreqLabel(hz: Int): String {
        return if (hz >= 1000) {
            val khz = hz / 1000.0
            if (khz % 1.0 == 0.0) "${khz.toInt()}kHz" else "${((khz * 10.0).roundToInt() / 10.0)}kHz"
        } else {
            "${hz}Hz"
        }
    }
}
