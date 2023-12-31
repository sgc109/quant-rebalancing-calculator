import kotlinx.coroutines.runBlocking
import portfolio.rebalancer.HAAReBalancingHelper
import portfolio.rebalancer.MarketDataClient
import portfolio.rebalancer.StockHistoryFileManager

// Warning: Modify this with the amount of money you will add
// One of these should be 0
const val ADDITIONAL_MONEY_TO_DEPOSIT = 0
const val MONEY_TO_WITHDRAW = 0

fun main() = runBlocking {
    val stockHistoryFileManager = StockHistoryFileManager()
    val marketDataClient = MarketDataClient()
    val reBalancingHelper = HAAReBalancingHelper(stockHistoryFileManager, marketDataClient)

    val res = reBalancingHelper.reBalance(
        additionalMoneyToDeposit = ADDITIONAL_MONEY_TO_DEPOSIT,
        moneyToWithdraw = MONEY_TO_WITHDRAW,
        // dryRun = true, // Uncomment this not to write the result to YAML file and just print it
    )

    res.printResult()
}
