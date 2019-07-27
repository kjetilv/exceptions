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
import no.scienta.unearth.core.storage.InMemoryFaults
import no.scienta.unearth.metrics.MeteringThrowablesSensor
import no.scienta.unearth.turbo.UnearthlyTurboFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory


object Unearth : () -> Unit {

    private val logger: Logger = LoggerFactory.getLogger(Unearth.javaClass)

    private val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("defaults.properties")

    fun conf(name: String): String = config[Key(name, stringType)]

    private val storage = InMemoryFaults()

    private val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    override fun invoke() {
        logger.info("Building ${UnearthlyServer::class.simpleName}...")

        val configuration = UnearthlyConfig(
                prefix = config[Key("server.api", stringType)],
                host = config[Key("server.host", stringType)],
                port = config[Key("server.port", intType)],
                selfDiagnose = config[Key("unearth.self-diagnose", booleanType)],
                unearthlyLogging = config[Key("unearth.logging", booleanType)])

        val unearthlyController = UnearthlyController(storage, storage, storage, sensor)
        val unearthServer = UnearthlyServer(configuration, unearthlyController)

        if (configuration.unearthlyLogging) {
            reconfigureLogging(unearthlyController)
        }

        logger.info("Created $unearthServer")

        registerShutdown(unearthServer)

        unearthServer.start {
            logger.info("Ready at http://${callableHost(configuration)}:${configuration.port}")
        }
    }

    private fun reconfigureLogging(controller: UnearthlyController) {
        (LoggerFactory.getILoggerFactory() as LoggerContext)
                .turboFilterList
                .add(UnearthlyTurboFilter(controller.handler, controller.serverRenderer))
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
