package com.dailyrecord.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSamplingTest {

    @Test
    fun `大图按 2 的幂降采样`() {
        assertEquals(2, calculateInSampleSize(4000, 3000, 1000, 1000))
    }

    @Test
    fun `超大图降采样更多`() {
        assertEquals(4, calculateInSampleSize(16000, 12000, 2000, 2000))
    }

    @Test
    fun `小于请求尺寸不采样`() {
        assertEquals(1, calculateInSampleSize(100, 100, 200, 200))
    }

    @Test
    fun `刚好两倍采样为 2`() {
        assertEquals(2, calculateInSampleSize(2048, 2048, 1024, 1024))
    }
}
