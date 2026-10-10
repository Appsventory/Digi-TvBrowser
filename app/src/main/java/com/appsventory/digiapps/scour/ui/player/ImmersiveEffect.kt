package com.appsventory.digiapps.scour.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Full-screen video chrome: hides system bars, keeps the screen on and (phones only)
 * locks landscape. Everything is restored when the composable leaves.
 */
@Composable
fun ImmersiveEffect(activity: Activity?, lockLandscape: Boolean) {
    val view = LocalView.current

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    DisposableEffect(activity, view) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    DisposableEffect(activity, lockLandscape) {
        if (lockLandscape && activity != null) {
            val previous = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose { activity.requestedOrientation = previous }
        } else onDispose { }
    }
}
