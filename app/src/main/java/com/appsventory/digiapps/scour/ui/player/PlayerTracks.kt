package com.appsventory.digiapps.scour.ui.player

import android.content.res.Resources
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import com.appsventory.digiapps.scour.R
import java.util.Locale
import kotlin.math.roundToInt

/** [override] == null means "Auto" (adaptive). */
data class QualityOption(
    val label: String,
    val override: TrackSelectionOverride?,
    val selected: Boolean
)

data class EmbeddedSubtitleOption(
    val label: String,
    val override: TrackSelectionOverride,
    val selected: Boolean
)

object PlayerTracks {

    private const val HIGH_FPS = 50
    private const val BITS_PER_KBIT = 1000

    private data class Candidate(
        val group: Tracks.Group,
        val index: Int,
        val height: Int,
        val fps: Int,
        val bitrate: Int
    )

    /** Empty when the stream offers fewer than two distinct qualities (nothing to choose). */
    fun qualityOptions(
        tracks: Tracks,
        params: TrackSelectionParameters,
        res: Resources
    ): List<QualityOption> {
        val found = ArrayList<Candidate>()
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_VIDEO) continue
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val f = group.getTrackFormat(i)
                if (f.height <= 0 && f.bitrate <= 0) continue
                found += Candidate(
                    group, i, f.height,
                    if (f.frameRate > 0f) f.frameRate.roundToInt() else 0,
                    f.bitrate.coerceAtLeast(0)
                )
            }
        }

        fun labelOf(c: Candidate): String = when {
            c.height > 0 && c.fps >= HIGH_FPS -> res.getString(R.string.player_quality_height_fps, c.height, c.fps)
            c.height > 0 -> res.getString(R.string.player_quality_height, c.height)
            else -> res.getString(R.string.player_quality_kbps, c.bitrate / BITS_PER_KBIT)
        }

        val best = found
            .groupBy { labelOf(it) }
            .values
            .map { list -> list.maxBy { it.bitrate } }
            .sortedWith(compareByDescending<Candidate> { it.height }.thenByDescending { it.bitrate })
        if (best.size < 2) return emptyList()

        val hasOverride = params.overrides.values.any { it.type == C.TRACK_TYPE_VIDEO }
        val options = ArrayList<QualityOption>()
        options += QualityOption(res.getString(R.string.player_quality_auto), null, !hasOverride)
        for (c in best) {
            val selected = hasOverride &&
                params.overrides[c.group.mediaTrackGroup]?.trackIndices?.contains(c.index) == true
            options += QualityOption(
                labelOf(c),
                TrackSelectionOverride(c.group.mediaTrackGroup, c.index),
                selected
            )
        }
        return options
    }

    fun embeddedSubtitles(tracks: Tracks, res: Resources): List<EmbeddedSubtitleOption> {
        val out = ArrayList<EmbeddedSubtitleOption>()
        var n = 0
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                n++
                val f = group.getTrackFormat(i)
                val lang = languageName(f.language)
                val extra = f.label?.takeIf { it.isNotBlank() && !it.equals(lang, ignoreCase = true) }
                val label = when {
                    lang != null && extra != null -> "$lang ($extra)"
                    lang != null -> lang
                    extra != null -> extra
                    else -> res.getString(R.string.player_subtitle_n, n)
                }
                out += EmbeddedSubtitleOption(
                    label,
                    TrackSelectionOverride(group.mediaTrackGroup, i),
                    group.isTrackSelected(i)
                )
            }
        }
        return out
    }

    fun languageName(code: String?): String? {
        if (code.isNullOrBlank() || code == "und") return null
        val name = Locale.forLanguageTag(code).displayLanguage
        return name.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
}
