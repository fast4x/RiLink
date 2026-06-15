package it.fast4x.ritune.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import okhttp3.OkHttpClient
import okhttp3.Request

data class TrackMetadata(
    val mediaId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val coverBitmap: Bitmap? = null
)

object TrackMetadataResolver {
    private val client = OkHttpClient()
    private val cache = ConcurrentHashMap<String, TrackMetadata>()

    private const val YOUTUBE_OEMBED =
        "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=%s&format=json"

    private const val MB_SEARCH =
        "https://musicbrainz.org/ws/2/recording?query=%s&fmt=json&limit=1&dismax=true"

    private const val CAA_RELEASE =
        "https://coverartarchive.org/release/%s"

    suspend fun resolve(mediaId: String): TrackMetadata? = withContext(Dispatchers.IO) {
        val id = mediaId.trim()
        if (id.isBlank()) return@withContext null

        cache[id]?.let { return@withContext it }

        val result = try {
            val youtube = fetchYouTubeOEmbed(id)
            val baseTitle = youtube?.title?.trim().orEmpty()
            val baseArtist = youtube?.authorName?.trim().orEmpty()
            val youtubeThumb = youtube?.thumbnailUrl?.trim()

            val mbMatch = if (baseTitle.isNotBlank() || baseArtist.isNotBlank()) {
                searchMusicBrainz(
                    title = baseTitle,
                    artist = baseArtist
                )
            } else {
                null
            }

            val finalTitle = mbMatch?.title?.takeIf { it.isNotBlank() }
                ?: baseTitle.ifBlank { "Riproduzione in corso" }

            val finalArtist = mbMatch?.artist?.takeIf { it.isNotBlank() }
                ?: baseArtist.ifBlank { "YouTube" }

            val coverUrl = mbMatch?.coverUrl ?: youtubeThumb
            val coverBitmap = coverUrl?.let { downloadBitmap(it) }

            TrackMetadata(
                mediaId = id,
                title = finalTitle,
                artist = finalArtist,
                coverUrl = coverUrl,
                coverBitmap = coverBitmap
            )
        } catch (_: Exception) {
            fallback(id)
        }

        if (result != null) {
            cache[id] = result
        }
        result
    }

    private fun fallback(mediaId: String): TrackMetadata {
        return TrackMetadata(
            mediaId = mediaId,
            title = "Riproduzione in corso",
            artist = "YouTube",
            coverUrl = youtubeThumbnailUrl(mediaId),
            coverBitmap = null
        )
    }

    private fun youtubeThumbnailUrl(mediaId: String): String {
        return "https://i.ytimg.com/vi/$mediaId/hqdefault.jpg"
    }

    private fun fetchYouTubeOEmbed(mediaId: String): YouTubeOEmbed? {
        val url = String.format(Locale.US, YOUTUBE_OEMBED, mediaId)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RiTune/1.0 (Android)")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            return YouTubeOEmbed(
                title = json.optString("title"),
                authorName = json.optString("author_name"),
                thumbnailUrl = json.optString("thumbnail_url")
            )
        }
    }

    private fun searchMusicBrainz(title: String, artist: String): TrackMetadata? {
        val cleanedTitle = title.trim()
        val cleanedArtist = artist.trim()

        val queryParts = buildList {
            if (cleanedTitle.isNotBlank()) add(cleanedTitle)
            if (cleanedArtist.isNotBlank()) add(cleanedArtist)
        }

        if (queryParts.isEmpty()) return null

        val query = queryParts.joinToString(" ")
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val url = String.format(Locale.US, MB_SEARCH, encodedQuery)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RiTune/1.0 (Android)")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val recordings = json.optJSONArray("recordings") ?: return null
            if (recordings.length() == 0) return null

            val recording = recordings.optJSONObject(0) ?: return null
            val mbTitle = recording.optString("title").trim()
            val mbArtist = parseArtistCredit(recording.optJSONArray("artist-credit"))
            val releaseId = firstReleaseId(recording.optJSONArray("releases"))

            val coverUrl = releaseId?.let { fetchCoverArtUrl(it) }

            return TrackMetadata(
                mediaId = "",
                title = mbTitle.ifBlank { cleanedTitle.ifBlank { "Riproduzione in corso" } },
                artist = mbArtist.ifBlank { cleanedArtist.ifBlank { "YouTube" } },
                coverUrl = coverUrl
            )
        }
    }

    private fun fetchCoverArtUrl(releaseId: String): String? {
        val url = String.format(Locale.US, CAA_RELEASE, releaseId)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RiTune/1.0 (Android)")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val images = json.optJSONArray("images") ?: return null
            if (images.length() == 0) return null

            val frontImage = findFrontImage(images) ?: images.optJSONObject(0) ?: return null
            val thumbnails = frontImage.optJSONObject("thumbnails")

            return thumbnails?.optString("500")
                ?.takeIf { it.isNotBlank() }
                ?: thumbnails?.optString("250")
                    ?.takeIf { it.isNotBlank() }
                ?: frontImage.optString("image").takeIf { it.isNotBlank() }
        }
    }

    private fun findFrontImage(images: JSONArray): JSONObject? {
        for (i in 0 until images.length()) {
            val candidate = images.optJSONObject(i) ?: continue
            if (candidate.optBoolean("front", false)) {
                return candidate
            }
        }
        return null
    }

    private fun firstReleaseId(releases: JSONArray?): String? {
        if (releases == null || releases.length() == 0) return null
        return releases.optJSONObject(0)?.optString("id")?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun parseArtistCredit(artistCredit: JSONArray?): String {
        if (artistCredit == null || artistCredit.length() == 0) return ""

        val parts = mutableListOf<String>()

        for (i in 0 until artistCredit.length()) {
            val item = artistCredit.optJSONObject(i) ?: continue
            val artist = item.optJSONObject("artist")?.optString("name").orEmpty().trim()
            val joinPhrase = item.optString("joinphrase").orEmpty()

            if (artist.isNotBlank()) {
                parts += artist + joinPhrase
            }
        }

        return parts.joinToString("").trim()
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "RiTune/1.0 (Android)")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            if (bytes.isEmpty()) return null
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private data class YouTubeOEmbed(
        val title: String,
        val authorName: String,
        val thumbnailUrl: String
    )
}