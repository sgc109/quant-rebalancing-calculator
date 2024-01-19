package portfolio.rebalancer.util

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/**
 * 주어진 날짜 목록 dates 중에서 baseTime 와 가장 가까운 날짜를 찾는다.
 * 단, 가장 가까운 날짜가 baseTime 과 4일 초과하여 차이가 난다면 예외를 던진다.
 * 만약 가장 가까운 날짜가 여러개라면 더 이른 날짜를 반환한다.
 */
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
        if (minDist > 4) {
            throw NoSuchElementException("No closest date found")
        }
        if (distLeft <= distRight) {
            return sortedDates[idx - 1]
        }
        return sortedDates[idx]
    }
}
