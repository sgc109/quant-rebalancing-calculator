package portfolio.rebalancer

import com.charleskorn.kaml.Yaml
import portfolio.rebalancer.dto.StocksHistory
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

    fun writeNewStockPositionsToFile(
        isFirstPosition: Boolean,
        resultAmountsBySymbol: Map<String, Int>,
    ) {
        val prefixDashes = if (isFirstPosition) {
            ""
        } else {
            "---\n"
        }

        val stringToAppendOnFile = prefixDashes +
            """
            date: ${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}
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
}
