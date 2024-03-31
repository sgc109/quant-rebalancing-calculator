package portfolio.rebalancer.util

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.absoluteValue

object ClosestDateTimeFinder {
    /**
     * 주어진 날짜 목록 dates 중에서 baseTime 와 가장 가까운 날짜를 찾는다(앞뒤 모두 포함).
     * 단, 가장 가까운 날짜가 baseTime 과 5일 초과하여 차이가 난다면 예외를 던진다.
     * (2012년 10월 29~30일에 허리케인으로 인한 휴장으로 최대 4일(토일월화)가 휴장일 때가 있었음)
     * 만약 가장 가까운 날짜가 여러개라면 더 이른 날짜를 반환한다.
     */
    fun findClosestDate(
        baseTime: ZonedDateTime,
        dates: List<ZonedDateTime>,
    ): ZonedDateTime {
        val sortedDates = dates.sorted()
        val res = sortedDates.binarySearch(baseTime)
        if (res >= 0) {
            return sortedDates[res]
        }
        val idx = -res - 1
        val distLeft = if (idx - 1 >= 0) {
            ChronoUnit.DAYS.between(baseTime, sortedDates[idx - 1]).absoluteValue
        } else {
            Long.MAX_VALUE
        }
        val distRight = if (idx < dates.size) {
            ChronoUnit.DAYS.between(baseTime, sortedDates[idx]).absoluteValue
        } else {
            Long.MAX_VALUE
        }
        val minDist = minOf(distLeft, distRight)
        if (minDist > 5) {
            throw NoSuchElementException("No closest date found")
        }
        if (distLeft <= distRight) {
            return sortedDates[idx - 1]
        }
        return sortedDates[idx]
    }

    fun findLatestDateInMonth(
        baseTime: ZonedDateTime,
        dates: List<ZonedDateTime>,
    ): ZonedDateTime {
        val lastDayOfMonth = baseTime.with(TemporalAdjusters.lastDayOfMonth())
        return findClosestDate(baseTime = lastDayOfMonth, dates = dates)
    }
}
