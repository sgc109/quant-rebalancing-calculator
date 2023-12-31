package portfolio.rebalancer.dto

import kotlinx.serialization.Serializable

@Serializable
data class StocksHistory(
    val date: String,
    val stocks: Map<String, Int>,
    val additionalDeposit: Int? = null,
    val strategy: String? = null,
)
