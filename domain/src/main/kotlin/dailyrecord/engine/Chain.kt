package dailyrecord.engine

import java.time.LocalDate

/**
 * 当前链状态：已完成的记录页 + 链起点。不可变。
 *
 * 不变量：`completed` 包含所有已完成过的记录页（含重置前的只读历史）；
 * `isBroken` / `streak` 只关注 `>= epoch` 的天，重置前的历史不影响当前链。
 */
data class Chain(
    /** 所有已完成过的记录页日期（含重置前的只读历史）。 */
    val completed: Set<LocalDate> = emptySet(),
    /** 当前链起点：连续天数从这一天起计。 */
    val epoch: LocalDate,
) {
    /** 从 epoch 到昨天之间是否存在未完成的天（断链）。 */
    fun isBroken(today: LocalDate): Boolean {
        var d = epoch
        while (d.isBefore(today)) {
            if (d !in completed) return true
            d = d.plusDays(1)
        }
        return false
    }

    fun isUnlocked(today: LocalDate): Boolean = !isBroken(today)

    /** 连续天数。链未断时是实时 streak；断链时返回断链前达到的值。 */
    fun streak(today: LocalDate): Int {
        var n = 0
        var d = epoch
        while (d.isBefore(today)) {
            if (d !in completed) break
            n++
            d = d.plusDays(1)
        }
        if (!isBroken(today) && today in completed) n++
        return n
    }

    fun complete(target: LocalDate, today: LocalDate): CompleteResult = when {
        target.isBefore(today) -> CompleteResult.Rejected(RejectReason.FROZEN)
        target.isAfter(today) -> CompleteResult.Rejected(RejectReason.FUTURE)
        isBroken(today) -> CompleteResult.Rejected(RejectReason.LOCKED)
        else -> CompleteResult.Ok(copy(completed = completed + target))
    }

    fun undo(target: LocalDate, today: LocalDate): CompleteResult = when {
        target != today -> CompleteResult.Rejected(if (target.isBefore(today)) RejectReason.FROZEN else RejectReason.FUTURE)
        target !in completed -> CompleteResult.Rejected(RejectReason.NOT_COMPLETED)
        else -> CompleteResult.Ok(copy(completed = completed - target))
    }

    fun reset(today: LocalDate): Chain = Chain(completed, today)
}

sealed interface CompleteResult {
    data class Ok(val chain: Chain) : CompleteResult
    data class Rejected(val reason: RejectReason) : CompleteResult
}

enum class RejectReason { FROZEN, FUTURE, LOCKED, NOT_COMPLETED }
