package portfolio.rebalancer.util

import java.math.RoundingMode
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.pow

object RateCalculator {
    /**
     * 수익률 percentage(0% ~ 100%)
     */
    fun calculateEarnRate(curPrice: Double, pastPrice: Double): Double {
        return curPrice.toBigDecimal()
            .divide(pastPrice.toBigDecimal(), 8, RoundingMode.HALF_UP)
            .minus(1.0.toBigDecimal())
            .multiply(100.0.toBigDecimal())
            .toDouble()
    }

    fun calculateCAGR(totalValuesWithTime: List<Pair<ZonedDateTime, Double>>): Double {
        val startTimeAndValue = totalValuesWithTime.first()
        val finalTimeAndValue = totalValuesWithTime.last()
        val years = Duration.between(startTimeAndValue.first, finalTimeAndValue.first).toDays() / 365.0
        return ((finalTimeAndValue.second / startTimeAndValue.second).pow((1.0 / years)) - 1) * 100
    }
}
