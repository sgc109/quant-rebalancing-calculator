package portfolio.rebalancer.dto

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
