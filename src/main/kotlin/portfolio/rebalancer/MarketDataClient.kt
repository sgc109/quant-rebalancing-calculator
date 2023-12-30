package portfolio.rebalancer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.jacobpeterson.alpaca.AlpacaAPI
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed
import net.jacobpeterson.alpaca.rest.endpoint.marketdata.stock.StockMarketDataEndpoint
import java.time.ZonedDateTime

class MarketDataClient {
    fun getLatestMinuteBar(symbol: String = "SPY"): StockBar {
        return stockMarketData
            .getBars(
                symbol,
                ZonedDateTime.now().minusWeeks(1),
                ZonedDateTime.now(),
                VAAReBalancingHelper.MAX_FETCH_BARS_CNT,
                null,
                1,
                BarTimePeriod.MINUTE,
                BarAdjustment.SPLIT,
                BarFeed.IEX,
            ).bars.last()
    }

    suspend fun fetchPricesByPastMonth(
        baseTime: ZonedDateTime,
    ): Map<Int, Map<String, Double>> = coroutineScope {
        (listOf(0) + VAAReBalancingHelper.SCORING_MONTHS).associateWith { pastMonth ->
            async(Dispatchers.IO) {
                batchGetPrices(
                    stockMarketData,
                    VAAReBalancingHelper.ALL_ASSETS.toList(),
                    baseTime,
                    pastMonth,
                )
            }
        }.also {
            it.values.awaitAll()
        }.mapValues {
            it.value.await()
        }
    }

    private fun batchGetPrices(
        stockMarketData: StockMarketDataEndpoint,
        symbols: List<String>,
        baseTime: ZonedDateTime,
        pastMonth: Int,
    ): Map<String, Double> {
        val startDate = baseTime.minusMonths(pastMonth.toLong()).minusDays(1)
        return stockMarketData
            .getBars(
                symbols,
                startDate,
                startDate.plusDays(7),
                VAAReBalancingHelper.MAX_FETCH_BARS_CNT,
                null,
                1,
                BarTimePeriod.DAY,
                BarAdjustment.SPLIT,
                BarFeed.IEX,
            ).bars.mapValues { it.value.first().close }
    }

    companion object {
        val stockMarketData: StockMarketDataEndpoint = AlpacaAPI().stockMarketData()
    }
}
