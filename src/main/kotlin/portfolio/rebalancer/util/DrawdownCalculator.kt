package portfolio.rebalancer.util

class DrawdownCalculator {
    /**
     * @param prices 시간 순서로 정렬된 가격 목록
     *
     * 하락율의 리스트를 계산하는 것이라서 주어진 리스트보다 length 가 1 작은 리스트를 반환한다.
     */
    fun calculateDrawdowns(
        prices: List<Double>,
    ): List<Double> {
        val drawdowns = mutableListOf<Double>()
        var lastMaxPrice = prices[0]
        for (i in 1..<prices.size) {
            val earnRate = RateCalculator.calculateEarnRate(prices[i], lastMaxPrice)
            drawdowns.add(minOf(0.0, earnRate))
            lastMaxPrice = maxOf(lastMaxPrice, prices[i])
        }
        return drawdowns
    }
}
