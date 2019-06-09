package link.stuf.exceptions.server

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import link.stuf.exceptions.core.storage.InMemoryThrowablesStorage
import link.stuf.exceptions.micrometer.MeteringThrowablesSensor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {

    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("defaults.properties")

    val serverApi = Key("server.api", stringType)

    val serverHost = Key("server.host", stringType)

    val serverPort = Key("server.port", intType)

    val selfDiagnose = Key("exceptions.self-diagnose", booleanType)

    val logger: Logger = LoggerFactory.getLogger("exceptions")

    val storage = InMemoryThrowablesStorage()

    val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    val controller = WiredExceptionsController(storage, storage, storage, sensor)

    logger.info("Starting ...")

    val server = ContractualObligationServer(
            ServerConfiguration(
                    prefix = config[serverApi],
                    host = config[serverHost],
                    port = config[serverPort],
                    selfDiagnose = config[selfDiagnose]),
            controller
    ).start { srv ->
        logger.info("Started @ {}", srv)
    }

    Runtime.getRuntime().addShutdownHook(Thread({
        server.stop { srv ->
            logger.info("Stopped @ {}", srv) }
    }, "Shutdown"))
}

