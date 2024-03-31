package portfolio.rebalancer

import io.kotest.core.spec.style.FunSpec
import portfolio.rebalancer.dto.SymbolPricesByDate
import portfolio.rebalancer.io.PriceDataManager
import portfolio.rebalancer.strategy.HAAStrategy
import portfolio.rebalancer.util.DrawdownCalculator
import java.time.ZoneId
import java.time.ZonedDateTime

class StrategySimulatorIT : FunSpec({
    val allPrices = PriceDataManager()
    val drawdownCalculator = DrawdownCalculator()
    val strategySimulator = StrategySimulator(drawdownCalculator)

    test("Run strategy simulation") {
        val simulationRes = strategySimulator.simulate(
            budget = 100000.0,
            strategy = HAAStrategy(),
            allTimeSymbolPrices = SymbolPricesByDate(value = allPrices.allPrices),
            startDate = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
            endDate = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
        )

        with(simulationRes) {
            println("CAGR: $cagr")
            println("MDD: $mdd")
            // drawdownsByDate.forEach {
            //     println("[Drawdowns] ${it.first}: ${it.second}%")
            // }
        }
        // simulationRes.timeSeriesData.let {
        //     println("result: date=${it.first().first} ~ ${it.last().first}, budget=${it.first().second} -> ${it.last().second}")
        // }
    }
})
