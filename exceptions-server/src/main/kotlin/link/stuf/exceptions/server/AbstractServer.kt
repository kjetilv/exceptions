package link.stuf.exceptions.server

import link.stuf.exceptions.dto.FaultTypeDto
import link.stuf.exceptions.dto.FaultEventDto
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.CauseDto
import link.stuf.exceptions.munch.ids.FaultTypeId
import link.stuf.exceptions.munch.ids.FaultEventId
import link.stuf.exceptions.munch.ids.CauseTypeId
import link.stuf.exceptions.munch.data.FaultType
import link.stuf.exceptions.server.statik.Static
import org.http4k.core.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
abstract class AbstractServer(
        val configuration: ServerConfiguration = ServerConfiguration(),
        val controller: WiredExceptionsController
) {

    private val logger: Logger = LoggerFactory.getLogger(AbstractServer::class.java)

    fun start(after: (Http4kServer) -> Unit = {}): AbstractServer = apply {
        server.start()
        after(server)
    }

    fun stop(after: (Http4kServer) -> Unit = {}): AbstractServer = apply {
        server.stop()
        after(server)
    }

    private val staticContent =
            Thread.currentThread().contextClassLoader.let { cl ->
                Static(cl, "META-INF/resources/webjars/swagger-ui/".swaggerUi(cl))
            }

    protected abstract fun app(): HttpHandler

    @Suppress("LeakingThis")
    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(Filter(errors()))
            .then(app())
            .asServer(NettyConfig(configuration.host, configuration.port))

    protected fun submitFault(throwable: Throwable?) =
            controller.submit(throwable).let { sub ->
                Submission(
                        sub.faultTypeId.hash,
                        sub.faultId.hash,
                        sub.faultEventId.hash,
                        sub.isLoggable,
                        sub.isNew)
            }

    protected fun lookupFault(
            uuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): FaultEventDto =
            controller.lookupEvent(FaultEventId(uuid), fullStack, simpleTrace)

    protected fun lookupFaults(uuid: UUID, fullStack: Boolean = true): FaultTypeDto =
            controller.lookupFaultType(FaultTypeId(uuid), fullStack)

    protected fun lookupCause(
            pathUuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): CauseDto =
            controller.lookupStack(CauseTypeId(pathUuid), fullStack, simpleTrace)

    protected fun printException(pathUuid: UUID): String =
            controller.lookupPrintable(FaultEventId(pathUuid))

    protected fun lookupThrowable(pathUuid: UUID): Throwable =
            controller.lookupThrowable(FaultEventId(pathUuid))

    private fun errors(): (HttpHandler) -> (Request) -> Response =
            { next ->
                { req ->
                    handleErrors(next, req)
                }
            }

    private fun handleErrors(next: HttpHandler, req: Request): Response {
        return try {
            next(req)
        } catch (e: Throwable) {
            if (configuration.selfDiagnose) {
                try {
                    handledFailedResponse(e)
                } catch (sde: Exception) {
                    logger.warn("Failed to self-diagnose", sde)
                    simpleFailedResponse(e)
                }
            } else {
                simpleFailedResponse(e)
            }
        }
    }

    private fun handledFailedResponse(e: Throwable): Response {
        val handle = controller.submit(e)
        logger.error("Failed: ${handle.faultEventId}", e)
        return Response(Status.INTERNAL_SERVER_ERROR).body(handle.faultEventId.hash.toString())
    }

    private fun simpleFailedResponse(e: Throwable): Response {
        val faultTypeId = FaultType.create(e).id
        logger.error("Failed: $faultTypeId", e)
        return Response(Status.INTERNAL_SERVER_ERROR).body(faultTypeId.hash.toString())
    }

    private fun String.swaggerUi(cl: ClassLoader): String =
            cl.getResource(this)?.let { url ->
                Pattern.compile("^.*swagger-ui-([\\d.]+).jar!.*$").matcher(url.toExternalForm()).let { matcher ->
                    if (matcher.matches()) {
                        return this + matcher.group(1) + "/"
                    }
                    throw java.lang.IllegalStateException("No swagger-ui version found: $url")
                }
            } ?: throw IllegalStateException("No swagger-ui webjar found")

    protected fun swaggerUiRoute(): RoutingHttpHandler = "/doc/{path}" bind Method.GET to {
        Response(Status.OK).body(staticContent.read(it.path("path")))
    }

    protected fun swaggerReroute(prefix: String): RoutingHttpHandler = "/" bind Method.GET to {
        Response(Status.FOUND).header("Location", "/doc/index.html?url=$prefix/swagger.json")
    }

    protected fun withContentType(toResponse1: Response, type: ContentType) =
            toResponse1.header("Content-Type", type.value)
}
