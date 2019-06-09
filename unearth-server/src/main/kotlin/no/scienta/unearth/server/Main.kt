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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.server

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.scienta.unearth.core.storage.InMemoryThrowablesStorage
import no.scienta.unearth.micrometer.MeteringThrowablesSensor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {

    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("defaults.properties")

    val serverApi = Key("server.api", stringType)

    val serverHost = Key("server.host", stringType)

    val serverPort = Key("server.port", intType)

    val selfDiagnose = Key("unearth.self-diagnose", booleanType)

    val logger: Logger = LoggerFactory.getLogger("unearth")

    val storage = InMemoryThrowablesStorage()

    val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    val controller = UnearthController(storage, storage, storage, sensor)

    logger.info("Starting ...")

    val server = UnearthServer(
            UnearthConfig(
                    prefix = config[serverApi],
                    host = config[serverHost],
                    port = config[serverPort],
                    selfDiagnose = config[selfDiagnose]),
            controller
    ).start {
        logger.info("Started $it")
    }

    Runtime.getRuntime().addShutdownHook(Thread({
        server.stop {
            logger.info("Stopped $it")
        }
    }, "Shutdown"))
}

