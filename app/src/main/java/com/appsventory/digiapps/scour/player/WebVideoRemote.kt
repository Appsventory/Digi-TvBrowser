package com.appsventory.digiapps.scour.player

import android.view.KeyEvent
import android.webkit.WebView

/**
 * Remote-control keys for a page's own player (fullscreen): drives the page's <video> element
 * by script, and falls back to the equivalent keyboard key when no <video> is reachable
 * (cross-origin iframes, custom canvases).
 */
object WebVideoRemote {

    enum class Command(val fallbackKey: Int) {
        SeekBack(KeyEvent.KEYCODE_DPAD_LEFT),
        SeekForward(KeyEvent.KEYCODE_DPAD_RIGHT),
        TogglePlay(KeyEvent.KEYCODE_SPACE),
        SpeedUp(KeyEvent.KEYCODE_PLUS),
        SpeedDown(KeyEvent.KEYCODE_MINUS)
    }

    /** Result of a command, ready to show: raw tokens "seek:+10", "play", "pause", "speed:1.25". */
    private fun script(command: Command, seekSeconds: Int): String {
        val (name, arg) = when (command) {
            Command.SeekBack -> "seek" to -seekSeconds.toDouble()
            Command.SeekForward -> "seek" to seekSeconds.toDouble()
            Command.TogglePlay -> "toggle" to 0.0
            Command.SpeedUp -> "speed" to PlayerConfig.WEB_SPEED_STEP.toDouble()
            Command.SpeedDown -> "speed" to -PlayerConfig.WEB_SPEED_STEP.toDouble()
        }
        return "(function(c,n){" +
            "function vids(d,o){try{var a=d.querySelectorAll('video');" +
            "for(var i=0;i<a.length;i++)o.push(a[i]);" +
            "var f=d.querySelectorAll('iframe');" +
            "for(var j=0;j<f.length;j++){try{if(f[j].contentDocument)vids(f[j].contentDocument,o)}catch(e){}}}" +
            "catch(e){}return o}" +
            "var vs=vids(document,[]);if(!vs.length)return '';" +
            "var v=vs.filter(function(x){return !x.paused})[0]||" +
            "vs.sort(function(a,b){return b.videoWidth*b.videoHeight-a.videoWidth*a.videoHeight})[0];" +
            "if(c==='seek'){var d=v.duration>0&&isFinite(v.duration)?v.duration:1e9;" +
            "v.currentTime=Math.max(0,Math.min(d,v.currentTime+n));return 'seek:'+(n>0?'+':'')+n}" +
            "if(c==='toggle'){if(v.paused){v.play();return 'play'}v.pause();return 'pause'}" +
            "if(c==='speed'){var r=Math.max(${PlayerConfig.WEB_SPEED_MIN},Math.min(${PlayerConfig.WEB_SPEED_MAX}," +
            "(v.playbackRate||1)+n));v.playbackRate=r;return 'speed:'+r}" +
            "return ''})('$name',$arg)"
    }

    /** [onResult] gets the raw token, or "" when the command had to go out as a key press. */
    fun run(webView: WebView?, command: Command, seekSeconds: Int, onResult: (String) -> Unit) {
        if (webView == null) return
        webView.evaluateJavascript(script(command, seekSeconds)) { raw ->
            val token = raw?.trim('"').orEmpty().takeIf { it != "null" }.orEmpty()
            if (token.isEmpty()) sendKey(webView, command.fallbackKey)
            onResult(token)
        }
    }

    private fun sendKey(webView: WebView, keyCode: Int) {
        webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
