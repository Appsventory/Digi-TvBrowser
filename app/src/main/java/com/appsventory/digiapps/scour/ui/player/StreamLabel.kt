package com.appsventory.digiapps.scour.ui.player

import android.content.res.Resources
import com.appsventory.digiapps.scour.R
import com.appsventory.digiapps.scour.data.hostOf
import com.appsventory.digiapps.scour.player.DetectedStream
import com.appsventory.digiapps.scour.player.StreamType

private const val SEPARATOR = " · "

/** "1:42:10 · 1080p · HLS stream · cdn.example.com" — what the user is about to play. */
fun streamSummary(res: Resources, s: DetectedStream): String {
    val parts = ArrayList<String>()
    val info = s.info
    if (info != null) {
        when {
            info.live -> parts += res.getString(R.string.player_live)
            info.durationMs > 0L -> parts += formatTime(info.durationMs)
        }
        if (info.height > 0) parts += res.getString(R.string.player_quality_height, info.height)
    }
    parts += res.getString(
        when (s.type) {
            StreamType.HLS -> R.string.stream_type_hls
            StreamType.DASH -> R.string.stream_type_dash
            StreamType.PROGRESSIVE -> R.string.stream_type_file
        }
    )
    parts += hostOf(s.url)
    return parts.joinToString(SEPARATOR)
}
