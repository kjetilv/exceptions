package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.dto.Species
import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.WiredStackTrace
import link.stuf.exceptions.munch.ThrowableSpeciesId
import link.stuf.exceptions.munch.ThrowableSpecimenId
import link.stuf.exceptions.munch.ThrowableStackId
import link.stuf.exceptions.munch.Throwables
import link.stuf.exceptions.server.JSON.auto
import link.stuf.exceptions.server.statik.Static
import org.http4k.core.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.ContentNegotiation
import org.http4k.routing.path
import org.http4k.server.asServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

abstract class AbstractServer(
        private val controller: WiredExceptionsController,
        protected val swaggerJson: () -> OpenAPI,
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

    private val submissionLens =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    private val specimenLens =
            Body.auto<Specimen>(contentNegotiation = ContentNegotiation.None).toLens()

    private val speciesLens =
            Body.auto<Species>(contentNegotiation = ContentNegotiation.None).toLens()

    private val stackLens =
            Body.auto<WiredStackTrace>(contentNegotiation = ContentNegotiation.None).toLens()

    private val swaggerLens =
            Body.auto<OpenAPI>(contentNegotiation = ContentNegotiation.None).toLens()

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
            applicationJson(submissionLens) {
                controller.handle(throwableInBody(it)).let { sub ->
                    Submission(sub.speciesId.hash, sub.specimenId.hash, sub.isLoggable, sub.isNew)
                }
            }

    protected fun lookupException(it: Request): Response =
            applicationJson(specimenLens) {
                controller.lookupSpecimen(ThrowableSpecimenId(pathUuid(it)), flag(it, "fullStack"))
            }

    protected fun lookupExceptions(it: Request): Response =
            applicationJson(speciesLens) {
                controller.lookupSpecies(ThrowableSpeciesId(pathUuid(it)), flag(it, "fullStack"))
            }

    protected fun lookupStack(it: Request): Response =
            applicationJson(stackLens) {
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
            applicationJson(swaggerLens, swaggerJson)

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
            response(ContentType.APPLICATION_JSON) { lens.set(ok(), t()) }

    private fun textPlain(t: () -> String): Response =
            response(ContentType.TEXT_PLAIN) { ok().body(t()) }

    private fun throwableInBody(req: Request) = ThrowableParser.parse(req.body.payload)!!

    private fun flag(req: Request, flag: String): Boolean = req.queries(flag).isNotEmpty()

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun response(type: ContentType, toResponse: () -> Response): Response =
            try {
                toResponse().header("Content-Type", type.value)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to return type $type", e)
            }

    private fun ok() = Response(Status.OK)
}
