package portfolio.rebalancer

import kotlinx.coroutines.coroutineScope
import portfolio.rebalancer.io.MarketDataClient
import portfolio.rebalancer.io.StockHistoryFileManager
import portfolio.rebalancer.util.Loggable
import kotlin.math.abs
import kotlin.math.round

@Deprecated("Use ReBalancingRunner instead")
class LegacyHAAReBalancingHelper(
    private val stockHistoryFileManager: StockHistoryFileManager,
    private val marketDataClient: MarketDataClient,
) {
    suspend fun reBalance(
        additionalMoneyToDeposit: Int,
        moneyToWithdraw: Int,
        dryRun: Boolean = false,
    ): Result = coroutineScope {
        println("You're using HAA strategy")

        val stocksHistory = stockHistoryFileManager.loadLastStockPositionsMapFromFile()

        val originalStocksAmountMap: Map<String, Int> = stocksHistory?.stocks ?: emptyMap()

        println("You are adding $additionalMoneyToDeposit USD to your portfolio!")
        println("You are withdrawing $moneyToWithdraw USD to your portfolio!")

        val baseTime = marketDataClient.getLatestMinuteBar().timestamp
        println("baseTime: $baseTime")

        val pastMonthToSymbolToPrice: Map<Int, Map<String, Double>> =
            marketDataClient.legacyFetchPricesByPastMonth(
                (ALL_ASSETS + originalStocksAmountMap.keys).toList(),
                SCORING_MONTHS,
                baseTime,
            )

        println(pastMonthToSymbolToPrice)
        val symbolToCurrentPrice = pastMonthToSymbolToPrice[0]!!

        val symbolToMomentumScore = calculateMomentumScores(symbolToCurrentPrice, pastMonthToSymbolToPrice)

        println("symbolToMomentumScore=$symbolToMomentumScore")

        val originalTotalPrice = originalStocksAmountMap.map {
            symbolToCurrentPrice[it.key]!! * it.value
        }.sum()
        val newTotalMoney = originalTotalPrice + additionalMoneyToDeposit - moneyToWithdraw
        require(newTotalMoney > 0) { "'newTotalMoney' should not be negative. Please reduce withdrawal amount." }

        val aggressiveAssetsToBuy = AGGRESSIVE_ASSETS.associateWith { symbol ->
            symbolToMomentumScore[symbol]!!
        }.entries
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
            .take(CNT_AGGRESSIVE_ASSETS_TO_BUY)
            .map { it.key }

        val aggressiveAssetsBudget = if (symbolToMomentumScore[CANARY_ASSETS.first()]!! > 0) {
            println("You should buy aggressive assets: $aggressiveAssetsToBuy")

            newTotalMoney * 0.25 * aggressiveAssetsToBuy.size
        } else {
            0.0
        }
        val defensiveAssetsBudget = newTotalMoney - aggressiveAssetsBudget

        val budgetForEachAggressiveAsset = if (aggressiveAssetsBudget > 0) {
            aggressiveAssetsBudget / aggressiveAssetsToBuy.size
        } else {
            0.0
        }

        val aggressiveAssetsAmountToBuyBySymbol = aggressiveAssetsToBuy.associateWith {
            (budgetForEachAggressiveAsset / symbolToCurrentPrice[it]!!).toInt()
        }

        val defensiveAssetToBuy = if (defensiveAssetsBudget > 0) {
            DEFENSIVE_ASSETS.associateWith { symbol ->
                symbolToMomentumScore[symbol]!!
            }.maxBy { it.value }.key.also {
                println("You should buy defensive assets: $it")
            }
        } else {
            println("You shouldn't buy any defensive assets")
            null
        }

        val defensiveAssetAmountToBuy = defensiveAssetToBuy?.let {
            (defensiveAssetsBudget / symbolToCurrentPrice[defensiveAssetToBuy]!!).toInt()
        } ?: 0

        val defensiveAssetsAndAmountToBuyPair = defensiveAssetToBuy?.let {
            mapOf(
                defensiveAssetToBuy to defensiveAssetAmountToBuy,
            )
        }
        val resultAmountsBySymbol =
            mergeTwoStringToIntMaps(
                aggressiveAssetsAmountToBuyBySymbol,
                defensiveAssetsAndAmountToBuyPair,
            )

        val unusedPercentageForAggressiveBudgets = aggressiveAssetsAmountToBuyBySymbol.mapValues {
            val usedMoney = symbolToCurrentPrice[it.key]!! * it.value
            (budgetForEachAggressiveAsset - usedMoney) / budgetForEachAggressiveAsset * 100
        }

        val usedMoneyToBuyDefensiveAssets =
            defensiveAssetToBuy?.let {
                symbolToCurrentPrice[defensiveAssetToBuy]!! * defensiveAssetAmountToBuy
            } ?: 0.0

        val unusedPercentages =
            defensiveAssetToBuy?.let {
                val unusedPercentageForDefensiveBudget =
                    (defensiveAssetsBudget - usedMoneyToBuyDefensiveAssets) / defensiveAssetsBudget * 100
                unusedPercentageForAggressiveBudgets +
                    (defensiveAssetToBuy to unusedPercentageForDefensiveBudget)
            } ?: unusedPercentageForAggressiveBudgets

        val usedMoneyToBuyAggressiveAssets = aggressiveAssetsAmountToBuyBySymbol.entries.sumOf {
            symbolToCurrentPrice[it.key]!! * it.value
        }
        val totalMoneyUsed = usedMoneyToBuyAggressiveAssets + usedMoneyToBuyDefensiveAssets
        val unusedMoney = newTotalMoney - totalMoneyUsed
        val totalUnusedPercentage = round(unusedMoney / newTotalMoney * 100).toInt()

        printWhatToBuyAndSell(ALL_ASSETS, resultAmountsBySymbol, originalStocksAmountMap)

        if (!dryRun) {
            stockHistoryFileManager.legacyWriteNewStockPositionsToFile(
                isFirstPosition = stocksHistory == null,
                resultAmountsBySymbol,
                additionalMoneyToDeposit,
            )
        }

        Result(
            unusedPercentages,
            totalUnusedPercentage,
        )
    }

    private fun calculateMomentumScores(
        symbolToCurrentPrice: Map<String, Double>,
        pastMonthToSymbolToPrice: Map<Int, Map<String, Double>>,
    ) = ALL_ASSETS.associateWith { symbol ->
        SCORING_MONTHS.sumOf { pastMonth ->
            val basePrice = symbolToCurrentPrice[symbol]!!
            val pastPrice = pastMonthToSymbolToPrice[pastMonth]?.get(symbol)!!
            val earnRatio = calculateEarnRate(basePrice = basePrice, pastPrice = pastPrice)
            earnRatio
        } / SCORING_MONTHS.size
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
                println("Sell $symbol by ${abs(diff)}!")
            }
        }
    }

    private fun mergeTwoStringToIntMaps(
        m1: Map<String, Int>?,
        m2: Map<String, Int>?,
    ): Map<String, Int> {
        require(m1 != null || m2 != null) { "Both maps are null" }
        if (m1 == null) {
            return m2!!
        }
        if (m2 == null) {
            return m1
        }
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
        return basePrice / pastPrice - 1.0
    }

    data class Result(
        val unusedPercentages: Map<String, Double>,
        val totalUnusedPercentage: Int,
    ) {
        fun printResult() {
            unusedPercentages.forEach {
                println("unused money percent(${it.key}): ${it.value}%")
            }

            println("total unusedMoney: $totalUnusedPercentage%")
        }

        fun isAllAccurateUnderPercent(percent: Int): Boolean {
            return unusedPercentages.values.all { it < percent.toDouble() }
        }
    }

    companion object : Loggable {
        const val CNT_AGGRESSIVE_ASSETS_TO_BUY = 4

        val AGGRESSIVE_ASSETS = setOf(
            "SPY",
            "IWM",
            "EFA",
            "EEM",
            "VNQ",
            "PDBC",
            "IEF",
            "TLT",
        )
        val DEFENSIVE_ASSETS = setOf("IEF", "BIL")
        val CANARY_ASSETS = setOf("TIP")
        val ALL_ASSETS = AGGRESSIVE_ASSETS + DEFENSIVE_ASSETS + CANARY_ASSETS

        val SCORING_MONTHS = listOf(1, 3, 6, 12) // 13612U 모멘텀 스코어를 계산하기 위한 months
    }
}
