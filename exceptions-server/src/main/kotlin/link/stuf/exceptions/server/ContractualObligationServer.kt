package link.stuf.exceptions.server

import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.WiredException
import link.stuf.exceptions.dto.WiredStackTrace
import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.BiDiBodyLens
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.ZonedDateTime
import java.util.*

class ContractualObligationServer(
        controller: WiredExceptionsController,
        host: String = "0.0.0.0",
        port: Int = 8080,
        selfDiagnose: Boolean = true
) : AbstractServer(controller, host, port, selfDiagnose) {

    private val fullStack = Query.boolean().optional("fullStack")

    private fun submitSpecimenRoute(): ContractRoute =
            "/exception" meta {
                summary = "Submit an exception"
                receiving(Lenses.exception to exampleException())
                returning(Status.OK, Lenses.submission to exampleSubmission())
            } bindContract Method.POST to bodyExchange(
                    Lenses.exception, Lenses.submission, ::acceptException
            )

    private fun lookupSpecimenRoute(): ContractRoute =
            "/exception" / Path.of("uuid") meta {
                summary = "Lookup an exception"
                returning(Status.OK, Lenses.specimen to exampleSpecimen())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lenses.specimen) {
                        lookupException(UUID.fromString(uuid), fullStack(req) ?: false)
                    }
                }
            }

    private fun <I, O> bodyExchange(inLens: BiDiBodyLens<I?>, outLens: BiDiBodyLens<O>, accept: (I) -> O): HttpHandler =
            { req ->
                inLens[req]?.let {
                    accept(it)
                }?.let {
                    outLens.set(Response(Status.OK), it)
                } ?: Response(Status.BAD_REQUEST)
            }

    private fun <O> responseWith(outLens: BiDiBodyLens<O>, result: () -> O): Response =
            outLens.set(Response(Status.OK), result())

    private fun exampleUuid() = UUID.randomUUID()

    private fun exampleSubmission() =
            Submission(UUID.randomUUID(), UUID.randomUUID())

    private fun exampleSpecimen() = Specimen(
            exampleUuid(), exampleUuid(), random.nextLong(), ZonedDateTime.now(), exampleWiredException())

    private fun exampleWiredException(): WiredException =
            WiredException(
                    "BadClass",
                    "Bad class!",
                    stacktrace = WiredStackTrace("foo.bar.BadException", emptyList(), exampleUuid()));

    private fun exampleException(): Throwable = RuntimeException("Example throwable")

    override fun app(): HttpHandler = routes(
            apiRoute(),
            swaggerUiRoute(),
            swaggerReroute("/api/v1")
    )

    private fun apiRoute(): RoutingHttpHandler {
        return "/api/v1" bind contract {
            renderer = OpenApi3(
                    ApiInfo(
                            "My great API",
                            "v1.0"
                    ),
                    JSON
            )
            descriptionPath = "/swagger.json"
            routes += listOf(
                    submitSpecimenRoute(),
                    lookupSpecimenRoute())
        }
    }

    companion object {

        private val random = Random()
    }
}
