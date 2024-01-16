package portfolio.rebalancer.util

fun calculateProfitRate(prevPrice: Double, currentPrice: Double): Double {
    return (currentPrice - prevPrice) / prevPrice * 100.0
}
