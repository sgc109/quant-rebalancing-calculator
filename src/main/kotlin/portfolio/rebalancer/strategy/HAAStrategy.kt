package portfolio.rebalancer.strategy

import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.Asset.BIL
import portfolio.rebalancer.dto.Asset.EEM
import portfolio.rebalancer.dto.Asset.EFA
import portfolio.rebalancer.dto.Asset.IEF
import portfolio.rebalancer.dto.Asset.IWM
import portfolio.rebalancer.dto.Asset.PDBC
import portfolio.rebalancer.dto.Asset.DBC
import portfolio.rebalancer.dto.Asset.SPY
import portfolio.rebalancer.dto.Asset.TIP
import portfolio.rebalancer.dto.Asset.TLT
import portfolio.rebalancer.dto.Asset.VNQ
import portfolio.rebalancer.dto.SymbolPricesByDate
import portfolio.rebalancer.dto.toAssetShares
import portfolio.rebalancer.util.ClosestDateTimeFinder
import portfolio.rebalancer.util.RateCalculator
import java.time.Period
import java.time.ZonedDateTime

class HAAStrategy : Strategy {
    override val allAssets = AGGRESSIVE_ASSETS + DEFENSIVE_ASSETS + CANARY_ASSETS
    override val rebalancingPeriod: Period = Period.ofMonths(1)
    override val requiredPastMonths = setOf(1, 3, 6, 12) // 13612U 모멘텀 스코어를 계산하기 위해

    /**
     * @param pricesInfo 각 날짜별로 각 자산의 가격 정보를 담고 있는 Map. 전략에 필요한 시점의 데이터만 넣어주면 된다.
     *               TODO: 필요한 날짜 목록을 ZonedDateTime 으로 내려주는 메서드를 캡슐화할지 고민
     */
    override fun balanceBudget(
        budget: Double,
        baseTime: ZonedDateTime,
        pricesInfo: SymbolPricesByDate,
    ): Strategy.Result {
        val pastMonthToSymbolToPrice = pricesInfo.value.let {
            val dates = it.keys.toList()
            (requiredPastMonths + 0).associateWith { pastMonth ->
                val baseTimeForSearch = baseTime.minusMonths(pastMonth.toLong())
                val closestTime = ClosestDateTimeFinder.findClosestDate(baseTimeForSearch, dates)
                it[closestTime] ?: throw NoSuchElementException("There is no closestTime in given fetched prices data")
            }
        }
        val symbolToMomentumScore = calculateMomentumScores(pastMonthToSymbolToPrice)
        val symbolToCurrentPrice = ClosestDateTimeFinder.findClosestDate(
            baseTime = baseTime,
            pricesInfo.value.keys.toList(),
        ).let { pricesInfo.value[it]!! }

        println("symbolToMomentumScore=${symbolToMomentumScore.entries.toList().sortedByDescending { it.value }}")

        val (aggressiveAssetsToBuy, aggressiveAssetsBudget) = calculateAggressiveInfoToBuy(
            symbolToMomentumScore,
            budget,
        )
        val defensiveAssetsBudget = budget - aggressiveAssetsBudget

        val budgetForEachAggressiveAsset = if (aggressiveAssetsBudget > 0) {
            aggressiveAssetsBudget / aggressiveAssetsToBuy.size
        } else {
            0.0
        }

        val aggressiveAssetSharesToBuy = if (aggressiveAssetsBudget > 0) {
            aggressiveAssetsToBuy.associateWith {
                (budgetForEachAggressiveAsset / symbolToCurrentPrice[it]!!).toLong()
            }
        } else {
            emptyMap()
        }.toAssetShares()

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
            (defensiveAssetsBudget / symbolToCurrentPrice[defensiveAssetToBuy]!!).toLong()
        } ?: 0

        val resultAmountsBySymbol =
            defensiveAssetToBuy?.let {
                aggressiveAssetSharesToBuy.add(it, defensiveAssetAmountToBuy)
            } ?: aggressiveAssetSharesToBuy

