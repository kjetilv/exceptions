package link.stuf.exceptions.server

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer

class WiredExceptionsServer(val port: Int) {

    private val app = routes(
            "submit" bind Method.POST to { req: Request ->
                onThrowable(req)
            },
            "lookup" bind Method.GET to { req: Request ->
                getThrowable(req)
            }
    )

    private val server = app.asServer(Netty(9000)).start()

    fun stop() {
        server.stop()
    }

    private fun getThrowable(request: Request): Response {
        return Response(Status.OK).body("Hello, ${request.query("name")}!")
    }

    private fun onThrowable(request: Request): Response {
        return Response(Status.OK).body("Hello, ${request.query("name")}!")
    }
}
