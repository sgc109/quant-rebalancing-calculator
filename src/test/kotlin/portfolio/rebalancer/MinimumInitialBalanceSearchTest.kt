package portfolio.rebalancer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MinimumInitialBalanceSearchTest : FunSpec({
    val stockHistoryFileManager = StockHistoryFileManager()
    val marketDataClient = MarketDataClient()
    val reBalancingHelper = HAAReBalancingHelper(stockHistoryFileManager, marketDataClient)

    test("모든 자산 분배 비율에 미사용 금액의 비율이 10% 미만인 최소 추가 투입 비용 찾기") {
        var found = false

        // 필요에 따라 수정
        val start = 0
        val end = 20000
        val step = 100
        val unusedPercentLimit = 10

        for (additionalMoney in start..end step step) {
            if (additionalMoney % 1000 == 0) {
                println("[progress] additionalMoney: $additionalMoney")
            }

            val res =
                reBalancingHelper.reBalance(
                    additionalMoneyToDeposit = additionalMoney,
                    moneyToWithdraw = 0,
                    dryRun = true,
                )

            res.printResult()

            if (res.isAllAccurateUnderPercent(unusedPercentLimit)) {
                println("additionalMoney: $additionalMoney")
                found = true
                break
            }
        }

        found shouldBe true
    }

    test("모든 자산 분배 비율에 미사용 금액의 비율이 10% 미만인 최소 인출 비용 찾기") {
        var found = false

        // 필요에 따라 수정
        val start = 0
        val end = 20000
        val step = 100
        val unusedPercentLimit = 5

        for (withdrawalAmount in start..end step step) {
            if (withdrawalAmount % 1000 == 0) {
                println("[progress] withdrawalAmount: $withdrawalAmount")
            }

            val res =
                reBalancingHelper.reBalance(
                    additionalMoneyToDeposit = 0,
                    moneyToWithdraw = withdrawalAmount,
                    dryRun = true,
                )

            res.printResult()

            if (res.isAllAccurateUnderPercent(unusedPercentLimit)) {
                println("withdrawalAmount: $withdrawalAmount")
                found = true
                break
            }
        }

        found shouldBe true
    }
})
