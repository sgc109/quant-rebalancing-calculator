import kotlinx.coroutines.runBlocking
import portfolio.rebalancer.ReBalancingRunner
import portfolio.rebalancer.io.MarketDataClient
import portfolio.rebalancer.io.StockHistoryFileManager
import portfolio.rebalancer.strategy.HAAStrategy

// Warning: Modify this with the amount of money you will add
// One of these should be 0
const val ADDITIONAL_MONEY_TO_DEPOSIT = 0
const val MONEY_TO_WITHDRAW = 0

fun main() = runBlocking {
    val stockHistoryFileManager = StockHistoryFileManager()
    val marketDataClient = MarketDataClient()

    val reBalancingRunner = ReBalancingRunner(
        stockHistoryFileManager,
        marketDataClient,
        strategy = HAAStrategy(), // HAA 전략을 사용
    )

    val res = reBalancingRunner.reBalance(
        additionalMoneyToDeposit = ADDITIONAL_MONEY_TO_DEPOSIT,
        moneyToWithdraw = MONEY_TO_WITHDRAW,
        dryRun = true, // Uncomment this not to write the result to YAML file and just print it
    )

    // Uncomment this when you want to use legacy VAA strategy(It will eventually be deprecated when new HAAStrategy is stable
    // val res = LegacyHAAReBalancingHelper(
    //     stockHistoryFileManager,
    //     marketDataClient,
    // ).reBalance(
    //     additionalMoneyToDeposit = ADDITIONAL_MONEY_TO_DEPOSIT,
    //     moneyToWithdraw = MONEY_TO_WITHDRAW,
    //     dryRun = true,
    // )

    res.printResult()
}
