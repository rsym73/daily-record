package com.dailyrecord.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WallpaperStoreTest {

    private lateinit var filesDir: File
    private lateinit var store: WallpaperStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 每个测试用唯一子目录，避免相互污染
        filesDir = File(context.filesDir, "wallpaper-test-${System.nanoTime()}")
        filesDir.mkdirs()
        store = WallpaperStore(filesDir)
    }

    @After
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test
    fun `初始 hasWallpaper 为 false`() {
        assertFalse(store.hasWallpaper())
        assertNull(store.wallpaperFile())
    }

    @Test
    fun `save 后 hasWallpaper 为 true 且读回内容一致`() {
        val bytes = "hello wallpaper".toByteArray(Charsets.UTF_8)

        store.save(ByteArrayInputStream(bytes))

        assertTrue(store.hasWallpaper())
        val file = store.wallpaperFile()
        assertNotNull(file)
        assertEquals("wallpaper", file!!.name)
        assertArrayEquals(bytes, file.readBytes())
    }

    @Test
    fun `再次 save 覆盖旧内容且只有一份文件`() {
        store.save(ByteArrayInputStream("first".toByteArray(Charsets.UTF_8)))

        store.save(ByteArrayInputStream("second-longer-content".toByteArray(Charsets.UTF_8)))

        assertTrue(store.hasWallpaper())
        val file = store.wallpaperFile()
        assertNotNull(file)
        assertArrayEquals("second-longer-content".toByteArray(Charsets.UTF_8), file!!.readBytes())
        // filesDir 里名为 wallpaper 的文件只有一份
        assertEquals(1, filesDir.listFiles()!!.count { it.name == "wallpaper" })
    }

    @Test
    fun `remove 后 hasWallpaper 为 false`() {
        store.save(ByteArrayInputStream("content".toByteArray(Charsets.UTF_8)))

        store.remove()

        assertFalse(store.hasWallpaper())
        assertNull(store.wallpaperFile())
    }

    @Test
    fun `没有壁纸时 remove 不抛异常`() {
        store.remove()

        assertFalse(store.hasWallpaper())
        assertNull(store.wallpaperFile())
    }
}
