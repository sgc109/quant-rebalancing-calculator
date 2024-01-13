package portfolio.rebalancer.io

import com.charleskorn.kaml.Yaml
import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.StocksHistory
import portfolio.rebalancer.util.Loggable
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class StockHistoryFileManager {
    fun loadLastStockPositionsMapFromFile(): StocksHistory? {
        val yamlString =
            javaClass.classLoader.getResourceAsStream("stocks-history.yaml")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw FileNotFoundException()

        val lastChunk = yamlString.split("---\n").last()
        println(lastChunk)

        return if (lastChunk.isNotBlank()) {
            Yaml.default.decodeFromString(StocksHistory.serializer(), lastChunk)
        } else {
            null
        }
    }

    @Deprecated("Use writeNewStockPositionsToFile instead")
    fun legacyWriteNewStockPositionsToFile(
        isFirstPosition: Boolean,
        resultAmountsBySymbol: Map<String, Int>,
        additionalMoneyToDeposit: Int,
    ) = writeNewStockPositionsToFile(
        isFirstPosition,
        resultAmountsBySymbol.mapKeys { Asset.valueOf(it.key) }.mapValues { it.value.toLong() },
        additionalMoneyToDeposit,
    )

    fun writeNewStockPositionsToFile(
        isFirstPosition: Boolean,
        resultAmountsBySymbol: Map<Asset, Long>,
        additionalMoneyToDeposit: Int,
    ) {
        val prefixDashes = if (isFirstPosition) {
            ""
        } else {
            "---\n"
        }

        val stringToAppendOnFile = prefixDashes +
            """
            date: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}
            additionalDeposit: $additionalMoneyToDeposit
            stocks:
            
            """.trimIndent()
                .plus(
                    resultAmountsBySymbol.entries.joinToString("\n") { (symbol, amount) ->
                        "  $symbol: $amount"
                    },
                )
        println(stringToAppendOnFile)

        Files.write(
            Paths.get("src/main/resources", "stocks-history.yaml"),
            listOf(stringToAppendOnFile),
            StandardOpenOption.APPEND,
        )
    }

    companion object : Loggable
}
