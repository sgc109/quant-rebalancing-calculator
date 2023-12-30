import kotlinx.coroutines.runBlocking
import portfolio.rebalancer.MarketDataClient
import portfolio.rebalancer.StockHistoryFileManager
import portfolio.rebalancer.VAAReBalancingHelper

// Warning: Modify this with the amount of money you will add
// One of these should be 0
const val ADDITIONAL_MONEY_TO_DEPOSIT = 0
const val MONEY_TO_WITHDRAW = 0

fun main() = runBlocking {
    val stockHistoryFileManager = StockHistoryFileManager()
    val marketDataClient = MarketDataClient()
    val reBalancingHelper = VAAReBalancingHelper(stockHistoryFileManager, marketDataClient)

    val res = reBalancingHelper.reBalance(
        additionalMoneyToDeposit = ADDITIONAL_MONEY_TO_DEPOSIT,
        moneyToWithdraw = MONEY_TO_WITHDRAW,
    )

    res.printResult()
}
