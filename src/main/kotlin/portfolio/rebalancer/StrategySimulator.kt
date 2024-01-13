package portfolio.rebalancer

import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.AssetShares
import portfolio.rebalancer.dto.SymbolPricesByDate
import portfolio.rebalancer.strategy.Strategy
import java.time.LocalDate
import java.time.ZonedDateTime

class StrategySimulator {
    /**
     * allTimePricesBySymbol 에서 전략에서 사용되는 모든 자산의 가격이 존재하는 날짜들을 뽑고,
     * 리밸런싱 주기마다 월 말에서 최대한 가까운, 가격이 존재하는 과거 날짜에 리밸런싱을 한다.
     * 만약 그 날짜가 원래 해야하는 날짜와 리밸런싱 주기의 4일 초과하여 차이가 나는 경우가 한 번이라도 있다면 거기서 중단한다.
     * (과거 9.11 테러 때가 가장 긴 NASDAQ 휴장 기간으로 4일이었다고 함)
     *
     * TODO: 거래 수수료, SEC fees, 세금(양도세), 슬리피지 등도 고려해야 정확도가 올라감
     *
     * @param allTimeSymbolPrices: 날짜 별 자산 별 가격 데이터
     */
    fun simulate(
        budget: Double,
        strategy: Strategy,
        allTimeSymbolPrices: SymbolPricesByDate,
        startDate: ZonedDateTime,
        endDate: ZonedDateTime = ZonedDateTime.now(),
    ): Result {
        // TODO: 시작 날짜부터 끝 날짜까지 해당 전략의 리밸런싱 주기마다 리밸런싱하면서(만약 해당 날짜에 가격이 없다면 과거 최근 가격 기준으로)
        // 최종 자산을 도출해내고, 이를 바탕으로 값을 반환한다.
        val commonDates =
            allTimeSymbolPrices.value.keys.sorted().filter { date ->
                strategy.allAssets.all { symbol: Asset ->
                    allTimeSymbolPrices[date]?.containsKey(symbol) ?: false
                }
            }.let { dates ->
                dates.dropWhile { it < startDate }.dropLastWhile { endDate < it }
            }

        if (commonDates.isEmpty()) {
            throw IllegalArgumentException("There is no common price date between all symbols used in given strategy")
        }

        println("Start simulating from ${commonDates.first()} with $budget USD")

        // TODO: 시작 날짜 찾아서 주기 별로 시뮬레이션 하는 코드 필요(해당 주기에서 가장 가까운 과거 날짜 찾는 부분 중요)
        var nextDate = commonDates.first().plusYears(1)
        val datesToReBalance = mutableSetOf<ZonedDateTime>()
        while (true) {
            try {
                ClosestDateTimeFinder.findClosestDate(nextDate, commonDates)
            } catch (e: Exception) {
                break
            }.let {
                datesToReBalance.add(it)
            }
            nextDate += strategy.rebalancingPeriod
        }
        println("datesToReBalance=$datesToReBalance")

        var assetShares = AssetShares(value = emptyMap())
        var unusedMoney = budget

        val results = mutableListOf<Pair<ZonedDateTime, Double>>()
        commonDates.forEach { nowTime ->
            val estimatedTotalValue = estimateCurrentTotalValue(assetShares, nowTime, allTimeSymbolPrices) + unusedMoney
            results.add(nowTime to estimatedTotalValue)

            if (datesToReBalance.contains(nowTime)) {
                strategy.balanceBudget(
                    budget = estimatedTotalValue,
                    baseTime = nowTime,
                    allTimeSymbolPrices,
                ).let {
                    assetShares = it.assetShares
                    unusedMoney = it.unusedMoney
                }
            }
        }

        return Result(
            cagr = 0.0,
            drawdownsByDate = emptyList(),
            timeSeriesData = results,
        )
    }

    private fun estimateCurrentTotalValue(
        assetShares: AssetShares,
        baseTime: ZonedDateTime,
        allTimePrices: SymbolPricesByDate,
    ): Double {
        return assetShares.value.entries.sumOf { (symbol, amount) ->
            allTimePrices[baseTime]?.get(symbol)!! * amount
        }
    }

    fun LocalDate.getLastDayOfMonth(): Int {
        return this.plusMonths(1).minusDays(1).dayOfMonth
    }

    data class Result(
        val cagr: Double,
        val drawdownsByDate: List<Pair<ZonedDateTime, Double>>,
        val timeSeriesData: List<Pair<ZonedDateTime, Double>>,
    ) {
        // val mdd: Double = drawdownsByDate.minOf { it.second }
    }
}
