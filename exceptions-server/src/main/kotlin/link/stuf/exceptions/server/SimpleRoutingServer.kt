package link.stuf.exceptions.server

import io.swagger.v3.oas.models.OpenAPI
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

class SimpleRoutingServer(
        controller: WiredExceptionsController,
        val swaggerJson: () -> OpenAPI,
        host: String = "0.0.0.0",
        port: Int = 8080,
        selfDiagnose: Boolean = true
) : AbstractServer(controller, host, port, selfDiagnose) {

    override fun app(): HttpHandler = routes(
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
                swaggerJsonResponse(swaggerJson)
            },
            "/doc/{path}" bind GET to {
                staticResponse(it.path("path"))
            },
            "/" bind GET to {
                Response(Status.FOUND).header("Location", "/doc/index.html?url=/swagger.json")
            }
    )
}
