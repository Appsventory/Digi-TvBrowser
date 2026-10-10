package com.appsventory.digiapps.scour.player

/** Player constants — no magic numbers scattered in UI code. */
object PlayerConfig {
    // Seeking
    const val SEEK_STEP_MS = 10_000L
    const val SEEK_STEP_LONG_MS = 30_000L
    /** Key-repeat count after which D-pad seek switches to the long step. */
    const val SEEK_LONG_AFTER_REPEATS = 8

    // Overlay timing
    const val CONTROLS_HIDE_MS = 5_000L
    const val MESSAGE_MS = 2_500L
    const val SEEK_FEEDBACK_MS = 700L

    // Polling
    const val PROGRESS_POLL_MS = 500L
    const val SUBTITLE_POLL_MS = 100L

    // Side-loaded subtitles
    const val SUBTITLE_NUDGE_STEP_MS = 100L
    const val SUBTITLE_NUDGE_MAX_MS = 30_000L
    const val SUBTITLE_MAX_BYTES = 2 * 1024 * 1024
    const val NETWORK_TIMEOUT_MS = 10_000

    // Detection (per page)
    const val MAX_STREAM_CANDIDATES = 8
    const val MAX_SUBTITLE_CANDIDATES = 8

    // Resume
    const val RESUME_MIN_POSITION_MS = 10_000L
    const val RESUME_END_MARGIN_MS = 15_000L
    const val RESUME_SAVE_INTERVAL_MS = 5_000L

    // Picture-in-picture: Android rejects ratios outside ~2.39:1 … 1:2.39
    const val PIP_MIN_RATIO = 0.42f
    const val PIP_MAX_RATIO = 2.38f
    const val PIP_DEFAULT_RATIO = 16f / 9f
    const val PIP_RATIO_SCALE = 1000

    // Main-video vs ad heuristics
    /** Streams shorter than this are treated as ads/previews. */
    const val MIN_MAIN_DURATION_MS = 60_000L
    /** Manifest duration vs <video>.duration tolerance when matching the stream being watched. */
    const val DURATION_MATCH_TOLERANCE_MS = 2_500L
    const val PROBE_MAX_BYTES = 512 * 1024

    /**
     * Page hook: reports the playing <video> (duration, currentSrc, <source>, <track>) from the top
     * document and every same-origin iframe. Cross-origin iframes are not reachable from JS.
     */
    const val VIDEO_HOOK_JS =
        "(function(){" +
        "if(window.__digiHook)return;window.__digiHook=true;" +
        "var hooked=new WeakSet(),sent={};" +
        "function http(u){return typeof u==='string'&&/^https?:/i.test(u)}" +
        "function once(u){if(!http(u)||sent[u])return false;sent[u]=1;return true}" +
        "function send(v){try{" +
        "var d=v.duration;" +
        "if(!v.paused&&isFinite(d)&&d>0)DigiVideo.onPlaying(d,v.videoHeight||0);" +
        "var s=v.currentSrc||v.src;if(once(s))DigiVideo.onMedia(s);" +
        "var ss=v.querySelectorAll('source[src]');" +
        "for(var i=0;i<ss.length;i++){if(once(ss[i].src))DigiVideo.onMedia(ss[i].src)}" +
        "var ts=v.querySelectorAll('track[src]');" +
        "for(var j=0;j<ts.length;j++){var t=ts[j];if(once(t.src))DigiVideo.onTrack(t.src,t.srclang||'')}" +
        "}catch(e){}}" +
        "function hook(doc){" +
        "if(!doc||hooked.has(doc))return;hooked.add(doc);" +
        "['playing','loadedmetadata','durationchange'].forEach(function(n){" +
        "doc.addEventListener(n,function(e){var v=e.target;" +
        "if(v&&v.tagName==='VIDEO'&&(n!=='durationchange'||!v.paused))send(v)},true)});" +
        "var vs=doc.querySelectorAll('video');" +
        "for(var i=0;i<vs.length;i++){if(!vs[i].paused||vs[i].currentSrc)send(vs[i])}}" +
        "function scan(doc){hook(doc);" +
        "var fr=doc.querySelectorAll('iframe');" +
        "for(var i=0;i<fr.length;i++){try{var d=fr[i].contentDocument;if(d)scan(d)}catch(e){}}}" +
        "scan(document);" +
        "var n=0,t=setInterval(function(){scan(document);if(++n>=20)clearInterval(t)},1500);" +
        "})();"
    const val VIDEO_BRIDGE_NAME = "DigiVideo"

    val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    const val DEFAULT_SPEED = 1f

    /** Pauses <video>/<audio> on the page so it does not play under the native player. */
    const val PAUSE_WEB_MEDIA_JS =
        "document.querySelectorAll('video,audio').forEach(function(m){try{m.pause()}catch(e){}})"
}
