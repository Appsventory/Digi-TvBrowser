package com.appsventory.digiapps.scour.data

import android.content.Context

/** Real versionName from the manifest (no hardcoded "1.0"). */
fun appVersionName(context: Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()
