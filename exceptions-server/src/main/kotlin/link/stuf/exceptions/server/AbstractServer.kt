package link.stuf.exceptions.server

import link.stuf.exceptions.dto.Species
import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.WiredStackTrace
import link.stuf.exceptions.munch.ThrowableSpeciesId
import link.stuf.exceptions.munch.ThrowableSpecimenId
import link.stuf.exceptions.munch.ThrowableStackId
import link.stuf.exceptions.munch.Throwables
import link.stuf.exceptions.server.statik.Static
import org.http4k.core.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.server.asServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
abstract class AbstractServer(

        val host: String = "0.0.0.0",

        val port: Int = 8080,

        private val controller: WiredExceptionsController,

        private val selfDiagnose: Boolean = true
) {

    private val logger: Logger = LoggerFactory.getLogger(SimpleRoutingServer::class.java)

    fun start(): AbstractServer = apply {
        server.start()
    }

    fun stop(): AbstractServer = apply {
        server.stop()
    }

    private val staticContent =
            Thread.currentThread().contextClassLoader.let { cl ->
                Static(cl, "META-INF/resources/webjars/swagger-ui/".swaggerUi(cl))
            }

    protected abstract fun app(): HttpHandler

    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(Filter(errors()))
            .then(app())
            .asServer(NettyConfig(host, port))

    protected fun submitException(throwable: Throwable?) =
            controller.handle(throwable).let { sub ->
                Submission(sub.speciesId.hash, sub.specimenId.hash, sub.isLoggable, sub.isNew)
            }

    protected fun lookupException(
            uuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): Specimen =
            controller.lookupSpecimen(ThrowableSpecimenId(uuid), fullStack, simpleTrace)

    protected fun lookupExceptions(uuid: UUID, fullStack: Boolean = true): Species =
            controller.lookupSpecies(ThrowableSpeciesId(uuid), fullStack)

    protected fun lookupStack(
            pathUuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): WiredStackTrace =
            controller.lookupStack(ThrowableStackId(pathUuid), fullStack, simpleTrace)

    protected fun printException(pathUuid: UUID): String =
            controller.lookupPrintable(ThrowableSpecimenId(pathUuid))

    protected fun lookupThrowable(pathUuid: UUID): Throwable =
            controller.lookupThrowable(ThrowableSpecimenId(pathUuid))

    private fun errors(): (HttpHandler) -> (Request) -> Response = { next ->
        { req ->
            handleErrors(next, req)
        }
    }

    private fun handleErrors(next: HttpHandler, req: Request): Response {
        return try {
            next(req)
        } catch (e: Throwable) {
            if (selfDiagnose) {
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
        val handle = controller.handle(e)
        logger.error("Failed: ${handle.specimenId}", e)
        return Response(Status.INTERNAL_SERVER_ERROR).body(handle.specimenId.hash.toString())
    }

    private fun simpleFailedResponse(e: Throwable): Response {
        val speciesId = Throwables.species(e).id
        logger.error("Failed: $speciesId", e)
        return Response(Status.INTERNAL_SERVER_ERROR).body(speciesId.hash.toString())
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
