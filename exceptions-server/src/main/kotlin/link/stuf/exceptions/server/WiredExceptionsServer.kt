package link.stuf.exceptions.server

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import link.stuf.exceptions.api.NamedException
import link.stuf.exceptions.api.ThrowablesHandler
import link.stuf.exceptions.core.handler.DefaultThrowablesHandler
import link.stuf.exceptions.core.inputs.ThrowableParser
import link.stuf.exceptions.core.storage.InMemoryThrowablesStorage
import link.stuf.exceptions.micrometer.MeteringThrowablesSensor
import link.stuf.exceptions.server.api.WiredException
import link.stuf.exceptions.server.api.WiredStackTraceElement
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*

class WiredExceptionsServer(port: Int) {

    private val lens: BiDiBodyLens<WiredException> = Body.auto<WiredException>().toLens()

    private val logger = LoggerFactory.getLogger(WiredExceptionsServer::class.java)

    private val app = routes(
            "submit" bind Method.POST to httpWrapped { req: Request -> submit(uuid(req.body.payload)) },
            "echo" bind Method.POST to httpWrapped(this::echo),
            "lookup/{uuid}" bind Method.GET to httpWrapped(this::lookup, lens)
    )

    private fun submit(uuid: UUID): String = uuid.toString()

    private fun echo(req: Request): String = verify(req.body.payload)

    private fun lookup(req: Request): WiredException {
        return req.path("uuid")?.let(UUID::fromString)
                .let(handler::lookup)
                .let(this::wiredEx)!!
    }

    private fun httpWrapped(fn: (Request) -> String): (Request) -> Response {
        return { req: Request ->
            try {
                Response(Status.OK).body(fn(req))
            } catch (e: Exception) {
                logger.error("Failed", e)
                Response(Status.INTERNAL_SERVER_ERROR)
            }
        }
    }

    private fun <T> httpWrapped(fn: (Request) -> T, lens: BiDiBodyLens<T>): (Request) -> Response {
        return { req: Request ->
            try {
                lens.set(Response(Status.OK), fn(req))
            } catch (e: Exception) {
                logger.error("Failed", e)
                Response(Status.INTERNAL_SERVER_ERROR)
            }
        }
    }

    private val storage = InMemoryThrowablesStorage()

    private val sensor = MeteringThrowablesSensor(SimpleMeterRegistry())

    private val handler: ThrowablesHandler = DefaultThrowablesHandler(storage, sensor, storage)

    private val server = app.asServer(Netty(port)).start()

    private fun verify(payload: ByteBuffer): String = ThrowableParser.echo(String(payload.array()))

    private fun uuid(payload: ByteBuffer): UUID {
        val parsed = ThrowableParser.parse(payload)
        val handling = handler.handle(parsed)
        return handling.id
    }

    private fun wiredEx(lookup: Throwable?): WiredException? = lookup?.let { t: Throwable ->
        WiredException(
                className = (if (t is NamedException)
                    (t as NamedException).proxiedClassName
                else
                    t.javaClass.name),
                message = t.message,
                stacktrace = t.stackTrace?.let(this::wiredStackTrace),
                cause = t.cause?.let(this::wiredEx))
    }

    private fun wiredStackTrace(stackTrace: Array<StackTraceElement>?): Array<WiredStackTraceElement>? =
            stackTrace?.map { element ->
                WiredStackTraceElement(
                        classLoaderName = element.classLoaderName,
                        moduleName = element.moduleName,
                        moduleVersion = element.moduleVersion,
                        declaringClass = element.className,
                        methodName = element.methodName,
                        fileName = element.fileName,
                        lineNumber = element.lineNumber)
            }?.toTypedArray()

    fun stop() {
        server.stop()
    }
}
