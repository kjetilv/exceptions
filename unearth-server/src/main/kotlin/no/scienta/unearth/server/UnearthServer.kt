/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.server

import no.scienta.unearth.dto.*
import no.scienta.unearth.munch.data.FaultType
import no.scienta.unearth.munch.ids.CauseTypeId
import no.scienta.unearth.munch.ids.FaultEventId
import no.scienta.unearth.munch.ids.FaultTypeId
import no.scienta.unearth.server.statik.Statik
import org.http4k.contract.ContractRoute
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.*
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

class UnearthServer(
        private val configuration: UnearthConfig = UnearthConfig(),
        val controller: UnearthController
) {

    private val logger: Logger = LoggerFactory.getLogger(UnearthServer::class.java)

    private fun logFaultRoute(): ContractRoute =
            "/fault" meta {
                summary = "Submit an exception"
                consumes += ContentType.TEXT_PLAIN
                produces += ContentType.APPLICATION_JSON
                receiving(Lens.exception to Example.exception())
                returning(Status.OK, Lens.submission to Example.submission())
            } bindContract Method.POST to bodyExchange(
                    Lens.exception, Lens.submission, ::submitFault
            )

    private fun lookupFaultRoute(): ContractRoute =
            "/fault" / uuidPath meta {
                summary = "Lookup an exception"
                queries += listOf(fullStack, simpleTrace)
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK, Lens.faultEvent to Example.faultEventDtos())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.faultEvent) {
                        lookupFault(uuid, isSet(req, fullStack), isSet(req, simpleTrace))
                    }
                }
            }

    private fun lookupFaultsRoute(): ContractRoute =
            "/faults" / uuidPath meta {
                summary = "Lookup a fault type, with fault events"
                queries += listOf(fullStack, simpleTrace, offsetQuery, countQuery)
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK, Lens.faultType to Example.faultTypeDto())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.faultType) {
                        lookupFaults(uuid, isSet(req, fullStack), offsetQuery[req], countQuery[req])
                    }
                }
            }

    private fun lookupCauseRoute(): ContractRoute =
            "/cause" / uuidPath meta {
                summary = "Lookup a stack"
                queries += listOf(fullStack, simpleTrace)
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK, Lens.cause to Example.stack())
            } bindContract Method.GET to { uuid ->
                { req ->
                    responseWith(Lens.cause) {
                        lookupCause(
                                uuid,
                                isSet(req, fullStack, true),
                                isSet(req, simpleTrace))
                    }
                }
            }

    private fun printFaultRoute(): ContractRoute =
            "/fault-out" / uuidPath meta {
                summary = "Print an exception"
                produces += ContentType.TEXT_PLAIN
                returning(Status.OK, Lens.string to Example.exceptionOut())
            } bindContract Method.GET to { uuid ->
                {
                    responseWith(Lens.exception, type = ContentType.TEXT_PLAIN) {
                        lookupThrowable(uuid)
                    }
                }
            }

    private fun feedLimitsRoute(): ContractRoute =
            "/feed/limit" / Lens.sequenceType / uuidPath meta {
                summary = "Event limits"
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK)
            } bindContract Method.GET to { type, uuid ->
                {
                    responseWith {
                        lookupFeedLimit(type, uuid).toString()
                    }
                }
            }

    private fun feedLimitsGlobalRoute(): ContractRoute =
            "/feed/limit" meta {
                summary = "Event limits"
                produces += ContentType.APPLICATION_JSON
                returning(Status.OK)
            } bindContract Method.GET to {
                responseWith {
                    lookupFeedLimit().toString()
                }
            }

    private fun feedLookupRoute(): ContractRoute =
            "/feed" / Lens.sequenceType / uuidPath / offsetPath meta {
                summary = "Events"
                produces += ContentType.APPLICATION_JSON
                queries += listOf(countQuery, thinFeedQuery)
                returning(Status.OK, Lens.faultSequence to Example.faultSequence())
            } bindContract Method.GET to { type, uuid, offset ->
                { req ->
                    responseWith(Lens.faultSequence) {
                        lookupFaultSequence(
                                type,
                                uuid,
                                offset,
                                countQuery[req] ?: 0L,
                                isSet(req, thinFeedQuery))
                    }
                }
            }

    private fun feedLookupGlobalRoute(): ContractRoute =
            "/feed" / offsetPath meta {
                summary = "Events"
                produces += ContentType.APPLICATION_JSON
                queries += listOf(countQuery, thinFeedQuery)
                returning(Status.OK, Lens.faultSequence to Example.faultSequence())
            } bindContract Method.GET to { offset ->
                { req ->
                    responseWith(Lens.faultSequence) {
                        lookupFaultSequence(
                                offset,
                                countQuery[req] ?: 0L,
                                isSet(req, thinFeedQuery))
                    }
                }
            }

    private fun <I, O> bodyExchange(inLens: BiDiBodyLens<I>, outLens: BiDiBodyLens<O>, accept: (I) -> O): HttpHandler =
            { req ->
                inLens[req]?.let {
                    accept(it)
                }?.let {
                    outLens.set(Response(Status.OK), it)
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

    private fun responseWith(
            type: ContentType = ContentType.APPLICATION_JSON,
            result: () -> String
    ): Response =
            withContentType(Response(Status.OK), type).body(result())

    fun start(after: (Http4kServer) -> Unit = {}): UnearthServer = apply {
        server.start()
        after(server)
    }

    fun stop(after: (Http4kServer) -> Unit = {}): UnearthServer = apply {
        server.stop()
        after(server)
    }

    private val staticContent =
            Thread.currentThread().contextClassLoader.let { cl ->
                Statik(cl, "META-INF/resources/webjars/swagger-ui/".swaggerUi(cl))
            }

    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(Filter(errors()))
            .then(routes(
                    configuration.prefix bind contract {
                        renderer = OpenApi3(
                                apiInfo = ApiInfo("Unearth", "v1"),
                                json = JSON)
                        descriptionPath = "/swagger.json"
                        routes += listOf(
                                logFaultRoute(),
                                lookupFaultRoute(),
                                lookupFaultsRoute(),
                                lookupCauseRoute(),
                                feedLimitsGlobalRoute(),
                                feedLookupGlobalRoute(),
                                feedLimitsRoute(),
                                feedLookupRoute(),
                                printFaultRoute())
                    },
                    swaggerUiRoute(),
                    swaggerReroute(configuration.prefix)))
            .asServer(
                    NettyConfig(configuration.host, configuration.port)
            )

    private fun submitFault(throwable: Throwable?) =
            controller.submit(throwable).let {
                Submission(
                        it.faultTypeId.hash,
                        it.faultId.hash,
                        it.faultEventId.hash,
                        it.globalSequence,
                        it.faultTypeSequence,
                        it.faultSequence,
                        it.isLoggable)
            }

    private fun lookupFault(
            uuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): FaultEventDto =
            controller.lookupEvent(FaultEventId(uuid), fullStack, simpleTrace)

    private fun lookupFaults(
            uuid: UUID,
            fullStack: Boolean = true,
            offset: Long?,
            count: Long?
    ): FaultTypeDto =
            controller.lookupFaultType(FaultTypeId(uuid), fullStack, offset, count)

    private fun lookupCause(
            pathUuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): CauseDto =
            controller.lookupStack(CauseTypeId(pathUuid), fullStack, simpleTrace)

    private fun lookupFeedLimit(): Long = controller.feedLimit()

    private fun lookupFeedLimit(type: SequenceType, uuid: UUID): Long = controller.feedLimit(type, uuid)

    private fun lookupFaultSequence(
            offset: Long,
            count: Long,
            thin: Boolean
    ): FaultSequence =
            controller.faultSequence(offset, count, thin)

    private fun lookupFaultSequence(
            type: SequenceType,
            uuid: UUID,
            offset: Long,
            count: Long,
            thin: Boolean
    ): FaultSequence =
            controller.faultSequence(type, uuid, offset, count, thin)

    private fun lookupThrowable(uuid: UUID): Throwable =
            controller.lookupFault(uuid)

    private fun errors(): (HttpHandler) -> (Request) -> Response =
            { next ->
                { req ->
                    handleErrors(next, req)
                }
            }

    private fun handleErrors(next: HttpHandler, req: Request): Response {
        return try {
            next(req)
        } catch (e: Throwable) {
            if (configuration.selfDiagnose) {
                try {
                    handledFailedResponse(e)
                } catch (sde: Exception) {
                    logger.warn("Failed to self-diagnose", sde)
                    simpleFailedResponse(e)
                }
            } else {
                simpleFailedResponse(e)
            }
        }
    }

    private fun handledFailedResponse(e: Throwable): Response {
        val handle = controller.submit(e)
        logger.error("Failed: ${handle.faultEventId}", e)
        return Response(Status.INTERNAL_SERVER_ERROR).body(handle.faultEventId.hash.toString())
    }

    private fun simpleFailedResponse(e: Throwable): Response {
        val faultTypeId = FaultType.create(e).id
        logger.error("Failed: $faultTypeId", e)
        return Response(Status.INTERNAL_SERVER_ERROR).body(faultTypeId.hash.toString())
    }

    private fun String.swaggerUi(cl: ClassLoader): String =
            cl.getResource(this)?.let { url ->
                Pattern.compile("^.*swagger-ui-([\\d.]+).jar!.*$").matcher(url.toExternalForm()).let { matcher ->
                    if (matcher.matches()) {
                        return this + matcher.group(1) + "/"
                    }
                    throw java.lang.IllegalStateException("No swagger-ui version found: $url")
                }
            } ?: throw IllegalStateException("No swagger-ui webjar found")

    private fun swaggerUiRoute(): RoutingHttpHandler = "/doc/{path}" bind Method.GET to {
        Response(Status.OK).body(staticContent.read(it.path("path")))
    }

    private fun swaggerReroute(prefix: String): RoutingHttpHandler = "/" bind Method.GET to {
        Response(Status.FOUND).header("Location", "/doc/index.html?url=$prefix/swagger.json")
    }

    private fun withContentType(toResponse1: Response, type: ContentType) =
            toResponse1.header("Content-Type", type.value)

    companion object {

        private val fullStack = Query.boolean().optional("fullStack")

        private val simpleTrace = Query.boolean().optional("simpleTrace")

        private val offsetQuery = Query.long().optional("offset")

        private val countQuery = Query.long().optional("count")

        private val thinFeedQuery = Query.boolean().optional("thin")

        private val offsetPath = Path.long().of("offset")

        private val uuidPath = Path.uuid().of("uuid")
    }
}
