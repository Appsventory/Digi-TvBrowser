package com.appsventory.digiapps.scour.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap

data class BrowserTab(val id: Long, val url: String, val title: String)

/** In-memory tab previews, shared by Home and Browser (not persisted). */
object TabThumbs {
    val map = mutableStateMapOf<Long, ImageBitmap>()
}
