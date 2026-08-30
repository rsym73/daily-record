package dailyrecord.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 凌晨 1 点前属于前一天：now 时刻对应的「记录页日期」。 */
fun currentPageDate(now: Instant, zone: ZoneId): LocalDate =
    now.atZone(zone).minusHours(1).toLocalDate()

/** day 的记录页在 day+1 的凌晨 1 点冻结。 */
fun isFrozen(day: LocalDate, now: Instant, zone: ZoneId): Boolean =
    !now.isBefore(day.plusDays(1).atStartOfDay(zone).plusHours(1).toInstant())
