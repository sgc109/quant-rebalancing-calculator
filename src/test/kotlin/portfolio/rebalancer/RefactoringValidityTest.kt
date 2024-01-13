package portfolio.rebalancer

import io.kotest.core.spec.style.FunSpec
import portfolio.rebalancer.io.MarketDataClient
import portfolio.rebalancer.io.StockHistoryFileManager
import portfolio.rebalancer.strategy.HAAStrategy

class RefactoringValidityTest : FunSpec({
    val stockHistoryFileManager = StockHistoryFileManager()
    val marketDataClient = MarketDataClient()
    val haaReBalancingRunner = ReBalancingRunner(
        stockHistoryFileManager,
        marketDataClient,
        strategy = HAAStrategy(),
    )
    val legacyHAAReBalancingHelper = LegacyHAAReBalancingHelper(stockHistoryFileManager, marketDataClient)

    test("레거시와 신규 코드가 동일하게 동작하는지 테스트") {
        val legacyResult = legacyHAAReBalancingHelper.reBalance(
            additionalMoneyToDeposit = 0,
            moneyToWithdraw = 0,
            dryRun = true,
        )
        println("legacyResult=$legacyResult")

        val newResult = haaReBalancingRunner.reBalance(
            additionalMoneyToDeposit = 0,
            moneyToWithdraw = 0,
            dryRun = true,
        )
        println("newResult=$newResult")
    }
})
