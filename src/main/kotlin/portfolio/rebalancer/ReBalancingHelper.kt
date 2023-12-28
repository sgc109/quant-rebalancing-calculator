package portfolio.rebalancer

import kotlinx.coroutines.coroutineScope
import kotlin.math.round

class ReBalancingHelper(
    private val stockHistoryFileManager: StockHistoryFileManager,
    private val marketDataClient: MarketDataClient,
) {
    suspend fun reBalance(
        additionalMoneyToDeposit: Int,
        moneyToWithdraw: Int,
    ) = coroutineScope {
        val stocksHistory = stockHistoryFileManager.loadLastStockPositionsMapFromFile()

        val originalStocksAmountMap: Map<String, Int> = stocksHistory?.stocks ?: emptyMap()

        println("You are adding $additionalMoneyToDeposit USD to your portfolio!")

        val baseTime = marketDataClient.getLatestMinuteBar().timestamp
        println("baseTime: $baseTime")

        val pastMonthToSymbolToPrice: Map<Int, Map<String, Double>> =
            marketDataClient.fetchPricesByPastMonth(baseTime)

        println(pastMonthToSymbolToPrice)
        val symbolToCurrentPrice = pastMonthToSymbolToPrice[0]!!

        val symbolToMomentumScore = ALL_ASSETS.associateWith { symbol ->
            SCORING_MONTHS.sumOf { pastMonth ->
                val basePrice = symbolToCurrentPrice[symbol]!!
                val pastPrice = pastMonthToSymbolToPrice[pastMonth]?.get(symbol)!!
                val earnRatio = calculateEarnRate(basePrice = basePrice, pastPrice = pastPrice)
                val weight = 12 / pastMonth
                earnRatio * weight
            }
        }

        println(symbolToMomentumScore)

        val cntNegativeScores = AGGRESSIVE_ASSETS.map {
            symbolToMomentumScore[it]!!
        }.count { it < 0 }

        val defensiveInvestingRatio = cntNegativeScores.coerceAtMost(4) * 0.25
        println("defensiveInvestingRatio: ${(defensiveInvestingRatio * 100).toInt()}%")

        val newTotalMoney = originalStocksAmountMap.map {
            symbolToCurrentPrice[it.key]!! * it.value
        }.sum() + additionalMoneyToDeposit - moneyToWithdraw

        val defensiveAssetsBudget = newTotalMoney * defensiveInvestingRatio
        val aggressiveAssetsBudget = newTotalMoney - defensiveAssetsBudget

        val defensiveAssetToBuy = DEFENSIVE_ASSETS.associateWith { symbol ->
            symbolToMomentumScore[symbol]!!
        }.maxBy { it.value }.key
        println("You should buy defensive assets: $defensiveAssetToBuy")

        val aggressiveAssetsToBuy = AGGRESSIVE_ASSETS.associateWith { symbol ->
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
            val usedMoney = symbolToCurrentPrice[it.key]!! * it.value
            println("unused money percent(${it.key}): ${(budgetForEachAggressiveAsset - usedMoney) / budgetForEachAggressiveAsset * 100}%")
            usedMoney
        }
        val defensiveAssetAmountToBuy = (defensiveAssetsBudget / symbolToCurrentPrice[defensiveAssetToBuy]!!).toInt()
        val defensiveAssetsAndAmountToBuyPair = mapOf(
            defensiveAssetToBuy to defensiveAssetAmountToBuy,
        )
        val usedMoneyToBuyDefensiveAssets = symbolToCurrentPrice[defensiveAssetToBuy]!! * defensiveAssetAmountToBuy
        println("unused money percent($defensiveAssetToBuy): ${(defensiveAssetsBudget - usedMoneyToBuyDefensiveAssets) / defensiveAssetsBudget * 100}%")

        val resultAmountsBySymbol =
            mergeTwoStringToIntMaps(
                aggressiveAssetsAmountToBuyBySymbol,
                defensiveAssetsAndAmountToBuyPair,
            )

        val totalMoneyUsed = usedMoneyToBuyAggressiveAssets + usedMoneyToBuyDefensiveAssets
        val unusedMoney = newTotalMoney - totalMoneyUsed
        println("unusedMoney: $unusedMoney USD (${round(unusedMoney / newTotalMoney * 100).toInt()}%)")

        printWhatToBuyAndSell(ALL_ASSETS, resultAmountsBySymbol, originalStocksAmountMap)

        stockHistoryFileManager.writeNewStockPositionsToFile(
            isFirstPosition = stocksHistory == null,
            resultAmountsBySymbol,
        )
    }

    private fun printWhatToBuyAndSell(
        allAssets: Set<String>,
        resultAmountsBySymbol: Map<String, Int>,
        originalStocksAmountMap: Map<String, Int>,
    ) {
        allAssets.forEach { symbol ->
            val diff = (resultAmountsBySymbol[symbol] ?: 0) - (originalStocksAmountMap[symbol] ?: 0)
            if (diff > 0) {
                println("Buy $symbol by $diff!")
            } else if (diff < 0) {
                println("Sell $symbol by $diff!")
            }
        }
    }

    private fun mergeTwoStringToIntMaps(
        m1: Map<String, Int>,
        m2: Map<String, Int>,
    ): Map<String, Int> {
        return (m1.keys + m2.keys).distinct().mapNotNull { key ->
            if (m1.containsKey(key) && m2.containsKey(key)) {
                key to (m1[key]!! + m2[key]!!)
            } else if (m1.containsKey(key)) {
                key to m1[key]!!
            } else if (m2.containsKey(key)) {
                key to m2[key]!!
            } else {
                null
            }
        }.toMap()
    }

    private fun calculateEarnRate(basePrice: Double, pastPrice: Double): Double {
        return (basePrice - pastPrice) / basePrice
    }

    companion object {
        const val MAX_FETCH_BARS_CNT = 10000
        const val CNT_AGGRESSIVE_ASSETS_TO_BUY = 5

        val AGGRESSIVE_ASSETS = setOf(
            "SPY", "QQQ", "IWM", "VGK", "EWJ", "EEM", "VNQ", "GLD", "DBC", "HYG", "LQD", "TLT",
        )
        val DEFENSIVE_ASSETS = setOf("LQD", "IEF", "SHY")
        val ALL_ASSETS = AGGRESSIVE_ASSETS + DEFENSIVE_ASSETS

        val SCORING_MONTHS = listOf(1, 3, 6, 12) // 13612W 모멘텀 스코어를 계산하기 위한 months
    }
}
