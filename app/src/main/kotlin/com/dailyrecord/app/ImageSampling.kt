package com.dailyrecord.app

/**
 * 计算 BitmapFactory 的 inSampleSize（2 的幂），使解码后的位图尺寸接近但不小于请求尺寸，
 * 避免对高分辨率相册图做全尺寸解码造成卡顿 / OOM。
 */
fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}
