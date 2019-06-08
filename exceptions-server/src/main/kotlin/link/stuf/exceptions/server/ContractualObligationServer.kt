package link.stuf.exceptions.server

import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.lens.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.util.*

class ContractualObligationServer(
        controller: WiredExceptionsController,
        host: String = "0.0.0.0",
        port: Int = 8080,
        selfDiagnose: Boolean = true
) : AbstractServer(host, port, controller, selfDiagnose) {

    private fun submitSpecimenRoute(): ContractRoute =
            "/exception" meta {
                summary = "Submit an exception"
                receiving(Lens.exception to Ex.exception())
                returning(Status.OK, Lens.submission to Ex.submission())
            } bindContract Method.POST to bodyExchange(
                    Lens.exception, Lens.submission, ::submitException
            )

    private fun lookupSpecimenRoute(): ContractRoute =
            "/exception" / Path.of("uuid") meta {
                summary = "Lookup an exception"
                queries += listOf(fullStack, simpleTrace)
                returning(Status.OK, Lens.specimen to Ex.specimen())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.specimen) {
                        lookupException(UUID.fromString(uuid), isSet(req, fullStack), isSet(req, simpleTrace))
                    }
                }
            }

    private fun lookupSpecimensRoute(): ContractRoute =
            "/exceptions" / Path.of("uuid") meta {
                summary = "Lookup a species, with specimen"
                queries += listOf(fullStack, simpleTrace)
                returning(Status.OK, Lens.species to Ex.species())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.species) {
                        lookupExceptions(UUID.fromString(uuid), isSet(req, fullStack))
                    }
                }
            }

    private fun lookupStackRoute(): ContractRoute =
            "/stack" / Path.of("uuid") meta {
                summary = "Lookup a stack"
                queries += listOf(fullStack, simpleTrace)
                returning(Status.OK, Lens.stack to Ex.stack())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.stack) {
                        lookupStack(
                                UUID.fromString(uuid),
                                isSet(req, fullStack, true),
                                isSet(req, simpleTrace))
                    }
                }
            }

    private fun printThrowableRoute(): ContractRoute =
            "/exception-out" / Path.of("uuid") meta {
                summary = "Print an exception"
                returning(Status.OK, Lens.string to Ex.exceptionOut())
            } bindContract Method.GET to { uuid ->
                {
                    responseWith(Lens.exception, type = ContentType.TEXT_PLAIN) {
                        lookupThrowable(UUID.fromString(uuid))
                    }
                }
            }

    private fun <I, O> bodyExchange(iLens: BiDiBodyLens<I?>, oLens: BiDiBodyLens<O>, accept: (I) -> O): HttpHandler =
            { req ->
                iLens[req]?.let {
                    accept(it)
                }?.let {
                    oLens.set(Response(Status.OK), it)
                } ?: Response(Status.BAD_REQUEST)
            }

    private fun isSet(req: Request, lens: BiDiLens<Request, Boolean?>, defaultValue: Boolean = false) =
            lens[req] ?: defaultValue

    private fun <O> responseWith(
            outLens: BiDiBodyLens<O>,
            type: ContentType = ContentType.APPLICATION_JSON,
            result: () -> O
    ): Response =
            outLens.set(withContentType(Response(Status.OK), type), result())

    override fun app(): HttpHandler = routes(
            apiRoute(),
            swaggerUiRoute(),
            swaggerReroute("/api/v1"))

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
                    lookupSpecimenRoute(),
                    lookupSpecimensRoute(),
                    lookupStackRoute(),
                    printThrowableRoute()
            )
        }
    }

    companion object {

        private val fullStack = Query.boolean().optional("fullStack")

        private val simpleTrace = Query.boolean().optional("simpleTrace")
    }
}
