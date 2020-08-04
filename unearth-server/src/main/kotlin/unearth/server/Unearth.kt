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

package unearth.server

import ch.qos.logback.classic.LoggerContext
import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import unearth.analysis.CassandraInit
import unearth.analysis.CassandraSensor
import unearth.core.FaultSensor
import unearth.core.HandlingPolicy
import unearth.jdbc.JdbcStorage
import unearth.memory.Db
import unearth.memory.Sensor
import unearth.munch.model.FrameFun
import unearth.munch.print.*
import unearth.server.turbo.UnearthlyTurboFilter
import java.net.URI
import java.time.Clock
import java.util.*
import java.util.stream.Stream
import javax.sql.DataSource

class Unearth(private val customConfiguration: UnearthlyConfig? = null) {

    interface State {

        fun url(): URI

        fun port(): Int

        fun reset()

        fun close()
    }

    fun run(toServer: (UnearthlyController, UnearthlyConfig) -> UnearthlyServer): State {
        logger.info("Building ${Unearth::class.simpleName}...")

        val configuration = customConfiguration ?: unearthlyConfig(loadConfiguration())

        val db: DataSource = db(configuration)
        val sensor: FaultSensor = sensor(configuration)

        val storage = JdbcStorage(db, configuration.db.schema, Clock.systemDefaultZone())

        val controller = UnearthlyController(
            storage,
            storage,
            storage,
            sensor,
            UnearthlyRenderer(),
            configuration
        )

        val server: UnearthlyServer = toServer(controller, configuration)

        if (configuration.unearthlyLogging) {
            reconfigureLogging(controller)
        }

        logger.info("Created $server")

        registerShutdown(server)

        server.start {
            logger.info("$it ready at http://${configuration.host}:${server.port()}")
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
                        (if (configuration.prefix.endsWith("/")) "" else "/")
            )

            override fun port(): Int = server.port()
        }
    }

    private fun sensor(configuration: UnearthlyConfig): FaultSensor {
        if (configuration.unearthlyMemory) {
            return Sensor.memory()
        }

        CassandraInit(
            configuration.cassandra.host,
            configuration.cassandra.port,
            configuration.cassandra.dc,
            configuration.cassandra.keyspace
        ).init()

        return CassandraSensor(
            configuration.cassandra.host,
            configuration.cassandra.port,
            configuration.cassandra.dc,
            configuration.cassandra.keyspace
        )
    }

    private fun db(configuration: UnearthlyConfig): DataSource =
        if (configuration.unearthlyMemory) {
            Db.memory()
        } else {
            HikariDataSource(HikariConfig(Properties().apply {
                setProperty("jdbcUrl", configuration.db.jdbc)
                setProperty("username", configuration.db.username)
                setProperty("password", configuration.db.password)
            }))
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
            .add(
                UnearthlyTurboFilter(
                    controller.handler,
                    SimpleCausesRenderer(defaultStackRenderer)
                ).withRendererFor(
                    HandlingPolicy.Action.LOG_MESSAGES,
                    SimpleCausesRenderer(noStackRenderer)
                ).withRendererFor(
                    HandlingPolicy.Action.LOG_SHORT,
                    SimpleCausesRenderer(shortStackRenderer)
                )
            )
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

        private fun unearthlyConfig(config: Configuration): UnearthlyConfig =
            UnearthlyConfig(
                prefix = config[Key("server.api", stringType)],
                host = config[Key("server.host", stringType)],
                port = config[Key("server.port", intType)],
                selfDiagnose = config[Key("unearth.self-diagnose", booleanType)],
                unearthlyLogging = config[Key("unearth.logging", booleanType)],
                unearthlyMemory = config[Key("unearth.memory", booleanType)],
                cassandra = UnearthlyCassandraConfig(
                    host = config[Key("unearth.cassandra-host", stringType)],
                    port = config[Key("unearth.cassandra-port", intType)],
                    dc = config[Key("unearth.cassandra-dc", stringType)],
                    keyspace = config[Key("unearth.cassandra-keyspace", stringType)]
                ),
                db = UnearthlyDbConfig(
                    host = config[Key("unearth.db-host", stringType)],
                    port = config[Key("unearth.db-port", intType)],
                    username = config[Key("unearth.db-username", stringType)],
                    password = config[Key("unearth.db-password", stringType)],
                    schema = config[Key("unearth.db-schema", stringType)]
                )
            )

        private fun loadConfiguration(): Configuration {
            return systemProperties() overriding
                    EnvironmentVariables() overriding
                    ConfigurationProperties.fromResource("defaults.properties")
        }
    }
}

