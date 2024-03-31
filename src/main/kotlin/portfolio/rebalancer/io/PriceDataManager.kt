package portfolio.rebalancer.io

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import portfolio.rebalancer.dto.Asset
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
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
                        // 미국 현지 시간 기준 오후 4시(써머타임 적용돼도 현지에선 표기상의 시간 차이가 없으므로)에 14시간 시차보정하여 KST 로 변환
                        Pair(
                            date.atTime(CLOSING_TIME_OF_US_STOCK_MARKET)
                                .atZone(ZoneId.of("America/New_York"))
                                .toInstant()
                                .atZone(ZoneId.of("Asia/Seoul")),
                            price.toDouble()
                        )
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

    companion object {
        val CLOSING_TIME_OF_US_STOCK_MARKET = LocalTime.of(16, 0)
    }
}
