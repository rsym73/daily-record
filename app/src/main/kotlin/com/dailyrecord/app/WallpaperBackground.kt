package com.dailyrecord.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun WallpaperBackground(file: File?, generation: Long, content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    val config = LocalConfiguration.current
    val reqW = (config.screenWidthDp * density).toInt().coerceAtLeast(1)
    val reqH = (config.screenHeightDp * density).toInt().coerceAtLeast(1)

    Box(Modifier.fillMaxSize()) {
        val bitmap by produceState<ImageBitmap?>(initialValue = null, file, generation) {
            value = if (file != null && file.exists()) {
                withContext(Dispatchers.IO) { decodeSampled(file, reqW, reqH) }
            } else {
                null
            }
        }
        if (bitmap != null) {
            Image(
                bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.3f)))
        }
        content()
    }
}

private fun decodeSampled(file: File, reqW: Int, reqH: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
}
