package com.devekene.muzik.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devekene.muzik.ai.StudioSoundPad
import com.devekene.muzik.data.model.AudioTrack
import com.devekene.muzik.ui.viewmodel.AiStudioGenerationState
import com.devekene.muzik.ui.viewmodel.AiStudioViewModel
import java.util.Locale
import kotlin.math.roundToInt

private val STUDIO_STYLES = listOf(
    "Afrobeats",
    "Lo-Fi Chill",
    "R&B Soul",
    "Electronic Pop",
    "Acoustic Indie",
    "Hip-Hop Trap"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioScreen(
    bottomContentPadding: Dp,
    onPlayTrackInPlayer: (AudioTrack) -> Unit,
    onStatusMessage: (String) -> Unit,
    aiStudioViewModel: AiStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val isRecordingVoice by aiStudioViewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val voiceAmplitude by aiStudioViewModel.voiceAmplitude.collectAsStateWithLifecycle()
    val voiceDurationMs by aiStudioViewModel.voiceDurationMs.collectAsStateWithLifecycle()
    val hasVoiceRecording by aiStudioViewModel.hasVoiceRecording.collectAsStateWithLifecycle()
    val isVoicePreviewPlaying by aiStudioViewModel.isVoicePreviewPlaying.collectAsStateWithLifecycle()
    val voiceError by aiStudioViewModel.voiceError.collectAsStateWithLifecycle()
    val isMixPlaying by aiStudioViewModel.isMixPlaying.collectAsStateWithLifecycle()

    val prompt by aiStudioViewModel.prompt.collectAsStateWithLifecycle()
    val selectedStyle by aiStudioViewModel.selectedStyle.collectAsStateWithLifecycle()
    val bpm by aiStudioViewModel.bpm.collectAsStateWithLifecycle()
    val selectedPads by aiStudioViewModel.selectedPads.collectAsStateWithLifecycle()
    val generationState by aiStudioViewModel.generationState.collectAsStateWithLifecycle()

    var micPermissionDenied by remember { mutableStateOf(false) }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            micPermissionDenied = false
            aiStudioViewModel.startVoiceRecording()
        } else {
            micPermissionDenied = true
            onStatusMessage("Microphone permission is needed to record your voice.")
        }
    }

    fun handleRecordToggle() {
        if (isRecordingVoice) {
            aiStudioViewModel.stopVoiceRecording()
        } else if (aiStudioViewModel.hasRecordPermission()) {
            micPermissionDenied = false
            aiStudioViewModel.startVoiceRecording()
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text("AI Studio", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Create music with studio sounds, voice recording & AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Voice Recorder Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isRecordingVoice) 1.5.dp else 1.dp,
                    color = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text("Voice Recording", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = when {
                                        isRecordingVoice -> "Recording live audio… tap Stop when finished"
                                        hasVoiceRecording -> "Voice take ready (${formatSec(voiceDurationMs)}) — will mix into your track"
                                        else -> "Hum a melody or sing vocals (up to 25s)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isRecordingVoice || hasVoiceRecording) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isRecordingVoice) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = formatSec(voiceDurationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isRecordingVoice) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Live Microphone Level Visualizer
                    VoiceLevelMeter(
                        amplitude = if (isRecordingVoice) voiceAmplitude else if (hasVoiceRecording) 0.25f else 0.06f,
                        isActive = isRecordingVoice
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = ::handleRecordToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingVoice) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("ai_record_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isRecordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isRecordingVoice -> "Stop Recording"
                                    hasVoiceRecording -> "Re-record"
                                    else -> "Record Voice"
                                }
                            )
                        }

                        if (hasVoiceRecording && !isRecordingVoice) {
                            FilledTonalButton(
                                onClick = aiStudioViewModel::toggleVoicePreview,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("ai_preview_voice_button")
                            ) {
                                Icon(
                                    imageVector = if (isVoicePreviewPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = "Preview voice take"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isVoicePreviewPlaying) "Stop" else "Listen")
                            }

                            IconButton(
                                onClick = aiStudioViewModel::clearVoiceRecording,
                                modifier = Modifier.testTag("ai_clear_voice_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete voice recording"
                                )
                            }
                        }
                    }

                    if (micPermissionDenied || voiceError != null) {
                        Text(
                            text = voiceError
                                ?: "Microphone access was denied. Enable Microphone permission in device settings to record vocals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 2. Studio Sound Palette Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Sound Palette", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Tap a pad to select for your mix, or tap speaker to audition",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val pads = StudioSoundPad.entries
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (rowIndex in pads.indices step 2) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val firstPad = pads[rowIndex]
                                SoundPadTile(
                                    pad = firstPad,
                                    selected = selectedPads.contains(firstPad),
                                    onToggle = { aiStudioViewModel.togglePad(firstPad) },
                                    onAudition = { aiStudioViewModel.auditionPad(firstPad) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (rowIndex + 1 < pads.size) {
                                    val secondPad = pads[rowIndex + 1]
                                    SoundPadTile(
                                        pad = secondPad,
                                        selected = selectedPads.contains(secondPad),
                                        onToggle = { aiStudioViewModel.togglePad(secondPad) },
                                        onAudition = { aiStudioViewModel.auditionPad(secondPad) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Creative Direction + Generate CTA
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Style & Prompt", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = aiStudioViewModel::updatePrompt,
                        label = { Text("Describe your song idea") },
                        placeholder = { Text("e.g. Warm sunset groove with mellow chords and crisp percussion") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_prompt_input")
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        STUDIO_STYLES.forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { aiStudioViewModel.selectStyle(style) },
                                label = { Text(style) }
                            )
                        }
                    }

                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tempo", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "$bpm BPM",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = bpm.toFloat(),
                            onValueChange = { aiStudioViewModel.updateBpm(it.roundToInt()) },
                            valueRange = 65f..160f
                        )
                    }

                    val isComposing = generationState is AiStudioGenerationState.Composing
                    Button(
                        onClick = { aiStudioViewModel.generateMusic(onStatusMessage) },
                        enabled = !isComposing,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("ai_generate_button")
                    ) {
                        if (isComposing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isComposing) "Composing Track…" else "Create Track with AI Studio"
                        )
                    }
                }
            }
        }

        // 4. Generation Status / Output Card
        item {
            AnimatedVisibility(visible = generationState !is AiStudioGenerationState.Idle) {
                when (val state = generationState) {
                    is AiStudioGenerationState.Idle -> Unit

                    is AiStudioGenerationState.Composing -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.padding(18.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Creating Your Track", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = state.statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is AiStudioGenerationState.Error -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_error_card")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text("Generation Failed", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                OutlinedButton(
                                    onClick = { aiStudioViewModel.generateMusic(onStatusMessage) },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }

                    is AiStudioGenerationState.Ready -> {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_ready_card")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.blueprint.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${state.blueprint.style} • ${state.blueprint.bpm} BPM • ${state.blueprint.musicalKey} • ${formatSec(state.durationMs)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (state.hasVoiceOverlay) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(
                                                text = "Voice + AI Mix",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = state.blueprint.productionNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "LYRICS & VOCAL GUIDE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.blueprint.lyrics,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilledTonalButton(
                                        onClick = aiStudioViewModel::toggleMixPlayback,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("ai_play_mix_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isMixPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isMixPlaying) "Stop" else "Preview Mix")
                                    }

                                    Button(
                                        onClick = {
                                            aiStudioViewModel.downloadTrack(
                                                onDownloadedToLibrary = { _ -> },
                                                onStatusMessage = onStatusMessage
                                            )
                                        },
                                        enabled = !state.isDownloading,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("ai_download_button")
                                    ) {
                                        if (state.isDownloading) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (state.savedDisplayPath != null) "Save Again" else "Download")
                                    }

                                    IconButton(
                                        onClick = {
                                            val shareIntent = aiStudioViewModel.buildShareIntent()
                                            if (shareIntent != null) {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent.createChooser(shareIntent, "Share ${state.blueprint.title}")
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("ai_share_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Share generated track"
                                        )
                                    }
                                }

                                state.exportedTrack?.let { track ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Saved to ${state.savedDisplayPath ?: "Device Music"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            FilledTonalButton(
                                                onClick = {
                                                    aiStudioViewModel.toggleMixPlayback().takeIf { isMixPlaying }
                                                    onPlayTrackInPlayer(track)
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Rounded.GraphicEq,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Play")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundPadTile(
    pad: StudioSoundPad,
    selected: Boolean,
    onToggle: () -> Unit,
    onAudition: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle)
            .testTag("sound_pad_${pad.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pad.label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pad.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onAudition,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Audition ${pad.label}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceLevelMeter(
    amplitude: Float,
    isActive: Boolean
) {
    val activeColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(inactiveColor.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.56f).coerceAtLeast(3f)
        for (i in 0 until barCount) {
            val waveEnvelope = kotlin.math.sin((i / barCount.toFloat()) * Math.PI).toFloat()
            val heightFraction = (0.18f + amplitude * 0.82f * (0.45f + 0.55f * waveEnvelope)).coerceIn(0.14f, 1f)
            val barH = size.height * heightFraction
            val topY = (size.height - barH) / 2f
            drawRoundRect(
                color = if (isActive || amplitude > 0.1f) activeColor else inactiveColor,
                topLeft = Offset(x = i * spacing, y = topY),
                size = Size(width = barWidth, height = barH),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

private fun formatSec(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
