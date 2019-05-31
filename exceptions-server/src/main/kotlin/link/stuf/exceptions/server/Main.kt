package link.stuf.exceptions.server

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import link.stuf.exceptions.core.storage.InMemoryThrowablesStorage
import link.stuf.exceptions.micrometer.MeteringThrowablesSensor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {

    val logger: Logger = LoggerFactory.getLogger("exceptions")

    logger.info("Starting ...")

    val storage = InMemoryThrowablesStorage()

    val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    val server = WiredExceptionsServer(WiredExceptionsController(storage, storage, storage, sensor));

    server.start()

    logger.info("Started @ ${server.port}")

    Runtime.getRuntime().addShutdownHook(Thread({
        logger.info("Stopping ...")
        server.stop()
        logger.info("Stopped")
    }, "Shutdown"))
}
