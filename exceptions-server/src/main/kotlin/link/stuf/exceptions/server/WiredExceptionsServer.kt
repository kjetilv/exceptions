package link.stuf.exceptions.server

import link.stuf.exceptions.core.digest.ThrowablesDigest
import link.stuf.exceptions.core.inputs.ThrowableParser
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer
import java.nio.ByteBuffer

class WiredExceptionsServer(val port: Int) {

    private val app = routes(
            "submit" bind Method.POST to { req: Request ->
                Response(Status.OK).body(uuid(req.body.payload))
            },
            "verify" bind Method.POST to { req: Request ->
                Response(Status.OK).body(verify(req.body.payload))
            }
    )

    private val server = app.asServer(Netty(port)).start()

    private fun verify(payload: ByteBuffer): String = ThrowableParser.echo(String(payload.array()))

    private fun uuid(payload: ByteBuffer): String {
        return ThrowablesDigest.of(ThrowableParser.parse(payload)).id.toString();
    }

    fun stop() {
        server.stop()
    }
}
