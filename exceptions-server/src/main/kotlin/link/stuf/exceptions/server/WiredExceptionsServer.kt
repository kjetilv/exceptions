package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.dto.SpeciesExceptions
import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.WiredStackTrace
import link.stuf.exceptions.munch.ThrowableSpeciesId
import link.stuf.exceptions.munch.ThrowableSpecimenId
import link.stuf.exceptions.munch.ThrowableStackId
import link.stuf.exceptions.munch.Throwables
import link.stuf.exceptions.server.statik.Static
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
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
import java.util.regex.Pattern

class WiredExceptionsServer(
        private val controller: WiredExceptionsController,
        private val swaggerJson: () -> OpenAPI,
        private val selfDiagnose: Boolean = true,
        val port: Int = 8080
) {
    private val logger = LoggerFactory.getLogger(WiredExceptionsServer::class.java)

    private val submissionLens =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    private val specimenLens =
            Body.auto<Specimen>(contentNegotiation = ContentNegotiation.None).toLens()

    private val speciesExceptionsLens =
            Body.auto<SpeciesExceptions>(contentNegotiation = ContentNegotiation.None).toLens()

    private val stackLens =
            Body.auto<WiredStackTrace>(contentNegotiation = ContentNegotiation.None).toLens()

    private val swaggerLens =
            Body.auto<OpenAPI>(contentNegotiation = ContentNegotiation.None).toLens()

    private val staticContent =
            Thread.currentThread().contextClassLoader.let { cl ->
                Static(cl, "META-INF/resources/webjars/swagger-ui/".swaggerUi(cl))
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

    private val app = routes(
            "exception" bind Method.POST to { req ->
                submitException(req)
            },
            "exception/{uuid}" bind GET to { req ->
                lookupException(req)
            },
            "exceptions/{uuid}" bind GET to { req ->
                lookupExceptions(req)
            },
            "stack/{uuid}" bind GET to { req ->
                lookupStack(req)
            },
            "exception-out/{uuid}" bind GET to { req ->
                printException(req)
            },
            "swagger.json" bind GET to {
                applicationJson(swaggerLens, swaggerJson)
            },
            "/doc/{path}" bind GET to {
                Response(Status.OK).body(staticContent.read(it.path("path")))
            },
            "/" bind GET to {
                Response(Status.FOUND).header("Location", "/doc/index.html?url=/swagger.json")
            }
    )

    private fun submitException(it: Request): Response =
            applicationJson(submissionLens) {
                controller.handle(throwableInBody(it)).let { sub ->
                    Submission(sub.speciesId.hash, sub.specimenId.hash, sub.isLoggable, sub.isNew)
                }
            }

    private fun lookupException(it: Request): Response =
            applicationJson(specimenLens) {
                controller.lookupSpecimen(ThrowableSpecimenId(pathUuid(it)), flag(it, "fullStack"))
            }

    private fun lookupExceptions(it: Request): Response =
            applicationJson(speciesExceptionsLens) {
                controller.lookupSpecies(ThrowableSpeciesId(pathUuid(it)), flag(it, "fullStack"))
            }

    private fun lookupStack(it: Request): Response =
            applicationJson(stackLens) {
                controller.lookupStack(ThrowableStackId(pathUuid(it)), true)
            }

    private fun printException(it: Request): Response =
            textPlain {
                controller.lookupPrintable(ThrowableSpecimenId(pathUuid(it)))
            }

    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(Filter(errorHandler()))
            .then(app)
            .asServer(Netty(port))

    private fun errorHandler(): (HttpHandler) -> (Request) -> Response =
            { next ->
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

    fun start(): WiredExceptionsServer = apply {
        server.start()
    }

    fun stop(): WiredExceptionsServer = apply {
        server.stop()
    }

    private fun throwableInBody(req: Request) = ThrowableParser.parse(req.body.payload)!!

    private fun flag(req: Request, flag: String): Boolean = req.queries(flag).isNotEmpty()

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: () -> T): Response =
            response(ContentType.APPLICATION_JSON) { lens.set(ok(), t()) }

    private fun textPlain(t: () -> String): Response =
            response(ContentType.TEXT_PLAIN) { ok().body(t()) }

    private fun response(type: ContentType, toResponse: () -> Response): Response =
            try {
                toResponse().header("Content-Type", type.value)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to return type $type", e)
            }

    private fun ok() = Response(Status.OK)
}
