package com.dailyrecord.app.data

import dailyrecord.engine.Chain
import dailyrecord.engine.currentPageDate
import dailyrecord.engine.isFrozen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RecordRepository(
    private val dayDao: DayDao,
    private val entryDao: EntryDao,
    private val keyValueDao: KeyValueDao,
    private val zone: ZoneId,
) {
    // ---- 条目 ----

    suspend fun addEntry(now: Instant, text: String): AddEntryResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return AddEntryResult.Blank
        if (isTodayCompleted(now)) return AddEntryResult.Completed
        val today = currentPageDate(now, zone)
        entryDao.insert(EntryEntity(dayDate = today, text = trimmed))
        return AddEntryResult.Ok
    }

    suspend fun editEntry(now: Instant, id: Long, newText: String): EditResult {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return EditResult.Blank
        if (isTodayCompleted(now)) return EditResult.Completed
        val entry = entryDao.getById(id) ?: return EditResult.NotFound
        if (entry.dayDate != currentPageDate(now, zone)) {
            return if (isFrozen(entry.dayDate, now, zone)) EditResult.Frozen else EditResult.Future
        }
        entryDao.update(entry.copy(text = trimmed))
        return EditResult.Ok
    }

    suspend fun deleteEntry(now: Instant, id: Long): DeleteResult {
        if (isTodayCompleted(now)) return DeleteResult.Completed
        val entry = entryDao.getById(id) ?: return DeleteResult.NotFound
        if (entry.dayDate != currentPageDate(now, zone)) {
            return if (isFrozen(entry.dayDate, now, zone)) DeleteResult.Frozen else DeleteResult.Future
        }
        entryDao.delete(entry)
        return DeleteResult.Ok
    }

    // ---- 完成与连续天数 ----

    suspend fun completeToday(now: Instant): CompleteTodayResult {
        val today = currentPageDate(now, zone)
        val chain = currentChain(today)
        if (!chain.isUnlocked(today)) return CompleteTodayResult.Locked
        val outcome = chain.complete(today, today)
        if (outcome is dailyrecord.engine.CompleteResult.Ok) {
            dayDao.upsert(DayEntity(date = today, completedAt = now))
            return CompleteTodayResult.Ok(outcome.chain.streak(today))
        }
        return CompleteTodayResult.Locked
    }

    suspend fun undoToday(now: Instant): UndoTodayResult {
        val today = currentPageDate(now, zone)
        val day = dayDao.getDay(today) ?: return UndoTodayResult.NotCompleted
        val chain = currentChain(today)
        val outcome = chain.undo(today, today)
        if (outcome is dailyrecord.engine.CompleteResult.Ok) {
            dayDao.delete(day)
            return UndoTodayResult.Ok(outcome.chain.streak(today))
        }
        return UndoTodayResult.NotCompleted
    }

    suspend fun isTodayCompleted(now: Instant): Boolean {
        val today = currentPageDate(now, zone)
        return dayDao.getDay(today) != null
    }

    suspend fun streak(now: Instant): Int {
        val today = currentPageDate(now, zone)
        return currentChain(today).streak(today)
    }

    suspend fun isBroken(now: Instant): Boolean {
        val today = currentPageDate(now, zone)
        return currentChain(today).isBroken(today)
    }

    suspend fun loadToday(now: Instant): TodayState {
        val today = currentPageDate(now, zone)
        return TodayState(
            today = today,
            streak = streak(now),
            isCompleted = isTodayCompleted(now),
            isBroken = isBroken(now),
            entries = entryDao.getEntriesForDay(today),
        )
    }

    suspend fun reset(now: Instant): Int {
        val today = currentPageDate(now, zone)
        keyValueDao.put(KeyValueEntity(EPOCH_KEY, today.toString()))
        return currentChain(today).streak(today)
    }

    suspend fun getEntriesForDay(day: LocalDate): List<EntryEntity> = entryDao.getEntriesForDay(day)

    suspend fun getHistory(now: Instant, from: LocalDate, to: LocalDate): List<HistoryDay> {
        val today = currentPageDate(now, zone)
        val completed = dayDao.getCompletedDates().toSet()
        val epoch = getEpoch(today)
        val result = mutableListOf<HistoryDay>()
        var d = from
        while (!d.isAfter(to)) {
            val isCompleted = d in completed
            val missed = !d.isBefore(epoch) && d.isBefore(today) && !isCompleted
            result.add(HistoryDay(date = d, completed = isCompleted, missed = missed))
            d = d.plusDays(1)
        }
        return result
    }

    suspend fun loadHistory(now: Instant, daysBack: Long = 60): List<HistoryDay> {
        val today = currentPageDate(now, zone)
        return getHistory(now, today.minusDays(daysBack), today)
    }

    suspend fun getReminderHour(): Int =
        keyValueDao.get(REMINDER_HOUR_KEY)?.toIntOrNull() ?: 0

    suspend fun getReminderMinute(): Int =
        keyValueDao.get(REMINDER_MINUTE_KEY)?.toIntOrNull() ?: 30

    suspend fun setReminderTime(hour: Int, minute: Int) {
        keyValueDao.put(KeyValueEntity(REMINDER_HOUR_KEY, hour.toString()))
        keyValueDao.put(KeyValueEntity(REMINDER_MINUTE_KEY, minute.toString()))
    }

    private suspend fun currentChain(today: LocalDate): Chain {
        val completed = dayDao.getCompletedDates().toSet()
        return Chain(completed = completed, epoch = getEpoch(today))
    }

    private suspend fun getEpoch(today: LocalDate): LocalDate {
        return keyValueDao.get(EPOCH_KEY)?.let { LocalDate.parse(it) } ?: run {
            keyValueDao.put(KeyValueEntity(EPOCH_KEY, today.toString()))
            today
        }
    }

    private companion object {
        const val EPOCH_KEY = "epoch"
        const val REMINDER_HOUR_KEY = "reminder_hour"
        const val REMINDER_MINUTE_KEY = "reminder_minute"
    }
}

sealed interface AddEntryResult {
    data object Ok : AddEntryResult
    data object Blank : AddEntryResult
    data object Completed : AddEntryResult
}

sealed interface EditResult {
    data object Ok : EditResult
    data object Blank : EditResult
    data object NotFound : EditResult
    data object Frozen : EditResult
    data object Future : EditResult
    data object Completed : EditResult
}

sealed interface DeleteResult {
    data object Ok : DeleteResult
    data object NotFound : DeleteResult
    data object Frozen : DeleteResult
    data object Future : DeleteResult
    data object Completed : DeleteResult
}

sealed interface CompleteTodayResult {
    data class Ok(val streak: Int) : CompleteTodayResult
    data object Locked : CompleteTodayResult
}

sealed interface UndoTodayResult {
    data class Ok(val streak: Int) : UndoTodayResult
    data object NotCompleted : UndoTodayResult
}

data class TodayState(
    val today: java.time.LocalDate,
    val streak: Int,
    val isCompleted: Boolean,
    val isBroken: Boolean,
    val entries: List<EntryEntity>,
)

data class HistoryDay(
    val date: java.time.LocalDate,
    val completed: Boolean,
    val missed: Boolean,
)
