package portfolio.rebalancer.io

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
import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.SymbolPricesByDate
import java.time.ZonedDateTime

class MarketDataClient {
    fun getLatestMinuteBar(symbol: String = "SPY"): StockBar {
        return stockMarketData
            .getBars(
                symbol,
                ZonedDateTime.now().minusWeeks(1),
                ZonedDateTime.now(),
                MAX_FETCH_BARS_CNT,
                null,
                1,
                BarTimePeriod.MINUTE,
                BarAdjustment.SPLIT,
                BarFeed.IEX,
            ).bars.last()
    }

    @Deprecated("Use fetchPricesByPastMonth instead")
    suspend fun legacyFetchPricesByPastMonth(
        symbols: List<String>,
        scoringMonths: List<Int>,
        baseTime: ZonedDateTime,
    ): Map<Int, Map<String, Double>> = coroutineScope {
        (listOf(0) + scoringMonths).associateWith { pastMonth ->
            async(Dispatchers.IO) {
                batchGetPrices(
                    stockMarketData,
                    symbols.map { Asset.valueOf(it) },
                    baseTime,
                    pastMonth,
                )
            }
        }.also {
            it.values.awaitAll()
        }.mapValues { entry ->
            entry.value.await().mapKeys {
                it.key.name
            }
        }
    }

    suspend fun fetchPricesByPastMonth(
        symbols: List<Asset>,
        targetMonths: Collection<Int>,
        baseTime: ZonedDateTime,
    ): SymbolPricesByDate = coroutineScope {
        (targetMonths).associateWith { pastMonth ->
            async(Dispatchers.IO) {
                batchGetPrices(
                    stockMarketData,
                    symbols,
                    baseTime,
                    pastMonth,
                )
            }
        }.also {
            it.values.awaitAll()
        }.mapKeys {
            ZonedDateTime.now().minusMonths(it.key.toLong())
        }.mapValues {
            it.value.await()
        }.let {
            SymbolPricesByDate(it)
        }
    }

    /**
     * 각 종목에 대해 원하는 날짜가 휴장일 수 있으므로 기준 시점으로부터 7일 치 정보(일 봉)를 불러와 첫번쨋날의 종가를 사용한다
     */
    private fun batchGetPrices(
        stockMarketData: StockMarketDataEndpoint,
        symbols: List<Asset>,
        baseTime: ZonedDateTime,
        pastMonth: Int,
    ): Map<Asset, Double> {
        val startDate = baseTime.minusMonths(pastMonth.toLong()).minusDays(1)
        return stockMarketData
            .getBars(
                symbols.map { it.name },
                startDate,
                startDate.plusDays(7),
                MAX_FETCH_BARS_CNT,
                null,
                1,
                BarTimePeriod.DAY,
                BarAdjustment.SPLIT,
                BarFeed.IEX,
            ).bars.mapValues { it.value.first().close }
            .mapKeys { Asset.valueOf(it.key) }
    }

    companion object {
        const val MAX_FETCH_BARS_CNT = 10000

        val stockMarketData: StockMarketDataEndpoint = AlpacaAPI().stockMarketData()
    }
}
