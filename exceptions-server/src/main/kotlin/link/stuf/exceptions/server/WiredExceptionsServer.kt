package link.stuf.exceptions.server

import link.stuf.exceptions.core.parser.ThrowableParser
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId
import link.stuf.exceptions.server.api.Submission
import link.stuf.exceptions.server.api.WiredExceptions
import org.http4k.core.*
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
        val port: Int = 8080
) {

    private val submissionLens: BiDiBodyLens<Submission> =
            Body.auto<Submission>(contentNegotiation = ContentNegotiation.None).toLens()

    private val wiredLens: BiDiBodyLens<WiredExceptions> =
            Body.auto<WiredExceptions>(contentNegotiation = ContentNegotiation.None).toLens()

    private val logger = LoggerFactory.getLogger(WiredExceptionsServer::class.java)

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
            })


    private val server = app.asServer(Netty(port))

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    private fun throwableInBody(it: Request) = ThrowableParser.parse(it.body.payload)!!

    private fun pathUuid(req: Request): UUID = req.path("uuid")?.let(UUID::fromString)!!

    private fun <T> applicationJson(lens: BiDiBodyLens<T>, t: () -> T): Response =
            response(ContentType.APPLICATION_JSON) { lens.set(ok(), t()) }

    private fun response(type: ContentType, toResponse: () -> Response): Response = try {
        toResponse().header("Content-Type", type.value)
    } catch (e: Exception) {
        logger.error("Failed", e)
        Response(Status.INTERNAL_SERVER_ERROR)
    }

    private fun ok() = Response(Status.OK)
}
