package com.appsventory.digiapps.scour.data

/**
 * Lightweight host-based ad/tracker blocklist for WebView.
 * Not a full filter-list engine — blocks common ad/analytics hosts only.
 */
object AdBlock {
    private val hosts = setOf(
        // Google ads / analytics
        "pagead2.googlesyndication.com",
        "googleads.g.doubleclick.net",
        "googlesyndication.com",
        "doubleclick.net",
        "adservice.google.com",
        "adservice.google.co.id",
        "www.googleadservices.com",
        "google-analytics.com",
        "www.google-analytics.com",
        "ssl.google-analytics.com",
        "googletagmanager.com",
        "www.googletagmanager.com",
        "googletagservices.com",
        "www.googletagservices.com",
        // Facebook
        "connect.facebook.net",
        "facebook.com/tr",
        "www.facebook.com/tr",
        "pixel.facebook.com",
        // Other trackers / ads
        "scorecardresearch.com",
        "sb.scorecardresearch.com",
        "ads.yahoo.com",
        "analytics.yahoo.com",
        "ads.twitter.com",
        "static.ads-twitter.com",
        "ads.linkedin.com",
        "snap.licdn.com",
        "hotjar.com",
        "static.hotjar.com",
        "script.hotjar.com",
        "cdn.taboola.com",
        "trc.taboola.com",
        "ads.pubmatic.com",
        "image2.pubmatic.com",
        "partner.googleadservices.com",
        "cm.g.doubleclick.net",
        "ad.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "tpc.googlesyndication.com",
        "fundingchoicesmessages.google.com",
        "pagead.l.doubleclick.net",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "c.amazon-adsystem.com",
        "outbrain.com",
        "widgets.outbrain.com",
        "ads.adthrive.com",
        "cdn.adsafeprotected.com",
        "pixel.quantserve.com",
        "secure.quantserve.com",
        "rules.quantcount.com",
        "cdn.krcount.com",
        "bat.bing.com",
        "ads.msn.com",
        "adnxs.com",
        "ib.adnxs.com",
        "criteo.com",
        "static.criteo.net",
        "bidder.criteo.com",
        "moatads.com",
        "z.moatads.com",
        "openx.net",
        "rtb.openx.net",
        "rubiconproject.com",
        "fastlane.rubiconproject.com",
        "casalemedia.com",
        "advertising.com",
        "adtechus.com",
        "yieldmo.com",
        "ads.yieldmo.com",
        "media.net",
        "contextual.media.net",
        "3lift.com",
        "ib.3lift.com",
        "sharethrough.com",
        "bttrack.com",
        "cdn.branch.io",
        "app.link",
        "adjust.com",
        "app.adjust.com",
        "appsflyer.com",
        "impressions.onetrust.com",
        "cdn.cookielaw.org"
    )

    fun shouldBlock(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val host = runCatching {
            java.net.URI(url).host?.lowercase()?.removePrefix("www.")
        }.getOrNull() ?: return false
        if (hosts.contains(host)) return true
        // suffix match: foo.doubleclick.net
        return hosts.any { host == it || host.endsWith(".$it") }
    }
}