        val aggressiveAssetsTargetBuyPrice =
            aggressiveAssetsToBuy.associateWith {
                budgetForEachAggressiveAsset
            }
        return Strategy.Result(
            assetShares = resultAmountsBySymbol,
            targetBuyMoneyPerAsset = defensiveAssetToBuy?.let {
                aggressiveAssetsTargetBuyPrice.mergeHashMap(mapOf(it to defensiveAssetsBudget))
            } ?: aggressiveAssetsTargetBuyPrice,
            unusedMoney = budget - resultAmountsBySymbol.value.entries.sumOf { symbolToCurrentPrice[it.key]!! * it.value },
        )
    }

    private fun calculateAggressiveInfoToBuy(
        symbolToMomentumScore: Map<Asset, Double>,
        budget: Double,
    ): Pair<List<Asset>, Double> {
        val shouldHaveOnlyDefensive = symbolToMomentumScore[CANARY_ASSETS.first()]!! < 0
        val (aggressiveAssetsToBuy, aggressiveAssetsBudget) =
            if (shouldHaveOnlyDefensive) {
                println("You should only have defensive assets")
                Pair(emptyList(), 0.0)
            } else {
                val aggressiveAssetsToBuy = AGGRESSIVE_ASSETS.associateWith { symbol ->
                    symbolToMomentumScore[symbol]!!
                }.entries
                    .filter { it.value > 0.0 }
                    .sortedByDescending { it.value }
                    .take(CNT_AGGRESSIVE_ASSETS_TO_BUY)
                    .map { it.key }
                println("You should buy aggressive assets: $aggressiveAssetsToBuy")

                val aggressiveAssetsBudget = budget * 0.25 * aggressiveAssetsToBuy.size
                Pair(aggressiveAssetsToBuy, aggressiveAssetsBudget)
            }
        return Pair(aggressiveAssetsToBuy, aggressiveAssetsBudget)
    }

    private fun Map<Asset, Double>.mergeHashMap(other: Map<Asset, Double>): Map<Asset, Double> {
        val mergedValue = this.toMutableMap()
        other.forEach { (symbol, v) ->
            mergedValue[symbol] = mergedValue.getOrDefault(symbol, 0.0) + v
        }
        return mergedValue
    }

    private fun calculateMomentumScores(
        pastMonthToSymbolToPrice: Map<Int, Map<Asset, Double>>,
    ): Map<Asset, Double> {
        val symbolToCurrentPrice = pastMonthToSymbolToPrice.keys.min().let { pastMonthToSymbolToPrice[it]!! }

        return allAssets.associateWith { symbol ->
            requiredPastMonths.sumOf { pastMonth ->
                if (!symbolToCurrentPrice.containsKey(symbol)) {
                    println("'symbolToCurrentPrice' doesn't contain price data!!!!!!!!!!!!!!!!!!!symbol=$symbol, pastMonth=$pastMonth!!!!!!!!!!!!!")
                }
                val basePrice = symbolToCurrentPrice[symbol]!!
                if (!pastMonthToSymbolToPrice.containsKey(pastMonth)) {
                    println("symbol=$symbol, pastMonth=$pastMonth")
                }
                if (!pastMonthToSymbolToPrice[pastMonth]!!.containsKey(symbol)) {
                    println("symbol=$symbol, pastMonth=$pastMonth")
                }
                val pastPrice = pastMonthToSymbolToPrice[pastMonth]?.get(symbol)!!
                val earnRate = RateCalculator.calculateEarnRate(curPrice = basePrice, pastPrice = pastPrice)
                earnRate
            } / requiredPastMonths.size
        }
    }

    companion object {
        private const val CNT_AGGRESSIVE_ASSETS_TO_BUY = 4

        private val AGGRESSIVE_ASSETS = setOf(SPY, IWM, EFA, EEM, VNQ, DBC, IEF, TLT)
        private val DEFENSIVE_ASSETS = setOf(IEF, BIL)
        private val CANARY_ASSETS = setOf(TIP)
    }
}
