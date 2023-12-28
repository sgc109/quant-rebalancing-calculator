import kotlinx.coroutines.runBlocking
import portfolio.rebalancer.ReBalancingHelper

// Warning: Modify this with the amount of money you will add
// One of these should be 0
const val ADDITIONAL_MONEY_TO_DEPOSIT = 5500
const val MONEY_TO_WITHDRAW = 0

fun main() = runBlocking {
    val reBalancingHelper = ReBalancingHelper()

    reBalancingHelper.reBalance(
        additionalMoneyToDeposit = ADDITIONAL_MONEY_TO_DEPOSIT,
        moneyToWithdraw = MONEY_TO_WITHDRAW,
    )
}
