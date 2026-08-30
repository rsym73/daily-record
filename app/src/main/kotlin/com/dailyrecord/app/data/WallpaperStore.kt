package com.dailyrecord.app.data

import java.io.File
import java.io.InputStream

class WallpaperStore(private val filesDir: File) {

    private val target: File
        get() = File(filesDir, "wallpaper")

    // 把输入流复制到 File(filesDir, "wallpaper.jpg")，覆盖旧文件；关闭输入流
    fun save(source: InputStream) {
        source.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    // 文件存在且非空为 true
    fun hasWallpaper(): Boolean {
        val file = wallpaperFile() ?: return false
        return file.length() > 0
    }

    // 文件存在返回 File，否则 null
    fun wallpaperFile(): File? {
        return if (target.exists()) target else null
    }

    // 删除壁纸文件；不存在时静默不抛异常（幂等）
    fun remove() {
        target.delete()
    }
}
