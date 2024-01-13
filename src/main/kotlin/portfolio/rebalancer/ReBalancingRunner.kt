package portfolio.rebalancer

import kotlinx.coroutines.coroutineScope
import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.SymbolPricesByDate
import portfolio.rebalancer.io.MarketDataClient
import portfolio.rebalancer.io.StockHistoryFileManager
import portfolio.rebalancer.strategy.Strategy
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.round

class ReBalancingRunner(
    private val stockHistoryFileManager: StockHistoryFileManager,
    private val marketDataClient: MarketDataClient,
    private val strategy: Strategy,
) {
    /**
     * 기존 보유 수량들에 대한 정보가 기록된 YAML 파일을 읽어, 주어진 전략으로 리밸런싱을 수행한 후에, 결과를 다시 YAML 파일에 쓰는 역햘 수행.
     *
     * @return 리밸런싱 결과에 대한 리포트를 반환(e.g. 리밸런싱을 수행하면서 각 자산의 목표 매입 금액 대비 실제 매입 금액이 몇 퍼센트나 차이가 나는지)
     */
    suspend fun reBalance(
        additionalMoneyToDeposit: Int,
        moneyToWithdraw: Int,
        dryRun: Boolean = false,
    ): Result = coroutineScope {
        println("You're using HAA strategy")

        val stocksHistory = stockHistoryFileManager.loadLastStockPositionsMapFromFile()

        val originalStocksAmountMap: Map<Asset, Int> =
            stocksHistory?.stocks?.mapKeys { Asset.valueOf(it.key) } ?: emptyMap()

        println("You are adding $additionalMoneyToDeposit USD to your portfolio!")
        println("You are withdrawing $moneyToWithdraw USD to your portfolio!")

        val currentTime = marketDataClient.getLatestMinuteBar().timestamp.withZoneSameLocal(ZoneId.systemDefault())
        println("currentTime: $currentTime")

        val pastMonthToSymbolToPrice: SymbolPricesByDate =
            marketDataClient.fetchPricesByPastMonth(
                (strategy.allAssets + originalStocksAmountMap.keys).toList(),
                targetMonths = strategy.requiredPastMonths + 0,
                currentTime,
            )

        println("pastMonthToSymbolToPrice=$pastMonthToSymbolToPrice")
        val symbolToCurrentPrice = pastMonthToSymbolToPrice.value[pastMonthToSymbolToPrice.value.keys.max()]!!

        val originalTotalPrice = originalStocksAmountMap.map {
            symbolToCurrentPrice[it.key]!! * it.value
        }.sum()

        val totalValueEstimated = originalTotalPrice + additionalMoneyToDeposit - moneyToWithdraw

        require(totalValueEstimated > 0) { "'totalValueEstimated' should not be negative. Please reduce withdrawal amount." }

        // Execute re-balancing
        val reBalancedResult = strategy.balanceBudget(
            budget = totalValueEstimated,
            baseTime = currentTime,
            pricesInfo = pastMonthToSymbolToPrice,
        )
        val rebalancedAssetShares = reBalancedResult.assetShares

        val unusedPercentages = reBalancedResult.targetBuyMoneyPerAsset.mapValues {
            (it.value - symbolToCurrentPrice[it.key]!! * rebalancedAssetShares.value[it.key]!!) / it.value * 100.0
        }
        val totalMoneyUsed = rebalancedAssetShares.value.entries.sumOf {
            symbolToCurrentPrice[it.key]!! * it.value
        }
        val unusedMoney = totalValueEstimated - totalMoneyUsed
        val totalUnusedPercentage = round(unusedMoney / totalValueEstimated * 100).toInt()

        printWhatToBuyAndSell(
            allAssets = strategy.allAssets,
            resultAmountsBySymbol = rebalancedAssetShares.value,
            originalStocksAmountMap,
        )

        if (!dryRun) {
            stockHistoryFileManager.writeNewStockPositionsToFile(
                isFirstPosition = stocksHistory == null,
                resultAmountsBySymbol = rebalancedAssetShares.value,
                additionalMoneyToDeposit = additionalMoneyToDeposit,
            )
        }

        Result(
            unusedPercentages,
            totalUnusedPercentage,
        )
    }

    private fun printWhatToBuyAndSell(
        allAssets: Set<Asset>,
        resultAmountsBySymbol: Map<Asset, Long>,
        originalStocksAmountMap: Map<Asset, Int>,
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

    data class Result(
        val unusedPercentages: Map<Asset, Double>,
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
}
