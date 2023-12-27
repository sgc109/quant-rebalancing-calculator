import kotlinx.serialization.Serializable

@Serializable
data class StocksHistory(
    val date: String,
    val stocks: Map<String, Long>,
)
