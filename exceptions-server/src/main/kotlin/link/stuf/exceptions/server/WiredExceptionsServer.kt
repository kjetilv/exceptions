package link.stuf.exceptions.server

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import link.stuf.exceptions.core.NamedException
import link.stuf.exceptions.core.ThrowablesHandler
import link.stuf.exceptions.core.handler.DefaultThrowablesHandler
import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.core.storage.InMemoryThrowablesStorage
import link.stuf.exceptions.core.throwables.ThrowableSpecies
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId
import link.stuf.exceptions.core.throwables.ThrowableSpecimen
import link.stuf.exceptions.micrometer.MeteringThrowablesSensor
import link.stuf.exceptions.server.api.Occurrence
import link.stuf.exceptions.server.api.WiredException
import link.stuf.exceptions.server.api.WiredExceptions
import link.stuf.exceptions.server.api.WiredStackTraceElement
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*

class WiredExceptionsServer(port: Int) {

    private val wireLens: BiDiBodyLens<WiredExceptions> =
            Body.auto<WiredExceptions>(contentNegotiation = ContentNegotiation.None).toLens()

    private val logger = LoggerFactory.getLogger(WiredExceptionsServer::class.java)

    private val app = routes(
            "submit" bind Method.POST to {
                textPlain(
                        handleSubmission(it.body.payload))
            },
            "lookup/{uuid}" bind Method.GET to {
                applicationJson(
                        wireLens,
                        lookup(ThrowableSpeciesId(uuidPath(it))))
            }
    )

    private fun uuidPath(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun lookup(id: ThrowableSpeciesId): WiredExceptions {
        val species: ThrowableSpecies = storage.getSpecies(id)
        val specimen: List<ThrowableSpecimen> = storage.getSpecimen(id).toList()
        return WiredExceptions(species.id.hash, specimen.toList()
                .map {
                    Occurrence(
                            it.id.hash,
                            it.sequence,
                            it.time,
                            this.wiredEx(it.toThrowable()))
                })
    }

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: T): Response {
        return try {
            lens.set(Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.value), t)
        } catch (e: Exception) {
            logger.error("Failed", e)
            Response(Status.INTERNAL_SERVER_ERROR)
        }
    }

    private fun <T> textPlain(t: T): Response {
        return try {
            Response(Status.OK).header("Content-Type", ContentType.TEXT_PLAIN.value).body(t.toString())
        } catch (e: Exception) {
            logger.error("Failed", e)
            Response(Status.INTERNAL_SERVER_ERROR)
        }
    }

    private val storage = InMemoryThrowablesStorage()

    private val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    private val handler: ThrowablesHandler = DefaultThrowablesHandler(storage, storage, sensor, storage)

    private val server = app.asServer(Netty(port)).start()

    private fun handleSubmission(payload: ByteBuffer): UUID {
        val parsed = ThrowableParser.parse(payload)
        val handling = handler.handle(parsed)
        return handling.id
    }

    private fun wiredEx(specimen: Throwable): WiredException =
            WiredException(
                    className = (if (specimen is NamedException)
                        (specimen as NamedException).proxiedClassName
                    else
                        specimen.javaClass.name),
                    message = specimen.message,
                    stacktrace = specimen.stackTrace.let(this::wiredStackTrace),
                    cause = specimen.cause?.let(this::wiredEx))

    private fun wiredStackTrace(stackTrace: Array<StackTraceElement>): List<WiredStackTraceElement>? =
            stackTrace.map { element ->
                WiredStackTraceElement(
                        classLoaderName = element.classLoaderName,
                        moduleName = element.moduleName,
                        moduleVersion = element.moduleVersion,
                        declaringClass = element.className,
                        methodName = element.methodName,
                        fileName = element.fileName,
                        lineNumber = element.lineNumber)
            }.toList()

    fun stop() {
        server.stop()
    }
}
