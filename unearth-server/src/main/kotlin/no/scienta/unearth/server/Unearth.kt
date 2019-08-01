/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.server

import ch.qos.logback.classic.LoggerContext
import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.scienta.unearth.core.HandlingPolicy
import no.scienta.unearth.core.storage.InMemoryFaults
import no.scienta.unearth.metrics.MeteringThrowablesSensor
import no.scienta.unearth.munch.model.FrameFun
import no.scienta.unearth.munch.print.*
import no.scienta.unearth.turbo.UnearthlyTurboFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.stream.Stream


object Unearth : () -> Unit {

    private val clock: Clock = Clock.systemDefaultZone();

    private val logger: Logger = LoggerFactory.getLogger(Unearth.javaClass)

    private val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("defaults.properties")

    fun conf(name: String): String = config[Key(name, stringType)]

    private val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    private val storage = InMemoryFaults(sensor, clock)

    override fun invoke() {
        logger.info("Building ${UnearthlyServer::class.simpleName}...")

        val configuration = UnearthlyConfig(
                prefix = config[Key("server.api", stringType)],
                host = config[Key("server.host", stringType)],
                port = config[Key("server.port", intType)],
                selfDiagnose = config[Key("unearth.self-diagnose", booleanType)],
                unearthlyLogging = config[Key("unearth.logging", booleanType)])

        val controller = UnearthlyController(storage, storage, storage, UnearthlyRenderer())

        val server = UnearthlyServer(configuration, controller)

        if (configuration.unearthlyLogging) {
            reconfigureLogging(controller)
        }

        logger.info("Created $server")

        registerShutdown(server)

        server.start {
            logger.info("Ready at http://${callableHost(configuration)}:${configuration.port}")
        }
    }

    private fun reconfigureLogging(controller: UnearthlyController) {
        val squasher: (t: MutableCollection<String>, u: MutableList<CauseFrame>) -> Stream<String> =
                { _, causeFrames ->
                    Stream.of(" * [${causeFrames.size} hidden]")
                }
        val defaultStackRenderer: StackRenderer =
                ConfigurableStackRenderer()
                        .group(SimplePackageGrouper(listOf("org.http4k", "io.netty")))
                        .squash(squasher)
                        .reshape(FrameFun.LIKE_JAVA_8)
                        .reshape(FrameFun.SHORTEN_CLASSNAMES)
        val shortStackRenderer =
                ConfigurableStackRenderer()
                        .group(SimplePackageGrouper(listOf("org.http4k", "io.netty")))
                        .squash(FrameFun.JUST_COUNT_AND_TOP)
                        .reshape(FrameFun.LIKE_JAVA_8)
                        .reshape(FrameFun.SHORTEN_CLASSNAMES)
        val noStackRenderer =
                ConfigurableStackRenderer().noStack()

        (LoggerFactory.getILoggerFactory() as LoggerContext)
                .turboFilterList
                .add(UnearthlyTurboFilter(
                        controller.handler,
                        SimpleCausesRenderer(defaultStackRenderer)
                ).withRendererFor(
                        HandlingPolicy.Action.LOG_MESSAGES,
                        SimpleCausesRenderer(noStackRenderer)
                ).withRendererFor(
                        HandlingPolicy.Action.LOG_SHORT,
                        SimpleCausesRenderer(shortStackRenderer)))
    }

    private fun registerShutdown(server: UnearthlyServer) {
        Runtime.getRuntime().addShutdownHook(Thread({
            server.stop {
                logger.info("Stopped $it")
            }
        }, "Shutdown"))
    }

    private fun callableHost(configuration: UnearthlyConfig): String =
            if (configuration.host == UnearthlyConfig().host) "127.0.0.1" else configuration.host
}
