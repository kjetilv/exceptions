package link.stuf.exceptions.server

import org.http4k.contract.*
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.lens.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.routes
import java.util.*

class ContractualObligationServer(
        configuration: ServerConfiguration = ServerConfiguration(),
        controller: WiredExceptionsController
) : AbstractServer(configuration, controller) {

    private fun logFaultRoute(): ContractRoute =
            "/fault" meta {
                summary = "Submit an exception"
                consumes += ContentType.TEXT_PLAIN
                produces += ContentType.APPLICATION_JSON
                receiving(Lens.exception to Ex.exception())
                returning(Status.OK, Lens.submission to Ex.submission())
            } bindContract Method.POST to bodyExchange(
                    Lens.exception, Lens.submission, ::submitFault
            )

    private fun lookupFaultRoute(): ContractRoute =
            "/fault" / Path.of("uuid") meta {
                summary = "Lookup an exception"
                queries += listOf(fullStack, simpleTrace)
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK, Lens.faultEvent to Ex.faultEventDtos())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.faultEvent) {
                        lookupFault(UUID.fromString(uuid), isSet(req, fullStack), isSet(req, simpleTrace))
                    }
                }
            }

    private fun lookupFaultsRoute(): ContractRoute =
            "/faults" / Path.of("uuid") meta {
                summary = "Lookup a fault type, with fault events"
                queries += listOf(fullStack, simpleTrace)
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK, Lens.faultType to Ex.faultTypeDto())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.faultType) {
                        lookupFaults(UUID.fromString(uuid), isSet(req, fullStack))
                    }
                }
            }

    private fun lookupCauseRoute(): ContractRoute =
            "/cause" / Path.of("uuid") meta {
                summary = "Lookup a stack"
                queries += listOf(fullStack, simpleTrace)
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK, Lens.cause to Ex.stack())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.cause) {
                        lookupCause(
                                UUID.fromString(uuid),
                                isSet(req, fullStack, true),
                                isSet(req, simpleTrace))
                    }
                }
            }

    private fun printFaultRoute(): ContractRoute =
            "/fault-out" / Path.of("uuid") meta {
                summary = "Print an exception"
                produces += ContentType.TEXT_PLAIN
                returning(Status.OK, Lens.string to Ex.exceptionOut())
            } bindContract Method.GET to { uuid ->
                {
                    responseWith(Lens.exception, type = ContentType.TEXT_PLAIN) {
                        lookupThrowable(UUID.fromString(uuid))
                    }
                }
            }

    private fun <I, O> bodyExchange(iLens: BiDiBodyLens<I>, oLens: BiDiBodyLens<O>, accept: (I) -> O): HttpHandler =
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
            swaggerReroute(configuration.prefix))

    private fun apiRoute(): RoutingHttpHandler =
            configuration.prefix bind contract {
                renderer = OpenApi3(
                        apiInfo = ApiInfo("Exceptions", "v1"),
                        json = JSON)
                descriptionPath = "/swagger.json"
                routes += appRoutes()
            }

    private fun appRoutes(): List<ContractRoute> {
        return listOf(
                logFaultRoute(),
                lookupFaultRoute(),
                lookupFaultsRoute(),
                lookupCauseRoute(),
                printFaultRoute())
    }

    companion object {

        private val fullStack = Query.boolean().optional("fullStack")

        private val simpleTrace = Query.boolean().optional("simpleTrace")

        private val offset = Query.long().optional("offset")

        private val count = Query.long().optional("count")
    }
}
