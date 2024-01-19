package portfolio.rebalancer.dto

/**
 * 각 종목 별 수량을 HashMap 형태로 가지고 있는 클래스
 */
data class AssetShares(
    val value: Map<Asset, Long>,
) {
    fun merge(other: AssetShares?): AssetShares {
        if (other == null) {
            return this
        }
        val mergedValue = value.toMutableMap()
        other.value.forEach { (symbol, amount) ->
            mergedValue[symbol] = mergedValue.getOrDefault(symbol, 0L) + amount
        }
        return AssetShares(mergedValue)
    }

    fun add(asset: Asset, amount: Long) = AssetShares(value = value + (asset to amount))
}

fun Map<Asset, Long>.toAssetShares(): AssetShares {
    return AssetShares(this)
}
