package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId
import link.stuf.exceptions.core.throwables.Throwables
import link.stuf.exceptions.server.api.Submission
import link.stuf.exceptions.server.api.WiredExceptions
import link.stuf.exceptions.server.statik.Static
import org.http4k.core.*
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

class WiredExceptionsServer(
        controller: WiredExceptionsController,
        swaggerJson: () -> OpenAPI,
        val port: Int = 8080
) {
    private val logger = LoggerFactory.getLogger(WiredExceptionsServer::class.java)

    private val submissionLens =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    private val wiredLens =
            Body.auto<WiredExceptions>(contentNegotiation = ContentNegotiation.None).toLens()

    private val swaggerLens =
            Body.auto<OpenAPI>(contentNegotiation = ContentNegotiation.None).toLens()

    private val staticContent = Static(
            Thread.currentThread().contextClassLoader,
            "META-INF/resources/webjars/swagger-ui/3.22.1/")

    private val app = routes(
            "submit" bind Method.POST to {
                applicationJson(submissionLens) {
                    controller.handle(throwableInBody(it)).let {
                        Submission(it.speciesId.hash, it.specimenId.hash)
                    }
                }
            },
            "lookup/{uuid}" bind Method.GET to {
                applicationJson(wiredLens) {
                    controller.lookup(ThrowableSpeciesId(pathUuid(it)))
                }
            },
            "swagger.json" bind Method.GET to {
                applicationJson(swaggerLens, swaggerJson)
            },
            "/doc/{path}" bind Method.GET to {
                Response(Status.OK).body(staticContent.read(it.path("path")))
            },
            "/" bind Method.GET to {
                Response(Status.FOUND).header("Location", "/doc/index.html?url=/swagger.json")
            })

    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(ServerFilters.GZip())
            .then(Filter(errorHandler()))
            .then(app)
            .asServer(Netty(port))

    private fun errorHandler(): (HttpHandler) -> (Request) -> Response =
            { next ->
                { req ->
                    try {
                        next(req)
                    } catch (e: Exception) {
                        val id = Throwables.species(e).id
                        logger.error("Failed: " + id.hash, e)
                        Response(Status.INTERNAL_SERVER_ERROR).body(id.hash.toString())
                    }
                }
            }

    fun start(): WiredExceptionsServer = apply {
        server.start()
    }

    fun stop(): WiredExceptionsServer = apply {
        server.stop()
    }

    private fun throwableInBody(it: Request) = ThrowableParser.parse(it.body.payload)!!

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: () -> T): Response =
            response(ContentType.APPLICATION_JSON) { lens.set(ok(), t()) }

    private fun response(type: ContentType, toResponse: () -> Response): Response =
            toResponse().header("Content-Type", type.value)

    private fun ok() = Response(Status.OK)
}
