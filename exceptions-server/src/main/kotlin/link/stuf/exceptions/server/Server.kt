package link.stuf.exceptions.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request

fun main() {

    val server = WiredExceptionsServer(9000)

    val request = Request(Method.GET, "http://localhost:9000/submit").query("name", "John Doe")

    val client = ApacheClient()

    println(client(request))

    server.stop()
}
