package portfolio.rebalancer.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DrawdownCalculatorTest : DescribeSpec({
    val sut = DrawdownCalculator()

    describe("가격이 오른 경우") {
        context("마지막 최고가를 갱신한 경우") {
            // given
            val prices = listOf(10.0, 20.0)

            // when
            val res = sut.calculateDrawdowns(prices = prices)

            // then
            it("반환된 리스트의 크기는 입력 리스트보다 1 작아야 한다") {
                res.size shouldBe prices.size - 1
            }
            it("drawdown 은 0 이어야한다") {
                res shouldBe listOf(0.0)
            }
        }

        context("가격이 2회 연속 내린 경우") {
            // given
            val prices = listOf(10.0, 8.0, 5.0)

            // when
            val res = sut.calculateDrawdowns(prices = prices)

            // then
            it("마지막 최고점을 기준으로 하락율을 계산한다") {
                // res shouldBe listOf(-20.0, -50.0)
                res[0] shouldBe -20.0
            }
        }

        context("가격이 내렸다 최고점 갱신 후 다시 내린 경우") {
            // given
            val prices = listOf(10.0, 5.0, 20.0, 5.0)

            // when
            val res = sut.calculateDrawdowns(prices = prices)

            // then
            it("적절한 값들을 내려주어야 한다") {
                res shouldBe listOf(-50.0, 0.0, -75.0)
            }
        }

        context("가격이 내렸다 최고점 미갱신 후 다시 내린 경우") {
            // given
            val prices = listOf(20.0, 5.0, 10.0, 5.0)

            // when
            val res = sut.calculateDrawdowns(prices = prices)

            // then
            it("마지막 최고가를 기준으로 하락율을 계산해야 한다") {
                res shouldBe listOf(-75.0, -50.0, -75.0)
            }
        }
    }
})
