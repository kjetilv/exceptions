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
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import unearth.analysis.CassandraInit
import unearth.analysis.CassandraSensor
import unearth.core.FaultSensor
import unearth.core.HandlingPolicy
import unearth.jdbc.Metrics
import unearth.memory.Db
import unearth.memory.Sensor
import unearth.metrics.MetricsFactory
import unearth.munch.model.FrameFun
import unearth.munch.print.*
import unearth.server.turbo.UnearthlyTurboFilter
import unearth.storage.JdbcStorage
import java.net.URI
import java.time.Clock
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction
import java.util.stream.Stream
import javax.sql.DataSource

class Unearth(private val configuration: UnearthlyConfig = UnearthlyConfig.load()) {

    companion object {

        private val logger = LoggerFactory.getLogger(Unearth::class.java)
    }

    fun startJavaServer(
        metricsFactory: MetricsFactory,
        toServer: BiFunction<UnearthlyResources, UnearthlyConfig, UnearthlyServer>
    ): State =
        startServer(metricsFactory) { controller, config ->
            toServer.apply(controller, config)
        }

    fun startServer(
        metricsFactory: MetricsFactory,
        toServer: (UnearthlyResources, UnearthlyConfig) -> UnearthlyServer
    ): State {
        logger.info("Building ${Unearth::class.simpleName}...")

        val sensorFuture = CompletableFuture.supplyAsync { sensor() }
        val storageFuture = CompletableFuture.supplyAsync { storage(metricsFactory) }

        val storage = storageFuture.join()
        val sensor = sensorFuture.join()

        val renderer = UnearthlyRenderer(configuration.prefix)
        val resources = UnearthlyController(storage, storage, storage, sensor, renderer)

        val server: UnearthlyServer = toServer(resources, configuration)

        if (configuration.unearthlyLogging) {
            reconfigureLogging(resources)
        }

        Runtime.getRuntime().addShutdownHook(Thread({ server.stop() }, "Shutdown"))

        server.start()



        return object : State {
            override fun url(): URI = configuration.connectUri
            override fun port(): Int = configuration.port
            override fun reset() = resources.reset()
            override fun close() = server.stop()
        }
    }

    private fun storage(metricsFactory: MetricsFactory): JdbcStorage {
        val db: DataSource = db(configuration)
        val storage = JdbcStorage(
            db,
            configuration.db.schema,
            Clock.systemDefaultZone(),
            metricsFactory.instantiate(Metrics::class.java)
        )
        initStorage(storage)
        return storage
    }

    private fun initStorage(storage: JdbcStorage) =
        try {
            storage.initStorage().run()
        } catch (e: Exception) {
            throw IllegalStateException("$this failed to init $storage", e)
        }

    private fun sensor(): FaultSensor =
        if (configuration.unearthlyMemory) Sensor.memory()
        else configuration.cassandra.run {
            CassandraInit(host, port, dc, keyspace).init()
            return CassandraSensor(host, port, dc, keyspace)
        }

    private fun db(configuration: UnearthlyConfig): DataSource =
        if (configuration.unearthlyMemory) Db.memory()
        else HikariDataSource(HikariConfig(Properties().apply {
            setProperty("jdbcUrl", configuration.db.jdbc)
            setProperty("username", configuration.db.username)
            setProperty("password", configuration.db.password)
        }))

    private fun reconfigureLogging(resources: UnearthlyResources) {
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
                    resources,
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
}

