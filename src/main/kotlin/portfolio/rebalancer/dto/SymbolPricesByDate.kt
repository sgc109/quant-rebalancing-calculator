package portfolio.rebalancer.dto

import java.time.ZonedDateTime

data class SymbolPricesByDate(
    val value: Map<ZonedDateTime, Map<Asset, Double>>,
) {
    operator fun get(key: ZonedDateTime) = value[key]
}
