package com.devekene.muzik.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devekene.muzik.ai.AiMusicComposer
import com.devekene.muzik.ai.AiSongBlueprint
import com.devekene.muzik.ai.AudioSynthEngine
import com.devekene.muzik.ai.StudioSoundPad
import com.devekene.muzik.ai.VoiceRecorderHelper
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.playback.AudioEngineLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "AiStudioViewModel"

sealed interface AiStudioGenerationState {
    data object Idle : AiStudioGenerationState
    data class Composing(val statusText: String) : AiStudioGenerationState
    data class Ready(
        val blueprint: AiSongBlueprint,
        val wavFile: File,
        val durationMs: Long,
        val hasVoiceOverlay: Boolean,
        val isDownloading: Boolean = false,
        val exportedTrack: AudioTrack? = null,
        val savedDisplayPath: String? = null
    ) : AiStudioGenerationState
    data class Error(val message: String) : AiStudioGenerationState
}

class AiStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = AudioEngineLocator.getRepository(appContext)
    private val voiceRecorder = VoiceRecorderHelper(appContext)
    private val synthEngine = AudioSynthEngine()
    private val composer = AiMusicComposer()

    val isRecordingVoice: StateFlow<Boolean> = voiceRecorder.isRecording
    val voiceAmplitude: StateFlow<Float> = voiceRecorder.amplitude
    val voiceDurationMs: StateFlow<Long> = voiceRecorder.recordedDurationMs
    val hasVoiceRecording: StateFlow<Boolean> = voiceRecorder.hasRecording
    val isVoicePreviewPlaying: StateFlow<Boolean> = voiceRecorder.isPreviewPlaying
    val voiceError: StateFlow<String?> = voiceRecorder.errorMessage
    val isMixPlaying: StateFlow<Boolean> = synthEngine.isMixPlaying

    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()

    private val _selectedStyle = MutableStateFlow("Afrobeats")
    val selectedStyle: StateFlow<String> = _selectedStyle.asStateFlow()

    private val _bpm = MutableStateFlow(104)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _selectedPads = MutableStateFlow(
        setOf(StudioSoundPad.WARM_RHODES, StudioSoundPad.LOFI_KIT)
    )
    val selectedPads: StateFlow<Set<StudioSoundPad>> = _selectedPads.asStateFlow()

    private val _generationState = MutableStateFlow<AiStudioGenerationState>(AiStudioGenerationState.Idle)
    val generationState: StateFlow<AiStudioGenerationState> = _generationState.asStateFlow()

    fun updatePrompt(value: String) {
        _prompt.value = value
    }

    fun selectStyle(style: String) {
        _selectedStyle.value = style
    }

    fun updateBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(65, 160)
    }

    fun togglePad(pad: StudioSoundPad) {
        val current = _selectedPads.value.toMutableSet()
        val added = if (current.contains(pad)) {
            if (current.size > 1) current.remove(pad)
            false
        } else {
            current.add(pad)
            true
        }
        _selectedPads.value = current
        if (added) {
            synthEngine.playPadPreview(pad)
        }
    }

    fun auditionPad(pad: StudioSoundPad) {
        synthEngine.playPadPreview(pad)
    }

    fun hasRecordPermission(): Boolean = voiceRecorder.hasRecordPermission()

    fun startVoiceRecording() {
        synthEngine.stopPlayback()
        voiceRecorder.startRecording()
    }

    fun stopVoiceRecording() {
        voiceRecorder.stopRecording()
    }

    fun toggleVoicePreview() {
        synthEngine.stopPlayback()
        voiceRecorder.togglePreviewVoice()
    }

    fun clearVoiceRecording() {
        voiceRecorder.clearRecording()
    }

    fun generateMusic(onStatusMessage: (String) -> Unit) {
        if (_generationState.value is AiStudioGenerationState.Composing) return
        if (isRecordingVoice.value) {
            voiceRecorder.stopRecording()
        }
        voiceRecorder.stopPreview()
        synthEngine.stopPlayback()

        viewModelScope.launch {
            _generationState.value = AiStudioGenerationState.Composing(
                if (hasVoiceRecording.value) {
                    "Analyzing your voice take and composing arrangement…"
                } else {
                    "Composing custom chords, drums & melody with AI Studio…"
                }
            )
            try {
                val wavBytes = voiceRecorder.readWavBytes()
                val blueprint = composer.composeTrack(
                    prompt = _prompt.value,
                    style = _selectedStyle.value,
                    targetBpm = _bpm.value,
                    selectedSounds = _selectedPads.value.toList(),
                    voiceWavBytes = wavBytes
                )

                _generationState.value = AiStudioGenerationState.Composing(
                    "Mixing instruments${if (wavBytes != null) " & vocal track" else ""} to studio WAV…"
                )

                val outDir = File(appContext.filesDir, "ai_studio").apply { mkdirs() }
                val outFile = File(outDir, "ai_mix_${System.currentTimeMillis()}.wav")
                val voicePcm = voiceRecorder.getRecordedPcm22k()
                val durationMs = synthEngine.synthesizeMixToWav(
                    blueprint = blueprint,
                    selectedPads = _selectedPads.value,
                    voicePcm22k = voicePcm,
                    outputFile = outFile
                )

                _generationState.value = AiStudioGenerationState.Ready(
                    blueprint = blueprint,
                    wavFile = outFile,
                    durationMs = durationMs,
                    hasVoiceOverlay = voicePcm != null
                )
                onStatusMessage("\"${blueprint.title}\" is ready to preview or download.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Studio generation failed", e)
                _generationState.value = AiStudioGenerationState.Error(
                    e.localizedMessage ?: "Failed to create track in AI Studio."
                )
            }
        }
    }

    fun toggleMixPlayback() {
        val ready = _generationState.value as? AiStudioGenerationState.Ready ?: return
        voiceRecorder.stopPreview()
        synthEngine.togglePlayMixWav(ready.wavFile)
    }

    fun downloadTrack(
        onDownloadedToLibrary: (AudioTrack) -> Unit,
        onStatusMessage: (String) -> Unit
    ) {
        val ready = _generationState.value as? AiStudioGenerationState.Ready ?: return
        if (ready.isDownloading) return

        viewModelScope.launch {
            _generationState.value = ready.copy(isDownloading = true)
            try {
                val exported = synthEngine.exportAndDownloadTrack(
                    context = appContext,
                    repository = repository,
                    sourceWavFile = ready.wavFile,
                    blueprint = ready.blueprint,
                    durationMs = ready.durationMs
                )
                _generationState.value = ready.copy(
                    isDownloading = false,
                    exportedTrack = exported.libraryTrack,
                    savedDisplayPath = exported.displayLocation
                )
                onDownloadedToLibrary(exported.libraryTrack)
                onStatusMessage("Downloaded to ${exported.displayLocation}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export AI Studio track", e)
                _generationState.value = ready.copy(isDownloading = false)
                onStatusMessage(e.localizedMessage ?: "Download failed.")
            }
        }
    }

    fun buildShareIntent(): Intent? {
        val ready = _generationState.value as? AiStudioGenerationState.Ready ?: return null
        val shareUri: Uri = runCatching {
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                ready.wavFile
            )
        }.getOrNull() ?: return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, ready.blueprint.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onCleared() {
        voiceRecorder.release()
        synthEngine.release()
        super.onCleared()
    }
}
