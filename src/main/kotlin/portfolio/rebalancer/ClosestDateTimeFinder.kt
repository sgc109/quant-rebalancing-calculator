package portfolio.rebalancer

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object ClosestDateTimeFinder {
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
        if (idx >= sortedDates.size) {
            throw NoSuchElementException("No closest date found")
        }
        if (ChronoUnit.DAYS.between(baseTime, sortedDates[idx]) > 4) {
            throw NoSuchElementException("No closest date found")
        }
        return sortedDates[idx]
    }
}
