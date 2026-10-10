package com.appsventory.digiapps.scour.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import kotlin.math.roundToInt

/** Picture-in-picture glue. Android TV supports it too (API 26+). */
object PlayerPip {

    /** Set by the player screen while it is open; MainActivity.onUserLeaveHint calls it. */
    @Volatile
    var onUserLeaveHint: (() -> Unit)? = null

    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun enter(activity: Activity, aspect: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ratio = aspect.coerceIn(PlayerConfig.PIP_MIN_RATIO, PlayerConfig.PIP_MAX_RATIO)
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(
                Rational((ratio * PlayerConfig.PIP_RATIO_SCALE).roundToInt(), PlayerConfig.PIP_RATIO_SCALE)
            )
            .build()
        runCatching { activity.enterPictureInPictureMode(params) }
    }

    fun isInPip(activity: Activity?): Boolean =
        activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            activity.isInPictureInPictureMode
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
