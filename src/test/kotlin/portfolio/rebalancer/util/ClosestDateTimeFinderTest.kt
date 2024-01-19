package portfolio.rebalancer.util

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import java.time.ZonedDateTime

class ClosestDateTimeFinderTest : DescribeSpec({
    val baseTime = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())

    describe("정확히 일치하는 날짜가 존재하는 경우") {
        context("그 날짜만 있는 경우") {
            // given

            // when
            val res = ClosestDateTimeFinder.findClosestDate(baseTime = baseTime, dates = listOf(baseTime))

            // then
            it("그 날짜가 조회되어야 한다") {
                res shouldBe baseTime
            }
        }

        context("다른 날짜도 있는 경우") {
            // given
            val beforeOneDay = baseTime.minusDays(1)

            // when
            val res = ClosestDateTimeFinder.findClosestDate(baseTime = baseTime, dates = listOf(beforeOneDay, baseTime))

            // then
            it("그 날짜가 조회되어야 한다") {
                res shouldBe baseTime
            }
        }

        context("다른 날짜들도 있는 경우") {
            // given
            val beforeOneDay = baseTime.minusDays(1)
            val afterOneDay = baseTime.plusDays(1)

            // when
            val res = ClosestDateTimeFinder.findClosestDate(
                baseTime = baseTime,
                dates = listOf(beforeOneDay, baseTime, afterOneDay),
            )

            // then
            it("그 날짜가 조회되어야 한다") {
                res shouldBe baseTime
            }
        }

        context("날짜들이 정렬되어있지 않은 경우에도") {
            // given
            val days = (1L..10L).map { baseTime.minusDays(it) } + baseTime

            // when
            val res = ClosestDateTimeFinder.findClosestDate(baseTime = baseTime, dates = days)

            it("해당 날짜를 찾을 수 있어야 한다") {
                res shouldBe baseTime
            }
        }
    }

    describe("정확히 일치하는 날짜는 없지만, 4일 이하로 차이나는 가까운 날짜가 있는 경우") {
        context("4일 이전 날짜가 있는 경우") {
            // given
            val beforeFourDays = baseTime.minusDays(4)

            // when
            val res = ClosestDateTimeFinder.findClosestDate(baseTime = baseTime, dates = listOf(beforeFourDays))

            // then
            it("4일 이전 날짜가 조회되어야 한다") {
                res shouldBe beforeFourDays
            }
        }

        context("4일 이후 날짜가 있는 경우") {
            // given
            val afterFourDays = baseTime.plusDays(4)

            // when
            val res = ClosestDateTimeFinder.findClosestDate(baseTime = baseTime, dates = listOf(afterFourDays))

            // then
            it("4일 이후 날짜가 조회되어야 한다") {
                res shouldBe afterFourDays
            }
        }

        context("4일 이전과 4일 이후 날짜가 있는 경우") {
            // given
            val beforeFourDays = baseTime.minusDays(4)
            val afterFourDays = baseTime.plusDays(4)

            // when
            val res = ClosestDateTimeFinder.findClosestDate(
                baseTime = baseTime,
                dates = listOf(beforeFourDays, afterFourDays),
            )

            // then
            it("4일 이전 날짜가 조회되어야 한다") {
                res shouldBe beforeFourDays
            }
        }
    }

    describe("정확히 일치하는 날짜도 없고, 가까운 날짜도 없는 경우") {
        context("가까운 날짜조차 없는 경우") {
            // given
            val beforeFiveDays = baseTime.minusDays(5)
            val afterFiveDays = baseTime.plusDays(5)

            // when, then
            shouldThrowExactly<NoSuchElementException> {
                ClosestDateTimeFinder.findClosestDate(
                    baseTime = baseTime,
                    dates = listOf(beforeFiveDays, afterFiveDays),
                )
            }
        }
    }
})
