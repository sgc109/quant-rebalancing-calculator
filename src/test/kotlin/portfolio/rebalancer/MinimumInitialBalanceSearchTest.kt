package portfolio.rebalancer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MinimumInitialBalanceSearchTest : FunSpec({
    val stockHistoryFileManager = StockHistoryFileManager()
    val marketDataClient = MarketDataClient()
    val reBalancingHelper = VAAReBalancingHelper(stockHistoryFileManager, marketDataClient)

    test("모든 자산 분배 비율에 미사용 금액의 비율이 5% 미만인 초기 투입 비용 찾기") {
        var found = false

        // 필요에 따라 수정
        val start = 10000
        val end = 20000
        val step = 100
        val unusedPercentLimit = 10

        for (totalBalance in start..end step step) {
            if (totalBalance % 1000 == 0) {
                println("[progress] totalBalance: $totalBalance")
            }

            val res =
                reBalancingHelper.reBalance(additionalMoneyToDeposit = totalBalance, moneyToWithdraw = 0, dryRun = true)

            res.printResult()

            if (res.isAllAccurateUnderPercent(unusedPercentLimit)) {
                println("totalBalance: $totalBalance")
                found = true
                break
            }
        }

        found shouldBe true
    }
})
