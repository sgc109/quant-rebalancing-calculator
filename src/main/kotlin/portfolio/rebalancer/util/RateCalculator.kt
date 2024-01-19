package portfolio.rebalancer.util

import java.math.RoundingMode

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
}
