package com.appsventory.digiapps.scour.ui.browser

import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.viewinterop.AndroidView
import com.appsventory.digiapps.scour.player.findActivity
import com.appsventory.digiapps.scour.ui.player.ImmersiveEffect
import com.appsventory.digiapps.scour.ui.theme.DigiColors

/** Page-requested fullscreen (WebChromeClient.onShowCustomView): DRM / in-page players keep working. */
@Composable
fun FullscreenWebVideo(
    view: View,
    callback: WebChromeClient.CustomViewCallback?,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isTv = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }

    ImmersiveEffect(activity, lockLandscape = !isTv)

    BackHandler {
        callback?.onCustomViewHidden()
        onExit()
    }

    AndroidView(
        factory = {
            (view.parent as? ViewGroup)?.removeView(view)
            view.requestFocus()
            view
        },
        modifier = Modifier.fillMaxSize().background(DigiColors.PlayerBg)
    )
}
