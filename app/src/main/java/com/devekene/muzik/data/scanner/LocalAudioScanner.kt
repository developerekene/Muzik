package com.devekene.muzik.data.scanner

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.devekene.muzik.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class LocalAudioScanner(private val context: Context) {

    suspend fun scanDeviceMediaStore(): List<TrackEntity> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val genreMap = buildGenreLookupMap()

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.AudioColumns.GENRE)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val results = mutableListOf<TrackEntity>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val displayCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.AudioColumns.GENRE)
                } else -1

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: cursor.getString(displayCol)?.substringBeforeLast('.')
                        ?: "Untitled Track"
                    val rawArtist = cursor.getString(artistCol)
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumCol)
                        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
                        ?: "Singles & Local"
                    val albumId = cursor.getLong(albumIdCol)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val displayName = cursor.getString(displayCol) ?: ""
                    val dateAdded = cursor.getLong(dateCol) * 1000L

                    val genreFromCol = if (genreCol >= 0) cursor.getString(genreCol) else null
                    val rawGenre = genreFromCol?.takeIf { it.isNotBlank() }
                        ?: genreMap[mediaId]
                        ?: inferGenreFromMimeOrPath(displayName, mimeType)

                    val albumArtUri = if (albumId > 0L) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } else null

                    val audioSpecs = probeAudioSpecs(contentUri, mimeType, displayName)

                    results.add(
                        TrackEntity(
                            id = mediaId,
                            uriString = contentUri.toString(),
                            title = rawTitle,
                            artist = rawArtist,
                            album = rawAlbum,
                            genre = rawGenre.trim().ifBlank { "General" },
                            durationMs = durationMs,
                            albumArtUri = albumArtUri,
                            mimeType = mimeType,
                            formatLabel = audioSpecs.formatLabel,
                            sampleRateHz = audioSpecs.sampleRateHz,
                            bitrateKbps = audioSpecs.bitrateKbps,
                            isLossless = audioSpecs.isLossless,
                            isOfflineAvailable = true,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        }
        results
    }

    suspend fun extractFromDocumentUri(uri: Uri): TrackEntity? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: uri.lastPathSegment ?: "Imported Audio"

        val mimeType = resolver.getType(uri) ?: guessMimeFromFilename(displayName)
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var genre: String? = null
        var durationMs = 0L
        var bitrateKbps = 0

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            bitrateKbps = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull() ?: 0) / 1000
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        val specs = probeAudioSpecs(uri, mimeType, displayName, fallbackBitrateKbps = bitrateKbps)
        val stableId = -abs(uri.toString().hashCode().toLong()).coerceAtLeast(1L)

        TrackEntity(
            id = if (stableId == 0L) -1L else stableId,
            uriString = uri.toString(),
            title = title?.takeIf { it.isNotBlank() } ?: displayName.substringBeforeLast('.'),
            artist = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
            album = album?.takeIf { it.isNotBlank() } ?: "Imported Library",
            genre = genre?.takeIf { it.isNotBlank() } ?: "Hi-Fi Import",
            durationMs = durationMs,
            albumArtUri = null,
            mimeType = mimeType,
            formatLabel = specs.formatLabel,
            sampleRateHz = specs.sampleRateHz,
            bitrateKbps = specs.bitrateKbps,
            isLossless = specs.isLossless,
            isOfflineAvailable = true,
            dateAdded = System.currentTimeMillis()
        )
    }

    fun buildStreamTrack(url: String, customTitle: String?, customArtist: String?): TrackEntity {
        val lower = url.lowercase()
        val formatLabel = when {
            lower.contains(".m3u8") -> "HLS"
            lower.contains(".mpd") -> "DASH"
            lower.contains(".flac") -> "FLAC"
            lower.contains(".wav") -> "WAV"
            lower.contains(".aac") || lower.contains(".m4a") -> "AAC"
            lower.contains(".opus") -> "OPUS"
            lower.contains(".ogg") -> "OGG"
            else -> "STREAM"
        }
        val mimeType = when (formatLabel) {
            "HLS" -> "application/x-mpegURL"
            "DASH" -> "application/dash+xml"
            "FLAC" -> "audio/flac"
            "WAV" -> "audio/wav"
            "AAC" -> "audio/mp4"
            "OPUS" -> "audio/opus"
            "OGG" -> "audio/ogg"
            else -> "audio/mpeg"
        }
        val isLossless = formatLabel in setOf("FLAC", "WAV")
        val fallbackTitle = url.substringAfterLast('/').substringBefore('?').ifBlank { "Hi-Res Audio Stream" }
        val stableId = -abs(url.hashCode().toLong()).coerceAtLeast(100_000L)

        return TrackEntity(
            id = stableId,
            uriString = url.trim(),
            title = customTitle?.takeIf { it.isNotBlank() } ?: fallbackTitle,
            artist = customArtist?.takeIf { it.isNotBlank() } ?: "Hi-Res Stream",
            album = "Live & Online Streams",
            genre = "Streaming",
            durationMs = 0L,
            albumArtUri = null,
            mimeType = mimeType,
            formatLabel = formatLabel,
            sampleRateHz = if (isLossless) 96000 else 48000,
            bitrateKbps = if (isLossless) 2304 else 320,
            isLossless = isLossless,
            isOfflineAvailable = false,
            dateAdded = System.currentTimeMillis()
        )
    }

    private fun buildGenreLookupMap(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        val resolver = context.contentResolver
        runCatching {
            resolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null,
                null,
                null
            )?.use { genreCursor ->
                val gIdCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val gNameCol = genreCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genreCursor.moveToNext()) {
                    val genreId = genreCursor.getLong(gIdCol)
                    val genreName = genreCursor.getString(gNameCol) ?: continue
                    if (genreName.isBlank()) continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    resolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null
                    )?.use { memberCursor ->
                        val aIdCol = memberCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (memberCursor.moveToNext()) {
                            map[memberCursor.getLong(aIdCol)] = genreName
                        }
                    }
                }
            }
        }
        return map
    }

    private data class AudioSpecs(
        val formatLabel: String,
        val sampleRateHz: Int,
        val bitrateKbps: Int,
        val isLossless: Boolean
    )

    private fun probeAudioSpecs(
        uri: Uri,
        mimeType: String,
        displayName: String,
        fallbackBitrateKbps: Int = 0
    ): AudioSpecs {
        var sampleRateHz = 44100
        var bitrateKbps = fallbackBitrateKbps

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (bitrateKbps == 0 && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrateKbps = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                    }
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { extractor.release() }
        }

        val lowerName = displayName.lowercase()
        val lowerMime = mimeType.lowercase()
        val formatLabel = when {
            lowerMime.contains("flac") || lowerName.endsWith(".flac") -> "FLAC"
            lowerMime.contains("wav") || lowerMime.contains("x-wav") || lowerName.endsWith(".wav") -> "WAV"
            lowerMime.contains("alac") || lowerName.endsWith(".alac") -> "ALAC"
            lowerMime.contains("aiff") || lowerName.endsWith(".aiff") -> "AIFF"
            lowerMime.contains("opus") || lowerName.endsWith(".opus") -> "OPUS"
            lowerMime.contains("ogg") || lowerMime.contains("vorbis") || lowerName.endsWith(".ogg") -> "OGG"
            lowerMime.contains("mp4") || lowerMime.contains("aac") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") -> "AAC"
            lowerMime.contains("mpeg") || lowerName.endsWith(".mp3") -> "MP3"
            else -> "AUDIO"
        }

        val isLossless = formatLabel in setOf("FLAC", "WAV", "ALAC", "AIFF") || sampleRateHz >= 48000 && formatLabel == "AUDIO"
        return AudioSpecs(
            formatLabel = formatLabel,
            sampleRateHz = sampleRateHz,
            bitrateKbps = bitrateKbps,
            isLossless = isLossless
        )
    }

    private fun inferGenreFromMimeOrPath(displayName: String, mimeType: String): String {
        val lower = "$displayName $mimeType".lowercase()
        return when {
            lower.contains("flac") || lower.contains("wav") -> "Hi-Fi Lossless"
            else -> "Local Music"
        }
    }

    private fun guessMimeFromFilename(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".flac") -> "audio/flac"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".m4a") || lower.endsWith(".aac") -> "audio/mp4"
            lower.endsWith(".opus") -> "audio/opus"
            lower.endsWith(".ogg") -> "audio/ogg"
            else -> "audio/mpeg"
        }
    }
}
