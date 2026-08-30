package dailyrecord.engine

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChainTest {

    private val jan1 = LocalDate.of(2024, 1, 1)
    private val jan2 = jan1.plusDays(1)
    private val jan3 = jan1.plusDays(2)
    private val jan4 = jan1.plusDays(3)

    private fun ok(result: CompleteResult): Chain =
        (result as CompleteResult.Ok).chain

    @Test
    fun `新链 streak 为 0`() {
        val chain = Chain(epoch = jan1)
        assertEquals(0, chain.streak(jan1))
    }

    @Test
    fun `完成今天后 streak 为 1`() {
        val chain = Chain(epoch = jan1)
        val result = chain.complete(jan1, jan1)
        assertIs<CompleteResult.Ok>(result)
        assertEquals(1, result.chain.streak(jan1))
    }

    @Test
    fun `连续完成多天 streak 累加`() {
        var chain = Chain(epoch = jan1)
        chain = ok(chain.complete(jan1, jan1))
        chain = ok(chain.complete(jan2, jan2))
        assertEquals(2, chain.streak(jan2))
    }

    @Test
    fun `漏一天导致断链并锁死`() {
        var chain = Chain(epoch = jan1)
        chain = ok(chain.complete(jan1, jan1))
        // 跳过 jan2，直接到 jan3
        assertTrue(chain.isBroken(jan3))
        assertFalse(chain.isUnlocked(jan3))
        val result = chain.complete(jan3, jan3)
        assertIs<CompleteResult.Rejected>(result)
        assertEquals(RejectReason.LOCKED, result.reason)
    }

    @Test
    fun `断链时 streak 保留断链前值`() {
        var chain = Chain(epoch = jan1)
        chain = ok(chain.complete(jan1, jan1))
        chain = ok(chain.complete(jan2, jan2))
        // 跳过 jan3
        assertTrue(chain.isBroken(jan4))
        assertEquals(2, chain.streak(jan4))
    }

    @Test
    fun `补写昨天被拒绝`() {
        val chain = Chain(epoch = jan1)
        val result = chain.complete(jan1, jan2)
        assertIs<CompleteResult.Rejected>(result)
        assertEquals(RejectReason.FROZEN, result.reason)
    }

    @Test
    fun `提前写明天被拒绝`() {
        val chain = Chain(epoch = jan1)
        val result = chain.complete(jan2, jan1)
        assertIs<CompleteResult.Rejected>(result)
        assertEquals(RejectReason.FUTURE, result.reason)
    }

    @Test
    fun `撤销今天的完成`() {
        var chain = Chain(epoch = jan1)
        chain = ok(chain.complete(jan1, jan1))
        val result = chain.undo(jan1, jan1)
        assertIs<CompleteResult.Ok>(result)
        assertEquals(0, result.chain.streak(jan1))
    }

    @Test
    fun `撤销未完成的天被拒绝`() {
        val chain = Chain(epoch = jan1)
        val result = chain.undo(jan1, jan1)
        assertIs<CompleteResult.Rejected>(result)
        assertEquals(RejectReason.NOT_COMPLETED, result.reason)
    }

    @Test
    fun `重置归零并保留历史`() {
        var chain = Chain(epoch = jan1)
        chain = ok(chain.complete(jan1, jan1))
        val reset = chain.reset(jan3)
        assertEquals(jan3, reset.epoch)
        assertTrue(jan1 in reset.completed)
        assertEquals(0, reset.streak(jan3))
        assertTrue(reset.isUnlocked(jan3))
    }
}
