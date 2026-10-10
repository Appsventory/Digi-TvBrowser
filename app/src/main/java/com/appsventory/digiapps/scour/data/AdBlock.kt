package com.appsventory.digiapps.scour.data

/**
 * Lightweight ad/tracker blocker for WebView (not a full filter-list engine).
 *
 *  - Standard: host blocklist.
 *  - Strong:   + pop-under / push / native-ad networks, third-party ad URL patterns (VAST, IMA,
 *              prebid …), cosmetic hiding of ad containers, and `window.open` without a user gesture.
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

    /** Pop-under / push / native-ad networks. Only used in strong mode. */
    private val strongHosts = setOf(
        "popads.net", "popcash.net", "propellerads.com", "exoclick.com", "exosrv.com",
        "juicyads.com", "trafficjunky.com", "adsterra.com", "hilltopads.com", "clickadu.com",
        "mgid.com", "revcontent.com", "zedo.com", "adform.net", "smartadserver.com",
        "bidswitch.net", "adcolony.com", "adsrvr.org", "tapad.com", "onetag-sys.com",
        "a-ads.com", "ad-maven.com", "adskeeper.com", "monetag.com", "popmyads.com",
        "richads.com", "adsco.re", "pubmatic.com", "taboola.com", "an.yandex.ru"
    )

    /** Third-party URL fragments that mean "ad request" (strong mode). */
    private val adUrlPatterns = listOf(
        "/ads/", "/adserver", "/adframe", "/advert", "/banner_ads", "pagead/", "/vast/",
        "/vast.xml", "/vmap", "ima3.js", "/imasdk", "prebid", "/popunder", "/popads",
        "adsbygoogle", "ad_type=", "adunit", "/preroll", "/ad-break"
    )

    /** Stream URLs that are ads, not the video the user came for. */
    private val adStreamPatterns = adUrlPatterns + listOf("/ad/", "advert", "preroll", "midroll", "/vast")

    /** Hiding ad containers + swallowing script-opened pop-ups. Run on page finished (strong mode). */
    const val COSMETIC_JS =
        "(function(){if(window.__digiCos)return;window.__digiCos=true;" +
            "var s=document.createElement('style');" +
            "s.textContent='ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads_\"],[id^=\"div-gpt-ad\"]," +
            ".adsbox,.ad-banner,.ad-container,.ad-slot,.advertisement,[data-ad-slot]," +
            "[data-google-query-id],iframe[src*=\"doubleclick.net\"],iframe[src*=\"googlesyndication\"]," +
            "iframe[src*=\"adsterra\"]{display:none!important}';" +
            "(document.head||document.documentElement).appendChild(s);" +
            "var wo=window.open;window.open=function(){" +
            "if(navigator.userActivation&&navigator.userActivation.isActive)" +
            "return wo.apply(window,arguments);return null};})();"

    private fun hostOf(url: String): String? = runCatching {
        java.net.URI(url).host?.lowercase()?.removePrefix("www.")
    }.getOrNull()

    /** foo.bar.doubleclick.net → checks foo.bar.doubleclick.net, bar.doubleclick.net, doubleclick.net. */
    private fun hostMatches(host: String, set: Set<String>): Boolean {
        var h = host
        while (true) {
            if (h in set) return true
            val dot = h.indexOf('.')
            if (dot < 0) return false
            h = h.substring(dot + 1)
        }
    }

    private val secondLevel = setOf("co", "com", "net", "org", "ac", "go", "or", "web", "my", "gov", "edu")

    /** Rough registrable domain (example.co.id / example.com). */
    private fun baseDomain(host: String): String {
        val parts = host.split('.')
        if (parts.size <= 2) return host
        val take = if (parts.last().length == 2 && parts[parts.size - 2] in secondLevel) 3 else 2
        return parts.takeLast(take).joinToString(".")
    }

    /**
     * @param pageUrl current page, needed so strong mode only judges *third-party* requests.
     */
    fun shouldBlock(url: String?, pageUrl: String? = null, strong: Boolean = false): Boolean {
        if (url.isNullOrBlank()) return false
        val host = hostOf(url) ?: return false
        if (hostMatches(host, hosts)) return true
        if (host.endsWith("facebook.com") && url.substringAfter(host, "").startsWith("/tr")) return true
        if (!strong) return false
        if (hostMatches(host, strongHosts)) return true

        val pageHost = pageUrl?.let { hostOf(it) } ?: return false
        if (baseDomain(host) == baseDomain(pageHost)) return false
        val lower = url.lowercase()
        return adUrlPatterns.any { lower.contains(it) }
    }

    /** True when a detected media URL is an ad (pre-roll / VAST …) rather than the main video. */
    fun looksLikeAdStream(url: String): Boolean {
        val lower = url.lowercase()
        val host = hostOf(url)
        return (host != null && (hostMatches(host, hosts) || hostMatches(host, strongHosts))) ||
            adStreamPatterns.any { lower.contains(it) }
    }
}
