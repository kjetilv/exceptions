package link.stuf.exceptions.server

import link.stuf.exceptions.core.inputs.ThrowableParser
import org.http4k.core.*
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.nio.ByteBuffer

class WiredExceptionsServer(val port: Int) {

    private val app = routes(
            "submit" bind Method.POST to { req: Request ->
                onThrowable(req)
            },
            "lookup" bind Method.GET to { req: Request ->
                getThrowable(req)
            },
            "verify" bind Method.POST to { req: Request ->
                Response(Status.OK).body(process(req.body.payload))
            }
    )

    private fun process(payload: ByteBuffer): String = ThrowableParser().toString(String(payload.array()))

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
