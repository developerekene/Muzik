package com.devekene.muzik.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AiMusicComposer"

data class AiSongBlueprint(
    val title: String,
    val style: String,
    val bpm: Int,
    val musicalKey: String,
    val chordMidiNotes: List<Int>,
    val melodyMidiNotes: List<Int>,
    val kickSteps: List<Int>,
    val snareSteps: List<Int>,
    val hiHatSteps: List<Int>,
    val lyrics: String,
    val productionNotes: String
)

class AiMusicComposer {

    private val songBlueprintSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Original song title, 2 to 5 words"),
            "style" to Schema.string(description = "Genre or production style"),
            "bpm" to Schema.integer(description = "Tempo in BPM between 65 and 160"),
            "musicalKey" to Schema.string(description = "Musical key e.g. C Minor or F Major"),
            "chordMidiNotes" to Schema.array(
                items = Schema.integer(),
                description = "4 to 8 root MIDI notes between 45 and 67 for the chord progression"
            ),
            "melodyMidiNotes" to Schema.array(
                items = Schema.integer(),
                description = "16 MIDI note numbers between 57 and 84 (or 0 for rest) forming a 16-step melody loop"
            ),
            "kickSteps" to Schema.array(
                items = Schema.integer(),
                description = "16 integers (0 or 1) for a 16-step kick drum groove"
            ),
            "snareSteps" to Schema.array(
                items = Schema.integer(),
                description = "16 integers (0 or 1) for a 16-step snare or clap pattern"
            ),
            "hiHatSteps" to Schema.array(
                items = Schema.integer(),
                description = "16 integers (0 or 1) for a 16-step hi-hat pattern"
            ),
            "lyrics" to Schema.string(description = "4 to 8 lines of lyrics or vocal guide matching the prompt and voice recording"),
            "productionNotes" to Schema.string(description = "Short studio arrangement summary")
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = songBlueprintSchema
            }
        )
    }

    suspend fun composeTrack(
        prompt: String,
        style: String,
        targetBpm: Int,
        selectedSounds: List<StudioSoundPad>,
        voiceWavBytes: ByteArray?
    ): AiSongBlueprint {
        val soundListText = if (selectedSounds.isEmpty()) {
            "Warm Rhodes, Lo-Fi Drums"
        } else {
            selectedSounds.joinToString(", ") { "${it.label} (${it.description})" }
        }

        val instruction = buildString {
            appendLine("You are an expert music producer in Muzik AI Studio.")
            appendLine("Create an original musical arrangement as structured JSON.")
            appendLine("User creative brief: ${prompt.ifBlank { "Create an uplifting, modern melodic groove." }}")
            appendLine("Target style: $style")
            appendLine("Target tempo (BPM): $targetBpm")
            appendLine("Selected studio sounds: $soundListText")
            if (voiceWavBytes != null) {
                appendLine("Attached is the user's recorded voice take (WAV audio). Listen closely to its rhythm, pitch contour, and phrasing, and design chords, melodyMidiNotes, drum groove, and matching lyrics that complement their vocal.")
            } else {
                appendLine("No voice take was attached; design a complete instrumental arrangement with optional vocal lyrics.")
            }
            appendLine("Rules:")
            appendLine("- Return exactly 16 items in melodyMidiNotes (values 57..84 or 0 for rest).")
            appendLine("- Return 4 to 8 root MIDI notes in chordMidiNotes (values 45..67).")
            appendLine("- Return exactly 16 binary values (0 or 1) in kickSteps, snareSteps, and hiHatSteps.")
        }

        val inputContent = content {
            if (voiceWavBytes != null && voiceWavBytes.size < 18 * 1024 * 1024) {
                inlineData(voiceWavBytes, "audio/wav")
            }
            text(instruction)
        }

        try {
            val response = withTimeout(60_000L) {
                model.generateContent(inputContent)
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("AI Studio returned an empty arrangement.")
            return parseBlueprintJson(rawJson, fallbackStyle = style, fallbackBpm = targetBpm)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Studio generation timed out", e)
            throw IllegalStateException("AI composition timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Studio composition failed", e)
            throw IllegalStateException(
                e.localizedMessage ?: "Failed to compose music with AI Studio."
            )
        }
    }

    private fun parseBlueprintJson(
        rawJson: String,
        fallbackStyle: String,
        fallbackBpm: Int
    ): AiSongBlueprint {
        val obj = JSONObject(rawJson)
        val title = obj.optString("title").ifBlank { "Midnight Sketch" }
        val style = obj.optString("style").ifBlank { fallbackStyle }
        val bpm = obj.optInt("bpm", fallbackBpm).coerceIn(65, 160)
        val key = obj.optString("musicalKey").ifBlank { "C Minor" }

        val chords = obj.optJSONArray("chordMidiNotes").toIntList()
            .map { it.coerceIn(40, 72) }
            .ifEmpty { listOf(48, 53, 55, 51) }

        val melody = normalizeTo16Steps(
            obj.optJSONArray("melodyMidiNotes").toIntList().map { note ->
                if (note <= 0) 0 else note.coerceIn(52, 88)
            },
            defaultPattern = listOf(60, 0, 63, 67, 65, 0, 63, 60, 67, 0, 70, 67, 65, 63, 60, 0)
        )

        val kick = normalizeBinary16(
            obj.optJSONArray("kickSteps").toIntList(),
            defaultPattern = listOf(1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 0)
        )
        val snare = normalizeBinary16(
            obj.optJSONArray("snareSteps").toIntList(),
            defaultPattern = listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0)
        )
        val hiHat = normalizeBinary16(
            obj.optJSONArray("hiHatSteps").toIntList(),
            defaultPattern = listOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1)
        )

        val lyrics = obj.optString("lyrics").ifBlank { "Instrumental session" }
        val notes = obj.optString("productionNotes").ifBlank {
            "Multi-track studio mix rendered at $bpm BPM in $key."
        }

        return AiSongBlueprint(
            title = title,
            style = style,
            bpm = bpm,
            musicalKey = key,
            chordMidiNotes = chords,
            melodyMidiNotes = melody,
            kickSteps = kick,
            snareSteps = snare,
            hiHatSteps = hiHat,
            lyrics = lyrics,
            productionNotes = notes
        )
    }

    private fun JSONArray?.toIntList(): List<Int> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (i in 0 until length()) {
                add(optInt(i, 0))
            }
        }
    }

    private fun normalizeTo16Steps(input: List<Int>, defaultPattern: List<Int>): List<Int> {
        if (input.isEmpty()) return defaultPattern
        return List(16) { idx -> input[idx % input.size] }
    }

    private fun normalizeBinary16(input: List<Int>, defaultPattern: List<Int>): List<Int> {
        if (input.isEmpty()) return defaultPattern
        return List(16) { idx -> if (input[idx % input.size] != 0) 1 else 0 }
    }
}
