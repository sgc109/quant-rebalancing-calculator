package portfolio.rebalancer.util

object RateCalculator {
    fun calculateEarnRate(curPrice: Double, pastPrice: Double): Double {
        return curPrice.toBigDecimal()
            .divide(pastPrice.toBigDecimal())
            .minus(1.0.toBigDecimal())
            .toDouble()
    }
}
