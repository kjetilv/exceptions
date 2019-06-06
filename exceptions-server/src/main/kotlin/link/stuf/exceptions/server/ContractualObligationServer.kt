package link.stuf.exceptions.server

import link.stuf.exceptions.dto.Specimen
import link.stuf.exceptions.dto.Submission
import link.stuf.exceptions.dto.WiredException
import link.stuf.exceptions.dto.WiredStackTrace
import org.http4k.contract.*
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.*
import org.http4k.routing.routes
import java.time.ZonedDateTime
import java.util.*

class ContractualObligationServer(
        controller: WiredExceptionsController,
        host: String = "0.0.0.0",
        port: Int = 8080,
        selfDiagnose: Boolean = true
) : AbstractServer(controller, host, port, selfDiagnose) {

    private fun submitSpecimenRoute(): ContractRoute = "/exception" meta {
        summary = "Submit an exception"
        receiving(Lenses.exception to exampleException())
        returning(OK, Lenses.submission to exampleSubmission())
    } bindContract Method.POST to bodyExchange(
            Lenses.exception, Lenses.submission, ::acceptException
    )

    private fun lookupSpecimenRoute(): ContractRoute {

        val fullStack = Query.boolean().optional("fullStack")

        return "/exception" / Path.uuid().of("/uuid") meta {
            summary = "Lookup an exception"
            returning(OK, Lenses.specimen to exampleSpecimen())
        } bindContract Method.GET to { uuid ->
            { req: Request ->
                Lenses.specimen.set(Response(OK), lookupException(uuid, fullStack(req) ?: false))
            }
        }
    }

    private fun <I, O> bodyExchange(inLens: BiDiBodyLens<I?>, outLens: BiDiBodyLens<O>, accept: (I) -> O): HttpHandler =
            { req ->
                inLens[req]?.let {
                    accept(it)
                }?.let {
                    outLens.set(Response(OK), it)
                } ?: Response(BAD_REQUEST)
            }

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

    override fun app(): HttpHandler = routes("/api/v1" bind contract {
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
    })

    companion object {

        private val random = Random()
    }
}
