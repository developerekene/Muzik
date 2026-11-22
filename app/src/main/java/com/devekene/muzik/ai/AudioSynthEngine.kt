package com.devekene.muzik.ai

import android.content.ContentValues
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.devekene.muzik.data.model.AudioTrack as MuzikAudioTrack
import com.devekene.muzik.data.repository.MuzikRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

private const val TAG = "AudioSynthEngine"
const val STUDIO_MIX_SAMPLE_RATE = 44100

enum class StudioSoundPad(
    val id: String,
    val label: String,
    val description: String,
    val previewMidi: Int
) {
    WARM_RHODES("rhodes", "Warm Rhodes", "Soulful electric piano chords", 60),
    SUB_808("sub808", "808 Sub Bass", "Deep punchy low-end bass", 36),
    LOFI_KIT("drums", "Studio Drums", "Kick, crisp snare & hi-hat groove", 48),
    ANALOG_LEAD("lead", "Analog Synth", "Warm expressive synth melody", 67),
    ACOUSTIC_PLUCK("pluck", "Pluck Strings", "Bright rhythmic harmonic pluck", 64),
    AMBIENT_PAD("pad", "Ambient Pad", "Lush atmospheric stereo bed", 57)
}

data class ExportedAudioResult(
    val displayLocation: String,
    val contentUri: Uri,
    val libraryTrack: MuzikAudioTrack
)

fun midiToFrequencyHz(midiNote: Int): Double {
    return 440.0 * 2.0.pow((midiNote - 69) / 12.0)
}

fun buildWavHeader(
    pcmDataSizeBytes: Int,
    sampleRate: Int,
    channels: Int = 1,
    bitsPerSample: Int = 16
): ByteArray {
    val byteRate = sampleRate * channels * (bitsPerSample / 8)
    val blockAlign = channels * (bitsPerSample / 8)
    val totalDataLen = pcmDataSizeBytes + 36
    return byteArrayOf(
        'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
        (totalDataLen and 0xff).toByte(),
        ((totalDataLen shr 8) and 0xff).toByte(),
        ((totalDataLen shr 16) and 0xff).toByte(),
        ((totalDataLen shr 24) and 0xff).toByte(),
        'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
        'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
        16, 0, 0, 0,
        1, 0,
        channels.toByte(), 0,
        (sampleRate and 0xff).toByte(),
        ((sampleRate shr 8) and 0xff).toByte(),
        ((sampleRate shr 16) and 0xff).toByte(),
        ((sampleRate shr 24) and 0xff).toByte(),
        (byteRate and 0xff).toByte(),
        ((byteRate shr 8) and 0xff).toByte(),
        ((byteRate shr 16) and 0xff).toByte(),
        ((byteRate shr 24) and 0xff).toByte(),
        blockAlign.toByte(), 0,
        bitsPerSample.toByte(), 0,
        'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
        (pcmDataSizeBytes and 0xff).toByte(),
        ((pcmDataSizeBytes shr 8) and 0xff).toByte(),
        ((pcmDataSizeBytes shr 16) and 0xff).toByte(),
        ((pcmDataSizeBytes shr 24) and 0xff).toByte()
    )
}

fun shortsToLittleEndianBytes(samples: ShortArray): ByteArray {
    val bytes = ByteArray(samples.size * 2)
    for (i in samples.indices) {
        val v = samples[i].toInt()
        bytes[i * 2] = (v and 0xff).toByte()
        bytes[i * 2 + 1] = ((v shr 8) and 0xff).toByte()
    }
    return bytes
}

class AudioSynthEngine {

    private val previewScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var previewJob: Job? = null
    private var activeTrack: AudioTrack? = null

    private val _isMixPlaying = MutableStateFlow(false)
    val isMixPlaying: StateFlow<Boolean> = _isMixPlaying.asStateFlow()

