import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.jacobpeterson.alpaca.AlpacaAPI
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed
import net.jacobpeterson.alpaca.rest.endpoint.marketdata.stock.StockMarketDataEndpoint
import java.time.ZonedDateTime
import kotlin.math.round

const val MAX_FETCH_BARS_CNT = 10000
const val ADDITIONAL_MONEY = 10000
const val CNT_AGGRESSIVE_ASSETS_TO_BUY = 5

fun main() = runBlocking {
    // val yamlString =
    //     javaClass.classLoader.getResourceAsStream("stocks-history.yaml")
    //         ?.bufferedReader()
    //         ?.use { it.readText() }
    //         ?: throw FileNotFoundException()
    // val stocksHistory = if (yamlString.isNotBlank()) {
    //     Yaml.default.decodeFromString(StocksHistory.serializer(), yamlString)
    // } else {
    //     null
    // }
    val originalStocksAmountMap: Map<String, Int> = emptyMap()

    val alpacaAPI = AlpacaAPI()
    val stockMarketData = alpacaAPI.stockMarketData()

    println("You are adding $ADDITIONAL_MONEY USD to your portfolio!")

    val baseTime = getLatestDayBar(stockMarketData).timestamp
    val aggressiveAssets = setOf(
        "SPY", "QQQ", "IWM", "VGK", "EWJ", "EEM", "VNQ", "GLD", "DBC", "HYG", "LQD", "TLT",
    )
    val defensiveAssets = setOf("LQD", "IEF", "SHY")
    val allAssets = aggressiveAssets + defensiveAssets
    val scoringMonths = listOf(1, 3, 6, 12)

    val pastMonthToSymbolToPrice = (listOf(0) + scoringMonths).associateWith { pastMonth ->
        async(Dispatchers.IO) {
            getPrice(
                stockMarketData,
                allAssets.toList(),
                baseTime,
                pastMonth,
            )
        }
    }.also {
        it.values.awaitAll()
    }.mapValues {
        it.value.await()
    }

    println(pastMonthToSymbolToPrice)
    val symbolToCurrentPrice = pastMonthToSymbolToPrice[0]!!

    val symbolToMomentumScore = allAssets.associateWith { symbol ->
        scoringMonths.sumOf { pastMonth ->
            val basePrice = symbolToCurrentPrice[symbol]!!
            val pastPrice = pastMonthToSymbolToPrice[pastMonth]?.get(symbol)!!
            val earnRatio = calculateEarnRate(basePrice = basePrice, pastPrice = pastPrice)
            val weight = 12 / pastMonth
            earnRatio * weight
        }
    }

    println(symbolToMomentumScore)

    val cntNegativeScores = aggressiveAssets.map {
        symbolToMomentumScore[it]!!
    }.count { it < 0 }

    val defensiveInvestingRatio = cntNegativeScores.coerceAtMost(4) * 0.25
    println("defensiveInvestingRatio: ${(defensiveInvestingRatio * 100).toInt()}%")

    val newTotalMoney = originalStocksAmountMap.map {
        symbolToCurrentPrice[it.key]!! * it.value
    }.sum() + ADDITIONAL_MONEY

    val defensiveAssetsBudget = newTotalMoney * defensiveInvestingRatio
    val aggressiveAssetsBudget = newTotalMoney - defensiveAssetsBudget

    val defensiveAssetToBuy = defensiveAssets.associateWith { symbol ->
        symbolToMomentumScore[symbol]!!
    }.maxBy { it.value }.key
    println("You should buy defensive assets: $defensiveAssetToBuy")

    val aggressiveAssetsToBuy = aggressiveAssets.associateWith { symbol ->
        symbolToMomentumScore[symbol]!!
    }.entries.sortedByDescending { it.value }
        .take(CNT_AGGRESSIVE_ASSETS_TO_BUY)
        .map { it.key }
    println("You should buy aggressive assets: $aggressiveAssetsToBuy")

    val budgetForEachAggressiveAsset = aggressiveAssetsBudget / CNT_AGGRESSIVE_ASSETS_TO_BUY

    val aggressiveAssetsAmountToBuyBySymbol = aggressiveAssetsToBuy.associateWith {
        (budgetForEachAggressiveAsset / symbolToCurrentPrice[it]!!).toInt()
    }

    val usedMoneyToBuyAggressiveAssets = aggressiveAssetsAmountToBuyBySymbol.entries.sumOf {
        symbolToCurrentPrice[it.key]!! * it.value
    }
    val defensiveAssetAmountToBuy = (defensiveAssetsBudget / symbolToCurrentPrice[defensiveAssetToBuy]!!).toInt()
    val defensiveAssetsAndAmountToBuyPair = mapOf(
        defensiveAssetToBuy to defensiveAssetAmountToBuy,
    )
    val usedMoneyToBuyDefensiveAssets = symbolToCurrentPrice[defensiveAssetToBuy]!! * defensiveAssetAmountToBuy

    val resultAmountsBySymbol =
        aggressiveAssetsAmountToBuyBySymbol + defensiveAssetsAndAmountToBuyPair

    val totalMoneyUsed = usedMoneyToBuyAggressiveAssets + usedMoneyToBuyDefensiveAssets
    val unusedMoney = newTotalMoney - totalMoneyUsed
    println("unusedMoney: $unusedMoney (${round(unusedMoney / newTotalMoney * 100).toInt()}%)")

    allAssets.forEach { symbol ->
        val diff = (resultAmountsBySymbol[symbol] ?: 0) - (originalStocksAmountMap[symbol] ?: 0)
        if (diff > 0) {
            println("Buy $symbol by $diff!")
        } else if (diff < 0) {
            println("Sell $symbol by $diff!")
        }
    }
}

fun calculateEarnRate(basePrice: Double, pastPrice: Double): Double {
    return (basePrice - pastPrice) / basePrice
}

fun getLatestDayBar(stockMarketData: StockMarketDataEndpoint, symbol: String = "SPY"): StockBar {
    return stockMarketData
        .getBars(
            symbol,
            ZonedDateTime.now().minusWeeks(1),
            ZonedDateTime.now(),
            MAX_FETCH_BARS_CNT,
            null,
            1,
            BarTimePeriod.DAY,
            BarAdjustment.SPLIT,
            BarFeed.IEX,
        ).bars.last()
}

fun getPrice(
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
            MAX_FETCH_BARS_CNT,
            null,
            1,
            BarTimePeriod.DAY,
            BarAdjustment.SPLIT,
            BarFeed.IEX,
        ).bars.mapValues { it.value.first().close }
}
