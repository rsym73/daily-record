package dailyrecord.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DayBoundaryTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant()

    @Test
    fun `凌晨 1 点之前属于前一天`() {
        // 2024-01-02 00:30（本地）—— 还没跨过 1 点，记录页仍是 01-01
        assertEquals(LocalDate.of(2024, 1, 1), currentPageDate(at(2024, 1, 2, 0, 30), zone))
    }

    @Test
    fun `凌晨 1 点整换天`() {
        assertEquals(LocalDate.of(2024, 1, 2), currentPageDate(at(2024, 1, 2, 1, 0), zone))
    }

    @Test
    fun `前一天在凌晨 1 点前未冻结、之后冻结`() {
        val day = LocalDate.of(2024, 1, 1)
        assertFalse(isFrozen(day, at(2024, 1, 2, 0, 30), zone))
        assertTrue(isFrozen(day, at(2024, 1, 2, 1, 0), zone))
    }

    @Test
    fun `跨时区边界一致（UTC）`() {
        val utc = ZoneId.of("UTC")
        fun atUtc(y: Int, m: Int, d: Int, h: Int, min: Int) =
            ZonedDateTime.of(y, m, d, h, min, 0, 0, utc).toInstant()

        assertEquals(LocalDate.of(2024, 1, 1), currentPageDate(atUtc(2024, 1, 2, 0, 30), utc))
        assertEquals(LocalDate.of(2024, 1, 2), currentPageDate(atUtc(2024, 1, 2, 1, 0), utc))
    }
}
