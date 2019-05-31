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
import link.stuf.exceptions.server.api.*
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
import java.util.*

class WiredExceptionsServer(port: Int) {

    private val submissionLens: BiDiBodyLens<Submission> =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    private val wiredLens: BiDiBodyLens<WiredExceptions> =
            Body.auto<WiredExceptions>(contentNegotiation = ContentNegotiation.None).toLens()

    private val logger = LoggerFactory.getLogger(WiredExceptionsServer::class.java)

    private val storage = InMemoryThrowablesStorage()

    private val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    private val handler: ThrowablesHandler = DefaultThrowablesHandler(storage, storage, sensor, storage)

    private val app = routes(
            "submit" bind Method.POST to {

                applicationJson(submissionLens) {
                    submission(throwableInBody(it))
                }
            },
            "lookup/{uuid}" bind Method.GET to {

                applicationJson(wiredLens) {
                    lookup(ThrowableSpeciesId(pathUuid(it)))
                }
            })

    private fun submission(throwableInBody: Throwable?): Submission {
        val handle = handler.handle(throwableInBody)
        return Submission(handle.speciesId.hash, handle.specimenId.hash)
    }

    private val server = app.asServer(Netty(port))

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    private fun throwableInBody(it: Request) = ThrowableParser.parse(it.body.payload)

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: () -> T): Response =
            response(ContentType.APPLICATION_JSON) { lens.set(ok(), t()) }

    private fun response(type: ContentType, toResponse: () -> Response): Response = try {
        toResponse().header("Content-Type", type.value)
    } catch (e: Exception) {
        logger.error("Failed", e)
        Response(Status.INTERNAL_SERVER_ERROR)
    }

    private fun ok() = Response(Status.OK)

    private fun lookup(id: ThrowableSpeciesId): WiredExceptions {
        val species: ThrowableSpecies = storage.getSpecies(id)
        val specimen: List<ThrowableSpecimen> = storage.getSpecimen(id).toList()
        return WiredExceptions(species.id.hash,
                specimen.toList().map {
                    Occurrence(it.id.hash, it.sequence, it.time, this.wiredEx(it.toThrowable()))
                })
    }

    private fun wiredEx(specimen: Throwable): WiredException = WiredException(
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
}
