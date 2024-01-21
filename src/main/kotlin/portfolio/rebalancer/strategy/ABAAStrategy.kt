package portfolio.rebalancer.strategy

import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.Asset.BIL
import portfolio.rebalancer.dto.Asset.BND
import portfolio.rebalancer.dto.Asset.DBC
import portfolio.rebalancer.dto.Asset.EEM
import portfolio.rebalancer.dto.Asset.EFA
import portfolio.rebalancer.dto.Asset.IEF
import portfolio.rebalancer.dto.Asset.LQD
import portfolio.rebalancer.dto.Asset.QQQ
import portfolio.rebalancer.dto.Asset.SPY
import portfolio.rebalancer.dto.Asset.TIP
import portfolio.rebalancer.dto.Asset.TLT
import portfolio.rebalancer.dto.SymbolPricesByDate
import java.time.Period
import java.time.ZonedDateTime

class ABAAStrategy : Strategy {
    override val allAssets: Set<Asset> = AGGRESSIVE_ASSETS + DEFENSIVE_ASSETS + CANARY_ASSETS
    override val rebalancingPeriod: Period = Period.ofMonths(1)
    override val requiredPastMonths: Set<Int> = (1..12).toSet()

    override fun balanceBudget(
        budget: Double,
        baseTime: ZonedDateTime,
        pricesInfo: SymbolPricesByDate,
    ): Strategy.Result {
        TODO("Not yet implemented")
    }

    companion object {
        private val AGGRESSIVE_ASSETS = setOf(QQQ, EFA, EEM, BND)
        private val DEFENSIVE_ASSETS = setOf(BND, BIL, IEF, TLT, TIP, LQD, DBC)
        private val CANARY_ASSETS = setOf(SPY, EEM, EFA, BND)
    }
}
