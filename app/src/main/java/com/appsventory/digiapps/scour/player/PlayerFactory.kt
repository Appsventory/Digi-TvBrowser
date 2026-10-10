package com.appsventory.digiapps.scour.player

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.appsventory.digiapps.scour.data.UserAgents

object PlayerFactory {

    /** Stable resume key: host + path, so rotating tokens in the query do not break resume. */
    fun resumeKey(url: String): String {
        val uri = Uri.parse(url)
        return uri.host.orEmpty() + uri.path.orEmpty()
    }

    /**
     * ExoPlayer that replays the WebView's identity:
     *  - User-Agent, Referer, Origin from the original request
     *  - Cookie looked up per request URL in the WebView cookie jar, so manifests *and* segments
     *    on other hosts get their own cookies (never one host's cookies sent to another).
     */
    @OptIn(UnstableApi::class)
    fun create(context: Context, stream: DetectedStream, startPositionMs: Long): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(stream.userAgent ?: UserAgents.MOBILE)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(PlayerConfig.NETWORK_TIMEOUT_MS)
            .setReadTimeoutMs(PlayerConfig.NETWORK_TIMEOUT_MS)
            .setDefaultRequestProperties(stream.headers)

        val withCookies = ResolvingDataSource.Factory(
            http,
            ResolvingDataSource.Resolver { spec ->
                val cookie = runCatching {
                    CookieManager.getInstance().getCookie(spec.uri.toString())
                }.getOrNull()
                if (cookie.isNullOrBlank()) spec
                else spec.withAdditionalHeaders(mapOf("Cookie" to cookie))
            }
        )

        val mime = when (stream.type) {
            StreamType.HLS -> MimeTypes.APPLICATION_M3U8
            StreamType.DASH -> MimeTypes.APPLICATION_MPD
            StreamType.PROGRESSIVE -> null
        }
        val item = MediaItem.Builder()
            .setUri(stream.url)
            .apply { mime?.let { setMimeType(it) } }
            .setMediaMetadata(MediaMetadata.Builder().setTitle(stream.title).build())
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(withCookies))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                if (startPositionMs >= PlayerConfig.RESUME_MIN_POSITION_MS) {
                    setMediaItem(item, startPositionMs)
                } else {
                    setMediaItem(item)
                }
                prepare()
                playWhenReady = true
            }
    }
}
