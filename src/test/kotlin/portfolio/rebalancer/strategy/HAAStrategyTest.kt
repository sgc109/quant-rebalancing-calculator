package portfolio.rebalancer.strategy

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import portfolio.rebalancer.dto.Asset
import portfolio.rebalancer.dto.SymbolPricesByDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HAAStrategyTest : DescribeSpec({
    val sut = HAAStrategy()

    val baseTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())

    describe("공격자산 100% 매수 케이스(물가 상승세)") {
        context("상승세인 공격자산이 4개 이상인 경우") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0),
                Asset.IEF to listOf(1.0, 2.0, 3.0, 4.0, 8.0),
                Asset.EFA to listOf(1.0, 2.0, 3.0, 4.0, 6.0),
                Asset.EEM to listOf(1.0, 2.0, 3.0, 4.0, 7.0),
                Asset.IWM to listOf(1.0, 2.0, 3.0, 4.0, 9.0),
                Asset.DBC to listOf(1.0, 2.0, 3.0, 4.0, 10.0), // Offensive Universe
                Asset.SPY to listOf(1.0, 2.0, 3.0, 4.0, 11.0), // Offensive Universe
                Asset.TLT to listOf(1.0, 2.0, 3.0, 4.0, 12.0), // Offensive Universe
                Asset.VNQ to listOf(1.0, 2.0, 3.0, 4.0, 13.0), // Offensive Universe
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("가장 상승세가 큰(13612U 모멘텀 스코어가 큰) 4개의 공격자산을 균일하게 매수한다") {
                val targetAssets = setOf(Asset.VNQ, Asset.TLT, Asset.SPY, Asset.DBC)
                res.assetShares.value.keys.toSet() shouldBe targetAssets
                res.targetBuyMoneyPerAsset.keys.shouldContainOnly(targetAssets)
                targetAssets.forEach {
                    res.targetBuyMoneyPerAsset[it] shouldBe budget / 4
                }
            }
        }
    }

    describe("공격자산 + 안전잔산 매수 케이스(물가 상승세)") {
        context("상승세인 공격자산이 3개인 케이스(안전자산 25% 매수)") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 6.0),
                Asset.IEF to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
                Asset.EFA to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EEM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.IWM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.DBC to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.SPY to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.TLT to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
                Asset.VNQ to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("상승세인 공격 자산인 IEF, TLT, VNQ 를 25% 씩, 나머지 25% 는 가장 상승세인 안전자산인 BIL 를 매수한다") {
                val targetAssets = setOf(Asset.TLT, Asset.VNQ, Asset.IEF, Asset.BIL)
                res.assetShares.value.keys.toSet() shouldBe targetAssets
                res.targetBuyMoneyPerAsset.keys.shouldContainOnly(targetAssets)
                res.targetBuyMoneyPerAsset[Asset.TLT] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.VNQ] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.IEF] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.BIL] shouldBe budget * 0.25
            }
        }

        context("상승세인 공격자산이 3개인 케이스(안전자산 25% 매수) - But, IEF 가 매수할 공격자산과 안전자산 모두에 포함된 경우") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0),
                Asset.IEF to listOf(1.0, 2.0, 3.0, 4.0, 6.0), // increasing Offensive Universe
                Asset.EFA to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EEM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.IWM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.DBC to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.SPY to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.TLT to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
                Asset.VNQ to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("상승세인 공격 자산인 IEF, TLT, VNQ 를 25% 씩, 나머지 25% 는 가장 상승세인 안전자산인 IEF 를 매수한다") {
                val targetAssets = setOf(Asset.TLT, Asset.VNQ, Asset.IEF)
                res.assetShares.value.keys.toSet() shouldBe targetAssets
                res.targetBuyMoneyPerAsset.keys.shouldContainOnly(targetAssets)
                res.targetBuyMoneyPerAsset[Asset.TLT] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.VNQ] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.IEF] shouldBe budget * 0.5 // 공격자산에서 25% + 안전자산에서 25%
            }
        }

        context("상승세인 공격자산이 2개인 케이스(안전자산 50% 매수)") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0),
                Asset.IEF to listOf(1.0, 2.0, 3.0, 4.0, 6.0),
                Asset.EFA to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EEM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.IWM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.DBC to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.SPY to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.TLT to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
                Asset.VNQ to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // increasing Offensive Universe
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("상승세인 공격 자산인 TLT, VNQ 를 25% 씩, 나머지 50% 는 가장 상승세인 안전자산인 IEF 을 매수한다") {
                val targetAssets = setOf(Asset.TLT, Asset.VNQ, Asset.IEF)
                res.assetShares.value.keys.toSet() shouldBe targetAssets
                res.targetBuyMoneyPerAsset.keys.shouldContainOnly(targetAssets)
                res.targetBuyMoneyPerAsset[Asset.TLT] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.VNQ] shouldBe budget * 0.25
                res.targetBuyMoneyPerAsset[Asset.IEF] shouldBe budget * 0.5
            }
        }

        context("상승세인 공격자산이 1개인 케이스(안전자산 75% 매수)") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0),
                Asset.IEF to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EFA to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EEM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.IWM to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Increasing Offensive Universe
                Asset.DBC to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.SPY to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.TLT to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.VNQ to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("가장 상승세가 큰 공격자산인 IWM 25% 와 나머지는 가장 상승세가 큰 안전자산 IEF 를 전량 매수한다") {
                res.assetShares.value.keys.toSet() shouldBe setOf(Asset.BIL, Asset.IWM)
                res.targetBuyMoneyPerAsset.keys.shouldContainOnly(Asset.BIL, Asset.IWM)
                res.targetBuyMoneyPerAsset[Asset.BIL] shouldBe budget * 0.75
                res.targetBuyMoneyPerAsset[Asset.IWM] shouldBe budget * 0.25
            }
        }

        context("상승세인 공격자산이 1개인 케이스(안전자산 75% 매수) - But, IEF 가 매수할 공격자산과 안전자산 모두에 포함된 경우") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0),
                Asset.IEF to listOf(1.0, 2.0, 3.0, 4.0, 6.0),
                Asset.EFA to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EEM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.IWM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.DBC to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.SPY to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.TLT to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.VNQ to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("유일한 상승세인 공격자산이자, 가장 상승세가 큰 안전자산인 IEF 를 전량 매수한다") {
                res.assetShares.value.keys.toSet() shouldBe setOf(Asset.IEF)
                res.targetBuyMoneyPerAsset.keys.shouldContainOnly(Asset.IEF)
                res.targetBuyMoneyPerAsset[Asset.IEF] shouldBe budget // 공격자산으로서 25% + 안전자산으로서 75%
            }
        }
    }

    describe("안전자산 100% 매수 케이스") {
        context("물가는 상승세지만, 상승세인 공격자산이 없는 케이스") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0),
                Asset.IEF to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EFA to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.EEM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.IWM to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.DBC to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.SPY to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.TLT to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
                Asset.VNQ to listOf(5.0, 4.0, 3.0, 2.0, 1.0),
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("유일하게 상승세인(13612U 모멘텀 스코어가 0보다 큰) 안전자산 인 BIL 를 전량 매수한다") {
                res.assetShares.value.keys.toSet().shouldContainOnly(Asset.BIL)
                res.targetBuyMoneyPerAsset.keys.toSet().shouldContainOnly(Asset.BIL)
                res.targetBuyMoneyPerAsset[Asset.BIL] shouldBe budget
            }
        }

        context("IEF 전량 매수(물가 하락세)") {
            // given
            val budget = 10000.0
            val pastMonths = listOf(0, 1, 3, 6, 12)
            val pricesPast13612MonthsByAsset = mapOf(
                Asset.TIP to listOf(5.0, 4.0, 3.0, 2.0, 1.0), // Canary Universe
                Asset.BIL to listOf(1.0, 2.0, 3.0, 4.0, 5.0), // Defensive Universe
                Asset.IEF to listOf(1.0, 2.0, 3.0, 4.0, 6.0), // Defensive Universe
                Asset.EFA to listOf(1.0, 2.0, 3.0, 4.0, 7.0),
                Asset.EEM to listOf(1.0, 2.0, 3.0, 4.0, 8.0),
                Asset.IWM to listOf(1.0, 2.0, 3.0, 4.0, 9.0),
                Asset.DBC to listOf(1.0, 2.0, 3.0, 4.0, 10.0),
                Asset.SPY to listOf(1.0, 2.0, 3.0, 4.0, 11.0),
                Asset.TLT to listOf(1.0, 2.0, 3.0, 4.0, 12.0),
                Asset.VNQ to listOf(1.0, 2.0, 3.0, 4.0, 13.0),
            )
            val priceInfo = buildPriceInfo(pricesPast13612MonthsByAsset, baseTime = baseTime, pastMonths = pastMonths)

            // when
            val res = sut.balanceBudget(budget = budget, baseTime = baseTime, pricesInfo = priceInfo)

            // then
            it("가장 상승세가 큰(13612U 모멘텀 스코어가 큰) 안전자산 인 IEF 를 전량 매수한다") {
                res.assetShares.value.keys.toSet().shouldContainOnly(Asset.IEF)
                res.targetBuyMoneyPerAsset.keys.toSet().shouldContainOnly(Asset.IEF)
                res.targetBuyMoneyPerAsset[Asset.IEF] shouldBe budget
            }
        }
    }
})

private fun buildPriceInfo(
    pricesPast13612MonthsByAsset: Map<Asset, List<Double>>,
    baseTime: ZonedDateTime,
    pastMonths: List<Int>,
): SymbolPricesByDate {
    val priceData = mutableMapOf<ZonedDateTime, MutableMap<Asset, Double>>()
    pricesPast13612MonthsByAsset.forEach {
        it.value.forEachIndexed { index, price ->
            val pastMonth = baseTime.minusMonths(pastMonths[pastMonths.size - index - 1].toLong())
            if (!priceData.containsKey(pastMonth)) {
                priceData[pastMonth] = mutableMapOf()
            }
            priceData[pastMonth]!![it.key] = price
        }
    }
    return SymbolPricesByDate(value = priceData)
}
