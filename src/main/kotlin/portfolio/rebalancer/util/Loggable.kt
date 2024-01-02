package portfolio.rebalancer.util

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

interface Loggable {
    val log: KLogger get() = KotlinLogging.logger(javaClass.name)
}
