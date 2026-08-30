package com.dailyrecord.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

class BackupService(
    private val dayDao: DayDao,
    private val entryDao: EntryDao,
    private val keyValueDao: KeyValueDao,
) {
    suspend fun exportData(): String {
        val root = JSONObject()
        keyValueDao.get("epoch")?.let { root.put("epoch", it) }

        val entriesByDay = entryDao.getAllEntries().groupBy { it.dayDate }
        val daysArr = JSONArray()
        for (day in dayDao.getAllDays()) {
            val dayObj = JSONObject()
            dayObj.put("date", day.date.toString())
            dayObj.put("completedAt", day.completedAt.toEpochMilli())
            val entriesArr = JSONArray()
            for (e in entriesByDay[day.date].orEmpty()) {
                entriesArr.put(
                    JSONObject()
                        .put("text", e.text)
                        .put("createdAt", e.createdAt.toEpochMilli())
                )
            }
            dayObj.put("entries", entriesArr)
            daysArr.put(dayObj)
        }
        root.put("days", daysArr)
        return root.toString()
    }

    suspend fun importData(json: String) {
        val root = JSONObject(json)
        root.optString("epoch").takeIf { it.isNotEmpty() }?.let {
            keyValueDao.put(KeyValueEntity("epoch", it))
        }
        val daysArr = root.optJSONArray("days") ?: return
        for (i in 0 until daysArr.length()) {
            val dayObj = daysArr.getJSONObject(i)
            val date = LocalDate.parse(dayObj.getString("date"))
            val completedAt = Instant.ofEpochMilli(dayObj.getLong("completedAt"))
            dayDao.upsert(DayEntity(date, completedAt))
            val entriesArr = dayObj.optJSONArray("entries") ?: JSONArray()
            for (j in 0 until entriesArr.length()) {
                val eObj = entriesArr.getJSONObject(j)
                entryDao.insert(
                    EntryEntity(
                        dayDate = date,
                        text = eObj.getString("text"),
                        createdAt = Instant.ofEpochMilli(eObj.getLong("createdAt")),
                    )
                )
            }
        }
    }
}
