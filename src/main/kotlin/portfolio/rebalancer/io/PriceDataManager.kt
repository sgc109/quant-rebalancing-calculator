package portfolio.rebalancer.io

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import portfolio.rebalancer.dto.Asset
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PriceDataManager(
    private val baseDir: String = "src/backtests/csv",
) {
    val allPrices = run {
        val symbolToDateToPrice =
            File(baseDir).walk()
                .filter { it.isFile }
                .associate {
                    val dateToPrice = csvReader().readAll(it).drop(1).associate { cols ->
                        val stringDate = cols[0]
                        val price = cols[5]
                        val date = LocalDate.parse(stringDate)
                        date.atTime(16, 0).plusHours(14).atZone(ZoneId.systemDefault()) to price.toDouble()
                    }

                    val symbol = "(.*)\\.csv".toRegex().find(it.name)!!.groupValues[1]

                    Asset.valueOf(symbol) to dateToPrice
                }

        val dateToSymbolToPrice =
            mutableMapOf<ZonedDateTime, MutableMap<Asset, Double>>()

        for ((symbol, innerMap) in symbolToDateToPrice) {
            for ((date, price) in innerMap) {
                dateToSymbolToPrice.getOrPut(date) { mutableMapOf() }[symbol] = price
            }
        }

        dateToSymbolToPrice.mapValues { it.value.toMap() }.toMap()
    }
}
