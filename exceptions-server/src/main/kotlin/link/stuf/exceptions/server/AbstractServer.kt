package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.munch.ThrowableSpeciesId
import link.stuf.exceptions.munch.ThrowableSpecimenId
import link.stuf.exceptions.munch.ThrowableStackId
import link.stuf.exceptions.munch.Throwables
import link.stuf.exceptions.server.statik.Static
import org.http4k.core.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.BiDiBodyLens
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.server.asServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

abstract class AbstractServer(
        private val controller: WiredExceptionsController,
        val host: String = "0.0.0.0",
        val port: Int = 8080,
        protected val selfDiagnose: Boolean = true
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
            .then(Filter(errorHandler()))
            .then(app())
            .asServer(NettyConfig(host, port))

    protected fun staticResponse(path: String?) = Response(Status.OK).body(staticContent.read(path))

    protected fun submitException(it: Request): Response =
            applicationJson(Lenses.submission) {
                acceptException(throwableInBody(it))
            }

    fun acceptException(throwable: Throwable?) =
            controller.handle(throwable).let { sub ->
                Submission(sub.speciesId.hash, sub.specimenId.hash, sub.isLoggable, sub.isNew)
            }

    fun lookupException(uuid: UUID, fullStack: Boolean = true): Specimen =
            controller.lookupSpecimen(ThrowableSpecimenId(uuid), fullStack)

    protected fun lookupException(it: Request): Response =
            applicationJson(Lenses.specimen) {
                val pathUuid = pathUuid(it)
                val fullStack = flag(it, "fullStack")
                lookupException(pathUuid, fullStack)
            }

    protected fun lookupExceptions(it: Request): Response =
            applicationJson(Lenses.species) {
                controller.lookupSpecies(ThrowableSpeciesId(pathUuid(it)), flag(it, "fullStack"))
            }

    protected fun lookupStack(it: Request): Response =
            applicationJson(Lenses.stack) {
                controller.lookupStack(ThrowableStackId(pathUuid(it)), true)
            }

    protected fun printException(it: Request): Response =
            textPlain {
                controller.lookupPrintable(ThrowableSpecimenId(pathUuid(it)))
            }

    private fun errorHandler(): (HttpHandler) -> (Request) -> Response =
            { next ->
                { req ->
                    handleErrors(next, req)
                }
            }

    protected fun swaggerJsonResponse(swaggerJson: () -> OpenAPI) =
            applicationJson(Lenses.swagger, swaggerJson)

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

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: () -> T): Response =
            response(ContentType.APPLICATION_JSON) { lens.set(Response(Status.OK), t()) }

    private fun textPlain(t: () -> String): Response =
            response(ContentType.TEXT_PLAIN) { Response(Status.OK).body(t()) }

    private fun throwableInBody(req: Request) = ThrowableParser.parse(req.body.payload)!!

    private fun flag(req: Request, flag: String): Boolean = req.queries(flag).isNotEmpty()

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun response(type: ContentType, toResponse: () -> Response): Response = try {
        toResponse().header("Content-Type", type.value)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to return type $type", e)
    }

    protected fun swaggerUiRoute(): RoutingHttpHandler = "/doc/{path}" bind Method.GET to {
        staticResponse(it.path("path"))
    }

    protected fun swaggerReroute(prefix: String): RoutingHttpHandler = "/" bind Method.GET to {
        Response(Status.FOUND).header("Location", "/doc/index.html?url=$prefix/swagger.json")
    }
}
