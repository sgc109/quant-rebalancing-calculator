package portfolio.rebalancer

class DrawdownCalculator {
    fun calculateDrawdowns(
        prices: List<Double>,
    ): List<Double> {
        val drawdowns = mutableListOf<Double>()
        var lastMaxPrice = prices[0]
        for (i in 1..<prices.size) {
            val earnRate = calculateEarnRate(prices[i], lastMaxPrice)
            drawdowns.add(minOf(0.0, earnRate))
            lastMaxPrice = maxOf(lastMaxPrice, prices[i])
        }
        return drawdowns
    }

    private fun calculateEarnRate(curPrice: Double, pastPrice: Double): Double {
        return curPrice / pastPrice - 1.0
    }
}
