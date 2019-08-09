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
import no.scienta.unearth.analysis.CassandraInit
import no.scienta.unearth.analysis.CassandraSensor
import no.scienta.unearth.core.HandlingPolicy
import no.scienta.unearth.core.poc.InMemoryFaults
import no.scienta.unearth.munch.model.FrameFun
import no.scienta.unearth.munch.print.*
import no.scienta.unearth.turbo.UnearthlyTurboFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Clock
import java.util.stream.Stream

class Unearth(private val customConfiguration: UnearthlyConfig? = null) : () -> Unearth.State {

    private val clock: Clock = Clock.systemDefaultZone()

    interface State {

        fun url(): URI

        fun port(): Int

        fun reset()

        fun close()
    }

    override fun invoke(): State {
        logger.info("Building ${UnearthlyServer::class.simpleName}...")

        val configuration = customConfiguration ?: loadUnearthlyConfig()

        val cassandraConfig = configuration.cassandra

        CassandraInit(cassandraConfig.host,
                cassandraConfig.port,
                cassandraConfig.dc,
                cassandraConfig.keyspace).init()

        val sensor = CassandraSensor(
                cassandraConfig.host,
                cassandraConfig.port,
                cassandraConfig.dc,
                cassandraConfig.keyspace)

        val storage = InMemoryFaults(sensor, clock)

        val controller = UnearthlyController(
                storage,
                storage,
                storage,
                UnearthlyRenderer(),
                configuration)

        val server = UnearthlyServer(controller, configuration)

        if (configuration.unearthlyLogging) {
            reconfigureLogging(controller)
        }

        logger.info("Created $server")

        registerShutdown(server)

        server.start {
            logger.info("Ready at http://${configuration.host}:${server.port()}")
        }

        return object : State {
            override fun close() {
                server.stop {
                    logger.debug("Stopped by demand: $it")
                }
            }

            override fun reset() {
                server.reset()
            }

            override fun url(): URI = URI.create(
                    "http://" + configuration.host + ":" + server.port() +
                            configuration.prefix +
                            (if (configuration.prefix.endsWith("/")) "" else "/"))

            override fun port(): Int = server.port()
        }
    }

    private fun loadUnearthlyConfig(): UnearthlyConfig {
        val config = loadConfiguration()
        return UnearthlyConfig(
                prefix = config[Key("server.api", stringType)],
                host = config[Key("server.host", stringType)],
                port = config[Key("server.port", intType)],
                selfDiagnose = config[Key("unearth.self-diagnose", booleanType)],
                unearthlyLogging = config[Key("unearth.logging", booleanType)],
                cassandra = UnearthlyCassandraConfig(
                        host = config[Key("unearth.cassandra-host", stringType)],
                        port = config[Key("unearth.cassandra-port", intType)],
                        dc = config[Key("unearth.cassandra-dc", stringType)],
                        keyspace = config[Key("unearth.cassandra-keyspace", stringType)]))
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
                logger.info("Stopped at shutdown: $it")
            }
        }, "Shutdown"))
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(Unearth::class.java)

        private fun loadConfiguration(): Configuration {
            return systemProperties() overriding
                    EnvironmentVariables() overriding
                    ConfigurationProperties.fromResource("defaults.properties")
        }
    }
}
