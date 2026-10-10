package com.appsventory.digiapps.scour.player

import kotlin.math.abs

enum class StreamRole { Pending, Main, Unknown, Short }

fun DetectedStream.role(): StreamRole {
    if (!probed) return StreamRole.Pending
    val i = info ?: return StreamRole.Unknown
    return when {
        i.live -> StreamRole.Main
        i.durationMs <= 0L -> StreamRole.Unknown
        i.durationMs < PlayerConfig.MIN_MAIN_DURATION_MS -> StreamRole.Short
        else -> StreamRole.Main
    }
}

object StreamRanking {

    /** Worth offering to the user right now (ads / previews and unfinished probes are not). */
    fun isEligible(s: DetectedStream): Boolean =
        s.role().let { it == StreamRole.Main || it == StreamRole.Unknown }

    /**
     * Best guess first: matches the <video> that is really playing > long VOD > live > unknown,
     * then taller, then manifests before plain files. Short clips (ads) sink to the bottom.
     * [playingMs] = duration reported by the page's playing <video>, 0 if none seen.
     */
    fun rank(streams: List<DetectedStream>, playingMs: Long): List<DetectedStream> =
        streams.sortedWith(
            compareBy<DetectedStream> { score(it, playingMs) }
                .thenByDescending { it.info?.durationMs ?: 0L }
                .thenByDescending { it.info?.height ?: 0 }
                .thenBy { it.type == StreamType.PROGRESSIVE }
        )

    private fun score(s: DetectedStream, playingMs: Long): Int {
        val d = s.info?.durationMs ?: 0L
        val matches = playingMs > 0 && d > 0 &&
            abs(d - playingMs) <= PlayerConfig.DURATION_MATCH_TOLERANCE_MS
        return when {
            matches -> 0
            s.role() == StreamRole.Main && !(s.info?.live ?: false) -> 1
            s.role() == StreamRole.Main -> 2
            s.role() == StreamRole.Unknown -> 3
            s.role() == StreamRole.Pending -> 4
            else -> 5
        }
    }
}
