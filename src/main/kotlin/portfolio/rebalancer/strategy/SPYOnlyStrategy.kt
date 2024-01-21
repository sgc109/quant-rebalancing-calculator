package portfolio.rebalancer.strategy

import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.Asset.SPY
import portfolio.rebalancer.dto.AssetShares
import portfolio.rebalancer.dto.SymbolPricesByDate
import java.time.Period
import java.time.ZonedDateTime

class SPYOnlyStrategy() : Strategy {
    override val allAssets: Set<Asset> = setOf(SPY)
    override val rebalancingPeriod: Period = Period.ofYears(9999)
    override val requiredPastMonths: Set<Int> = emptySet()
    override fun balanceBudget(
        budget: Double,
        baseTime: ZonedDateTime,
        pricesInfo: SymbolPricesByDate,
    ): Strategy.Result {
        val price = pricesInfo[baseTime]!![SPY]!!
        val amount = (budget / price).toLong()
        return Strategy.Result(
            assetShares = AssetShares(
                value = mapOf(SPY to amount),
            ),
            targetBuyMoneyPerAsset = mapOf(SPY to budget),
            unusedMoney = budget - amount * price,
        )
    }
}
