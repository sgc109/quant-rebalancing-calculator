package portfolio.rebalancer.strategy

import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.AssetShares
import portfolio.rebalancer.dto.SymbolPricesByDate
import java.time.Period
import java.time.ZonedDateTime

interface Strategy {
    val allAssets: Set<Asset>
    val rebalancingPeriod: Period
    val requiredPastMonths: Set<Int>

    fun balanceBudget(
        budget: Double,
        baseTime: ZonedDateTime,
        pricesInfo: SymbolPricesByDate,
    ): Result

    data class Result(
        val assetShares: AssetShares,
        val targetBuyMoneyPerAsset: Map<Asset, Double>,
        val unusedMoney: Double,
    )
}