    fun playPadPreview(pad: StudioSoundPad) {
        stopPlayback()
        previewJob = previewScope.launch {
            val durationSamples = (STUDIO_MIX_SAMPLE_RATE * 0.85).toInt()
            val mix = DoubleArray(durationSamples)
            val freq = midiToFrequencyHz(pad.previewMidi)
            val rng = Random(pad.ordinal + 42)

            for (i in 0 until durationSamples) {
                val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                val sample = when (pad) {
                    StudioSoundPad.WARM_RHODES -> {
                        val env = exp(-t * 3.2) * (1.0 - exp(-t * 90.0))
                        val f2 = midiToFrequencyHz(pad.previewMidi + 4)
                        val f3 = midiToFrequencyHz(pad.previewMidi + 7)
                        (sin(2 * PI * freq * t) + 0.55 * sin(2 * PI * f2 * t) + 0.45 * sin(2 * PI * f3 * t) +
                            0.25 * sin(2 * PI * freq * 2.0 * t) * exp(-t * 8.0)) * env * 0.42
                    }
                    StudioSoundPad.SUB_808 -> {
                        val pitchSweep = 1.0 + 1.8 * exp(-t * 28.0)
                        val env = exp(-t * 3.8) * (1.0 - exp(-t * 140.0))
                        tanh(sin(2 * PI * freq * pitchSweep * t) * 1.45) * env * 0.72
                    }
                    StudioSoundPad.LOFI_KIT -> {
                        val kickEnv = exp(-t * 14.0)
                        val kickFreq = 46.0 + 95.0 * exp(-t * 34.0)
                        val kick = sin(2 * PI * kickFreq * t) * kickEnv
                        val snareT = t - 0.35
                        val snare = if (snareT >= 0.0) {
                            (rng.nextDouble(-1.0, 1.0) * 0.65 + sin(2 * PI * 195.0 * snareT) * 0.35) * exp(-snareT * 18.0)
                        } else 0.0
                        (kick * 0.75 + snare * 0.65)
                    }
                    StudioSoundPad.ANALOG_LEAD -> {
                        val env = exp(-t * 2.4) * (1.0 - exp(-t * 60.0))
                        val s1 = sin(2 * PI * freq * t) + 0.38 * sin(2 * PI * freq * 2.0 * t) + 0.18 * sin(2 * PI * freq * 3.0 * t)
                        val s2 = sin(2 * PI * (freq * 1.004) * t)
                        (s1 + s2 * 0.5) * env * 0.36
                    }
                    StudioSoundPad.ACOUSTIC_PLUCK -> {
                        val env = exp(-t * 5.2) * (1.0 - exp(-t * 180.0))
                        (sin(2 * PI * freq * t) + 0.48 * sin(2 * PI * freq * 2.0 * t) * exp(-t * 9.0) +
                            0.22 * sin(2 * PI * freq * 3.0 * t) * exp(-t * 14.0)) * env * 0.48
                    }
                    StudioSoundPad.AMBIENT_PAD -> {
                        val env = (1.0 - exp(-t * 4.5)) * exp(-t * 1.3)
                        val f3 = midiToFrequencyHz(pad.previewMidi + 7)
                        val f5 = midiToFrequencyHz(pad.previewMidi + 12)
                        (sin(2 * PI * freq * t) + 0.7 * sin(2 * PI * f3 * t) + 0.55 * sin(2 * PI * f5 * t)) * env * 0.35
                    }
                }
                mix[i] = sample
            }

            val pcm16 = ShortArray(durationSamples) { idx ->
                (tanh(mix[idx]) * Short.MAX_VALUE * 0.9).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcmBuffer(pcm16, STUDIO_MIX_SAMPLE_RATE, trackState = false)
        }
    }

    fun renderMixPcm16(
        blueprint: AiSongBlueprint,
        selectedPads: Set<StudioSoundPad>,
        voicePcm22k: ShortArray?
    ): ShortArray {
        val activePads = selectedPads.ifEmpty {
            setOf(StudioSoundPad.WARM_RHODES, StudioSoundPad.LOFI_KIT)
        }

        val bpm = blueprint.bpm.coerceIn(65, 160)
        val secondsPerBeat = 60.0 / bpm
        val stepDurationSec = secondsPerBeat / 4.0
        val totalBeats = 32 // 8 bars of 4/4
        val totalSteps = totalBeats * 4 // 128 sixteenth-note steps
        val baseInstrumentalSamples = (totalSteps * stepDurationSec * STUDIO_MIX_SAMPLE_RATE).toInt()
        val upsampledVoice = voicePcm22k?.let { upsample22kTo44k(it) }
        val totalSamples = maxOf(baseInstrumentalSamples, upsampledVoice?.size ?: 0)

        val mix = DoubleArray(totalSamples)
        val samplesPerStep = (stepDurationSec * STUDIO_MIX_SAMPLE_RATE).toInt().coerceAtLeast(1)
        val samplesPerChord = samplesPerStep * 16 // 1 bar per chord
        val rng = Random(blueprint.title.hashCode())

        val useChords = activePads.contains(StudioSoundPad.WARM_RHODES) ||
            activePads.contains(StudioSoundPad.AMBIENT_PAD) ||
            activePads.contains(StudioSoundPad.ACOUSTIC_PLUCK)
        val useBass = activePads.contains(StudioSoundPad.SUB_808) || activePads.contains(StudioSoundPad.LOFI_KIT)
        val useDrums = activePads.contains(StudioSoundPad.LOFI_KIT) || activePads.contains(StudioSoundPad.SUB_808)
        val useLead = activePads.contains(StudioSoundPad.ANALOG_LEAD) ||
            activePads.contains(StudioSoundPad.ACOUSTIC_PLUCK) ||
            activePads.contains(StudioSoundPad.WARM_RHODES)

        val chords = blueprint.chordMidiNotes.ifEmpty { listOf(48, 53, 55, 51) }
        val melody = blueprint.melodyMidiNotes.ifEmpty { List(16) { 0 } }

        // 1. Chord progression layer
        if (useChords) {
            val isAmbient = activePads.contains(StudioSoundPad.AMBIENT_PAD)
            var cursor = 0
            var chordIndex = 0
            while (cursor < totalSamples) {
                val rootMidi = chords[chordIndex % chords.size]
                val triad = listOf(
                    midiToFrequencyHz(rootMidi + 12),
                    midiToFrequencyHz(rootMidi + 15),
                    midiToFrequencyHz(rootMidi + 19),
                    midiToFrequencyHz(rootMidi + 22)
                )
                val len = minOf(samplesPerChord, totalSamples - cursor)
                for (i in 0 until len) {
                    val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                    val attack = if (isAmbient) 1.0 - exp(-t * 5.0) else 1.0 - exp(-t * 65.0)
                    val decay = if (isAmbient) exp(-t * 0.55) else exp(-t * 1.35)
                    val env = attack * decay
                    var v = 0.0
                    for (f in triad) {
                        v += sin(2 * PI * f * t)
                    }
                    mix[cursor + i] += v * env * 0.11
                }
                cursor += samplesPerChord
                chordIndex++
            }
        }

        // 2. Step sequencer for Drums + 808 Bass + Melody
        for (step in 0 until totalSteps) {
            val stepStart = step * samplesPerStep
            if (stepStart >= totalSamples) break
            val patternIdx = step % 16
            val chordIdx = (step / 16) % chords.size
            val rootBassMidi = (chords[chordIdx] - 12).coerceIn(28, 48)

            // Kick + 808 Sub
            if (useDrums && blueprint.kickSteps[patternIdx] == 1) {
                val kickLen = minOf((STUDIO_MIX_SAMPLE_RATE * 0.42).toInt(), totalSamples - stepStart)
                val bassFreq = midiToFrequencyHz(rootBassMidi)
                for (i in 0 until kickLen) {
                    val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                    val kickEnv = exp(-t * 11.5) * (1.0 - exp(-t * 180.0))
                    val sweepFreq = 45.0 + 95.0 * exp(-t * 36.0)
                    val kickSample = sin(2 * PI * sweepFreq * t) * kickEnv * 0.42
                    val subSample = if (useBass) {
                        sin(2 * PI * bassFreq * t) * exp(-t * 4.5) * 0.24
                    } else 0.0
                    mix[stepStart + i] += kickSample + subSample
                }
            } else if (useBass && patternIdx == 0) {
                val bassLen = minOf(samplesPerStep * 4, totalSamples - stepStart)
                val bassFreq = midiToFrequencyHz(rootBassMidi)
                for (i in 0 until bassLen) {
                    val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                    val env = exp(-t * 3.8) * (1.0 - exp(-t * 120.0))
                    mix[stepStart + i] += sin(2 * PI * bassFreq * t) * env * 0.28
                }
            }

            // Snare
            if (useDrums && blueprint.snareSteps[patternIdx] == 1) {
                val snareLen = minOf((STUDIO_MIX_SAMPLE_RATE * 0.22).toInt(), totalSamples - stepStart)
                for (i in 0 until snareLen) {
                    val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                    val env = exp(-t * 20.0)
                    val noise = rng.nextDouble(-1.0, 1.0)
                    val body = sin(2 * PI * 185.0 * t) * exp(-t * 28.0)
                    mix[stepStart + i] += (noise * 0.55 + body * 0.45) * env * 0.26
                }
            }

            // Hi-hat
            if (useDrums && blueprint.hiHatSteps[patternIdx] == 1) {
                val hatLen = minOf((STUDIO_MIX_SAMPLE_RATE * 0.075).toInt(), totalSamples - stepStart)
                var prev = 0.0
                for (i in 0 until hatLen) {
                    val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                    val env = exp(-t * 52.0)
                    val white = rng.nextDouble(-1.0, 1.0)
                    val highPassed = white - prev
                    prev = white * 0.85
                    mix[stepStart + i] += highPassed * env * 0.14
                }
            }

            // Melody note
            val note = melody[patternIdx]
            if (useLead && note > 0) {
                val noteLen = minOf((samplesPerStep * 1.7).toInt(), totalSamples - stepStart)
                val f = midiToFrequencyHz(note)
                val isPluck = activePads.contains(StudioSoundPad.ACOUSTIC_PLUCK)
                for (i in 0 until noteLen) {
                    val t = i.toDouble() / STUDIO_MIX_SAMPLE_RATE
                    val attack = 1.0 - exp(-t * 95.0)
                    val decay = if (isPluck) exp(-t * 7.5) else exp(-t * 4.2)
                    val tone = sin(2 * PI * f * t) +
                        0.32 * sin(2 * PI * f * 2.0 * t) * exp(-t * 6.0) +
                        0.12 * sin(2 * PI * f * 3.0 * t) * exp(-t * 10.0)
                    mix[stepStart + i] += tone * attack * decay * 0.22
                }
            }
        }

        // 3. Mix recorded vocal track if available (normalized so it sits cleanly on top)
        if (upsampledVoice != null && upsampledVoice.isNotEmpty()) {
            val peak = upsampledVoice.maxOfOrNull { kotlin.math.abs(it.toInt()) }?.coerceAtLeast(1) ?: 1
            val voiceGain = (0.78 * Short.MAX_VALUE / peak.toDouble()).coerceAtMost(2.5)
            val delaySamples = (STUDIO_MIX_SAMPLE_RATE * 0.18).toInt()
            for (i in upsampledVoice.indices) {
                val dry = (upsampledVoice[i].toDouble() / Short.MAX_VALUE) * voiceGain
                val echo = if (i >= delaySamples) {
                    (upsampledVoice[i - delaySamples].toDouble() / Short.MAX_VALUE) * voiceGain * 0.20
                } else 0.0
                mix[i] = mix[i] * 0.78 + dry + echo
            }
        }

        return ShortArray(totalSamples) { i ->
            val limited = tanh(mix[i] * 1.15)
            (limited * Short.MAX_VALUE * 0.92).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    suspend fun synthesizeMixToWav(
        blueprint: AiSongBlueprint,
        selectedPads: Set<StudioSoundPad>,
        voicePcm22k: ShortArray?,
        outputFile: File
    ): Long = withContext(Dispatchers.IO) {
        outputFile.parentFile?.mkdirs()
        val pcm16 = renderMixPcm16(blueprint, selectedPads, voicePcm22k)
        val pcmBytes = shortsToLittleEndianBytes(pcm16)
        val header = buildWavHeader(
            pcmDataSizeBytes = pcmBytes.size,
            sampleRate = STUDIO_MIX_SAMPLE_RATE,
            channels = 1,
            bitsPerSample = 16
        )
        FileOutputStream(outputFile).use { out ->
            out.write(header)
            out.write(pcmBytes)
        }
        (pcm16.size * 1000L) / STUDIO_MIX_SAMPLE_RATE
    }

    fun togglePlayMixWav(wavFile: File) {
        if (_isMixPlaying.value) {
            stopPlayback()
            return
        }
        if (!wavFile.exists()) return
        stopPlayback()
        previewJob = previewScope.launch {
            val allBytes = runCatching { wavFile.readBytes() }.getOrNull() ?: return@launch
            if (allBytes.size <= 44) return@launch
            val pcmBytes = allBytes.copyOfRange(44, allBytes.size)
            val shorts = ShortArray(pcmBytes.size / 2) { idx ->
                val lo = pcmBytes[idx * 2].toInt() and 0xff
                val hi = pcmBytes[idx * 2 + 1].toInt() shl 8
                (hi or lo).toShort()
            }
            playPcmBuffer(shorts, STUDIO_MIX_SAMPLE_RATE, trackState = true)
        }
    }

    private suspend fun playPcmBuffer(samples: ShortArray, sampleRate: Int, trackState: Boolean) {
        val pcmBytes = shortsToLittleEndianBytes(samples)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmBytes.size.coerceAtLeast(4096))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack init failed", e)
            if (trackState) _isMixPlaying.value = false
            return
        }

        activeTrack = track
        if (trackState) _isMixPlaying.value = true
        try {
            track.write(pcmBytes, 0, pcmBytes.size)
            track.play()
            val durationMs = (samples.size * 1000L) / sampleRate
            val startTime = System.currentTimeMillis()
            while (previewScope.isActive && System.currentTimeMillis() - startTime < durationMs + 150L) {
                delay(100L)
            }
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
            if (activeTrack === track) activeTrack = null
            if (trackState) _isMixPlaying.value = false
        }
    }

    fun stopPlayback() {
        previewJob?.cancel()
        previewJob = null
        runCatching { activeTrack?.stop() }
        runCatching { activeTrack?.release() }
        activeTrack = null
        _isMixPlaying.value = false
    }

    suspend fun exportAndDownloadTrack(
        context: Context,
        repository: MuzikRepository,
        sourceWavFile: File,
        blueprint: AiSongBlueprint,
        durationMs: Long
    ): ExportedAudioResult = withContext(Dispatchers.IO) {
        val safeName = blueprint.title
            .replace(Regex("[^A-Za-z0-9_\\- ]"), "")
            .trim()
            .ifBlank { "Muzik_AI_Track" }
            .replace(' ', '_')
        val fileName = "${safeName}_${System.currentTimeMillis()}.wav"

        var savedUri: Uri? = null
        var displayLocation = "Music/Muzik AI Studio/$fileName"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                put(MediaStore.Audio.Media.TITLE, blueprint.title)
                put(MediaStore.Audio.Media.ARTIST, "Muzik AI Studio")
                put(MediaStore.Audio.Media.ALBUM, "AI Studio Creations")
                put(MediaStore.Audio.Media.DURATION, durationMs)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Muzik AI Studio")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val insertedUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (insertedUri != null) {
                resolver.openOutputStream(insertedUri)?.use { out ->
                    sourceWavFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(insertedUri, values, null, null)
                savedUri = insertedUri
            }
        }

        if (savedUri == null) {
            val fallbackDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
                "Muzik AI Studio"
            ).apply { mkdirs() }
            val destFile = File(fallbackDir, fileName)
            sourceWavFile.copyTo(destFile, overwrite = true)
            savedUri = Uri.fromFile(destFile)
            displayLocation = destFile.name
        }

        val libraryTrack = repository.addAiGeneratedTrack(
            uriString = savedUri.toString(),
            title = blueprint.title,
            genre = blueprint.style,
            durationMs = durationMs
        )

        ExportedAudioResult(
            displayLocation = displayLocation,
            contentUri = savedUri,
            libraryTrack = libraryTrack
        )
    }

    private fun upsample22kTo44k(input: ShortArray): ShortArray {
        if (input.isEmpty()) return ShortArray(0)
        val out = ShortArray(input.size * 2)
        for (i in input.indices) {
            val curr = input[i].toInt()
            val next = if (i + 1 < input.size) input[i + 1].toInt() else curr
            out[i * 2] = curr.toShort()
            out[i * 2 + 1] = ((curr + next) / 2).toShort()
        }
        return out
    }

    fun release() {
        stopPlayback()
        previewScope.cancel()
    }
}
