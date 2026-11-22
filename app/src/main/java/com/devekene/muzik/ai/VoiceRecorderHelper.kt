package com.devekene.muzik.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

private const val TAG = "VoiceRecorderHelper"
private const val VOICE_SAMPLE_RATE = 22050
private const val MAX_VOICE_SECONDS = 25

class VoiceRecorderHelper(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var recordJob: Job? = null
    private var previewJob: Job? = null
    private var activeRecorder: AudioRecord? = null
    private var activePreviewTrack: AudioTrack? = null

    private val recordedPcmSamples = mutableListOf<Short>()
    private val wavFile: File get() = File(context.cacheDir, "ai_studio_voice_take.wav")

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _recordedDurationMs = MutableStateFlow(0L)
    val recordedDurationMs: StateFlow<Long> = _recordedDurationMs.asStateFlow()

    private val _hasRecording = MutableStateFlow(false)
    val hasRecording: StateFlow<Boolean> = _hasRecording.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (_isRecording.value) return
        if (!hasRecordPermission()) {
            _errorMessage.value = "Microphone permission is required to record your voice."
            return
        }

        stopPreview()
        _errorMessage.value = null
        recordedPcmSamples.clear()
        _hasRecording.value = false
        _recordedDurationMs.value = 0L

        val minBufBytes = AudioRecord.getMinBufferSize(
            VOICE_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(VOICE_SAMPLE_RATE / 4)

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                VOICE_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufBytes * 2
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create AudioRecord", e)
            _errorMessage.value = "Unable to access device microphone."
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            _errorMessage.value = "Microphone initialization failed."
            return
        }

        activeRecorder = recorder
        _isRecording.value = true

        recordJob = scope.launch {
            val readBuffer = ShortArray(1024)
            val maxSamples = VOICE_SAMPLE_RATE * MAX_VOICE_SECONDS
            try {
                recorder.startRecording()
                while (isActive && _isRecording.value && recordedPcmSamples.size < maxSamples) {
                    val readCount = recorder.read(readBuffer, 0, readBuffer.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        synchronized(recordedPcmSamples) {
                            for (i in 0 until readCount) {
                                val s = readBuffer[i]
                                if (recordedPcmSamples.size < maxSamples) {
                                    recordedPcmSamples.add(s)
                                }
                                sumSquares += (s * s).toDouble()
                            }
                        }
                        val rms = sqrt(sumSquares / readCount) / Short.MAX_VALUE.toDouble()
                        _amplitude.value = (rms * 3.2).toFloat().coerceIn(0f, 1f)
                        _recordedDurationMs.value = (recordedPcmSamples.size * 1000L) / VOICE_SAMPLE_RATE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during voice recording", e)
                _errorMessage.value = e.localizedMessage ?: "Voice recording stopped unexpectedly."
            } finally {
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
                activeRecorder = null
                _isRecording.value = false
                _amplitude.value = 0f
                val snapshot = synchronized(recordedPcmSamples) { recordedPcmSamples.toShortArray() }
                if (snapshot.isNotEmpty()) {
                    writePcmToWav(snapshot, VOICE_SAMPLE_RATE, wavFile)
                    _recordedDurationMs.value = (snapshot.size * 1000L) / VOICE_SAMPLE_RATE
                    _hasRecording.value = true
                }
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun togglePreviewVoice() {
        if (_isPreviewPlaying.value) {
            stopPreview()
            return
        }
        val samples = getRecordedPcm22k() ?: return
        if (samples.isEmpty()) return

        stopPreview()
        _isPreviewPlaying.value = true
        previewJob = scope.launch {
            val byteBuf = shortsToLittleEndianBytes(samples)
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
                            .setSampleRate(VOICE_SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(byteBuf.size.coerceAtLeast(4096))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preview voice take", e)
                _isPreviewPlaying.value = false
                return@launch
            }

            activePreviewTrack = track
            try {
                track.write(byteBuf, 0, byteBuf.size)
                track.play()
                val durationMs = (samples.size * 1000L) / VOICE_SAMPLE_RATE
                val startTime = System.currentTimeMillis()
                while (isActive && System.currentTimeMillis() - startTime < durationMs + 150L) {
                    kotlinx.coroutines.delay(100L)
                }
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
                if (activePreviewTrack === track) {
                    activePreviewTrack = null
                }
                _isPreviewPlaying.value = false
            }
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        runCatching { activePreviewTrack?.stop() }
        runCatching { activePreviewTrack?.release() }
        activePreviewTrack = null
        _isPreviewPlaying.value = false
    }

    fun clearRecording() {
        stopRecording()
        stopPreview()
        synchronized(recordedPcmSamples) {
            recordedPcmSamples.clear()
        }
        runCatching { wavFile.delete() }
        _hasRecording.value = false
        _recordedDurationMs.value = 0L
        _amplitude.value = 0f
    }

    fun getRecordedPcm22k(): ShortArray? {
        synchronized(recordedPcmSamples) {
            return if (recordedPcmSamples.isEmpty()) null else recordedPcmSamples.toShortArray()
        }
    }

    suspend fun readWavBytes(): ByteArray? = withContext(Dispatchers.IO) {
        if (!_hasRecording.value || !wavFile.exists()) return@withContext null
        runCatching { wavFile.readBytes() }.getOrNull()
    }

    fun release() {
        stopRecording()
        stopPreview()
        scope.cancel()
    }

    private fun writePcmToWav(samples: ShortArray, sampleRate: Int, target: File) {
        val pcmBytes = shortsToLittleEndianBytes(samples)
        val header = buildWavHeader(
            pcmDataSizeBytes = pcmBytes.size,
            sampleRate = sampleRate,
            channels = 1,
            bitsPerSample = 16
        )
        FileOutputStream(target).use { out ->
            out.write(header)
            out.write(pcmBytes)
        }
    }
}
