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

    val serverHost = Key("server.host", stringType)

    val serverPort = Key("server.port", intType)

    val selfDiagnose= Key("exceptions.self-diagnose", booleanType)

    val logger: Logger = LoggerFactory.getLogger("exceptions")

    logger.info("Starting ...")

    val storage = InMemoryThrowablesStorage()

    val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    val controller = WiredExceptionsController(storage, storage, storage, sensor)

    val server = WiredExceptionsServer(
            controller,
            SwaggerJson,
            host = config[serverHost],
            port = config[serverPort],
            selfDiagnose = config[selfDiagnose]).start()

    logger.info("Started @ ${server.port}")

    Runtime.getRuntime().addShutdownHook(Thread({
        logger.info("Stopped: {}", server.stop())
    }, "Shutdown"))
}

