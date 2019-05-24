package link.stuf.exceptions.server

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import link.stuf.exceptions.api.ThrowablesHandler
import link.stuf.exceptions.core.MeteringHandlerListener
import link.stuf.exceptions.core.clearing.DefaultThrowablesHandler
import link.stuf.exceptions.core.clearing.InMemoryThrowablesStorage
import link.stuf.exceptions.core.digest.Packages
import link.stuf.exceptions.core.digest.SimpleThrowableReducer
import link.stuf.exceptions.core.inputs.ThrowableParser
import link.stuf.exceptions.server.api.WiredException
import link.stuf.exceptions.server.api.WiredStackTraceElement
import org.http4k.core.*
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.nio.ByteBuffer
import java.util.*

class WiredExceptionsServer(port: Int) {

    private val lens = Body.auto<WiredException>().toLens()

    private val app = routes(
            "submit" bind Method.POST to { req: Request ->
                Response(Status.OK).body(uuid(req.body.payload).toString())
            },
            "echo" bind Method.POST to { req: Request ->
                Response(Status.OK).body(verify(req.body.payload))
            },
            "lookup/{uuid}" bind Method.GET to { req: Request ->
                req.path("uuid")
                        ?.let(UUID::fromString)?.let(handler::lookup)?.let(this::wiredEx)?.let {
                            lens.set(Response(Status.OK), it)
                        }!!
            }
    )

    private val handlerListener = MeteringHandlerListener(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

    private val handler: ThrowablesHandler = DefaultThrowablesHandler(
            InMemoryThrowablesStorage(),
            SimpleThrowableReducer(Packages.all(), Packages.none(), Packages.none()),
            handlerListener)

    private val server = app.asServer(Netty(port)).start()

    private fun verify(payload: ByteBuffer): String = ThrowableParser.echo(String(payload.array()))

    private fun uuid(payload: ByteBuffer): UUID {
        val parsed = ThrowableParser.parse(payload)
        val handling = handler.handle(parsed)
        return handling.id
    }

    private fun wiredEx(lookup: Throwable?): WiredException? = lookup?.let { t: Throwable ->
        WiredException(
                className = t.javaClass.name,
                message = t.message,
                stacktrace = t.stackTrace?.let(this::wiredStackTrace),
                cause = t.cause?.let(this::wiredEx)
        )
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
                        lineNumber = element.lineNumber
                )
            }?.toTypedArray()

    fun stop() {
        server.stop()
    }
}
