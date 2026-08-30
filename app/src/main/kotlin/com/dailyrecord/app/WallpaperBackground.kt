package com.dailyrecord.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

@Composable
fun WallpaperBackground(file: File?, generation: Long, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (file != null && file.exists()) {
            val bitmap = remember(file, generation) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
            if (bitmap != null) {
                Image(
                    bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.3f)))
            }
        }
        content()
    }
}
