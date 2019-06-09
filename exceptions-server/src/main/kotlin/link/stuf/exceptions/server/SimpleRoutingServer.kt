package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import link.stuf.exceptions.core.parser.ThrowableParser
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.lens.BiDiBodyLens
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.util.*

open class SimpleRoutingServer(
        configuration: ServerConfiguration = ServerConfiguration(),
        controller: WiredExceptionsController,
        private val swaggerJson: () -> OpenAPI
) : AbstractServer(configuration, controller) {

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun flag(req: Request, flag: String): Boolean = req.queries(flag).isNotEmpty()

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: () -> T): Response =
            response(ContentType.APPLICATION_JSON) { lens.set(Response(Status.OK), t()) }

    private fun textPlain(t: () -> String): Response =
            response(ContentType.TEXT_PLAIN) { Response(Status.OK).body(t()) }

    private fun response(type: ContentType, toResponse: () -> Response): Response = try {
        withContentType(toResponse(), type)
    } catch (e: Exception) {
        throw IllegalStateException("Failed to return type $type", e)
    }

    private fun throwableInBody(req: Request) = ThrowableParser.parse(req.body.payload)!!

    private fun lookupException(it: Request): Response = applicationJson(Lens.specimen) {
        lookupException(pathUuid(it), flag(it, "fullStack"))
    }

    private fun lookupExceptions(it: Request): Response = applicationJson(Lens.species) {
        lookupExceptions(pathUuid(it), flag(it, "fullStack"))
    }

    private fun lookupStack(it: Request): Response = applicationJson(Lens.stack) {
        lookupStack(pathUuid(it))
    }

    private fun submitException(it: Request): Response = applicationJson(Lens.submission) {
        submitException(throwableInBody(it))
    }

    private fun printException(it: Request): Response = textPlain {
        printException(pathUuid(it))
    }

    private fun swaggerJsonResponse(swaggerJson: () -> OpenAPI) =
            applicationJson(Lens.swagger, swaggerJson)

    override fun app(): HttpHandler = routes(
            configuration.prefix + "exception" bind Method.POST to { req ->
                submitException(req)
            },
            configuration.prefix + "exception/{uuid}" bind GET to { req ->
                lookupException(req)
            },
            configuration.prefix + "exceptions/{uuid}" bind GET to { req ->
                lookupExceptions(req)
            },
            configuration.prefix + "stack/{uuid}" bind GET to { req ->
                lookupStack(req)
            },
            configuration.prefix + "exception-out/{uuid}" bind GET to { req ->
                printException(req)
            },
            configuration.prefix + "swagger.json" bind GET to {
                swaggerJsonResponse(swaggerJson)
            },
            swaggerUiRoute(),
            swaggerReroute(configuration.prefix)
    )
}
