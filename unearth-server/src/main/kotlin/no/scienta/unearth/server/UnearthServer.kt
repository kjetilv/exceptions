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

import no.scienta.unearth.core.HandlingPolicy
import no.scienta.unearth.core.parser.ThrowableParser
import no.scienta.unearth.dto.*
import no.scienta.unearth.munch.data.FaultType
import no.scienta.unearth.munch.id.*
import no.scienta.unearth.munch.util.Throwables
import no.scienta.unearth.server.JSON.auto
import no.scienta.unearth.statik.Statik
import org.http4k.asString
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
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

class UnearthServer(
        private val configuration: UnearthConfig = UnearthConfig(),
        val controller: UnearthController
) {
    private val logger = LoggerFactory.getLogger(UnearthServer::class.java)

    fun start(after: (Http4kServer) -> Unit = {}): UnearthServer = apply {
        server.start()
        after(server)
    }

    fun stop(after: (Http4kServer) -> Unit = {}): UnearthServer = apply {
        server.stop()
        after(server)
    }

    private fun submitPrintedExceptionRoute() = "/throwable" meta {
        summary = "Submit an exception"
        consumes += ContentType.TEXT_PLAIN
        produces += ContentType.APPLICATION_JSON
        receiving(exception to Swagger.exception())
        returning(Status.OK, submission to Swagger.submission())
    } bindContract Method.POST to exchange(exception, submission) { throwable ->
        submission(controller submitRaw throwable)
    }

    private fun lookupFaultEventRoute() = "/fault-event" / uuid(::FaultEventId) meta {
        summary = "Lookup a fault event"
        queries += listOf(fullStack, printStack)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, faultEvent to Swagger.faultEventDtos())
    } bindContract Method.GET to { faultEventId ->
        { req ->
            get(faultEvent) {
                controller.lookupFaultEventDto(faultEventId,
                        fullStack[req] ?: false,
                        printStack[req] ?: false
                )
            }
        }
    }

    private fun lookupFaultTypeRoute() = "/fault-type" / uuid(::FaultTypeId) meta {
        summary = "Lookup a fault type"
        queries += listOf(fullStack, printStack, offsetQuery, countQuery)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, faultType to Swagger.faultTypeDto())
    } bindContract Method.GET to { faultTypeId ->
        { req ->
            get(faultType) {
                controller.lookupFaultTypeDto(faultTypeId,
                        fullStack[req] ?: false,
                        printStack[req] ?: false)
            }
        }
    }

    private fun lookupFaultRoute() = "/fault" / uuid(::FaultId) meta {
        summary = "Lookup a fault"
        queries += listOf(fullStack, printStack)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, fault to Swagger.faultDto())
    } bindContract Method.GET to { faultId ->
        { req ->
            get(fault) {
                controller.lookupFaultDto(faultId,
                        fullStack[req] ?: false,
                        printStack[req] ?: false)
            }
        }
    }

    private fun lookupCauseRoute() = "/cause-type" / uuid(::CauseTypeId) meta {
        summary = "Lookup a cause type"
        queries += listOf(fullStack, printStack)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, causeType to Swagger.causeTypeDto())
    } bindContract Method.GET to { causeTypeId ->
        { req ->
            get(causeType) {
                controller.lookupCauseTypeDto(causeTypeId,
                        fullStack[req] ?: false,
                        printStack[req] ?: false)
            }
        }
    }

    private fun lookupCauseTypeRoute() = "/cause" / uuid(::CauseId) meta {
        summary = "Lookup a cause"
        queries += listOf(fullStack, printStack)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, cause to Swagger.causeDto())
    } bindContract Method.GET to { causeId ->
        { req ->
            get(cause) {
                controller.lookupCauseDto(causeId,
                        fullStack[req] ?: false,
                        printStack[req] ?: false)
            }
        }
    }

    private fun feedLimitsGlobalRoute() = "/feed/limit" meta {
        summary = "Event limits global"
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, limit to Swagger.limit())
    } bindContract Method.GET to {
        get(limit) {
            controller.feedLimitGlobal()
        }
    }

    private fun feedLookupGlobalRoute() = "/feed" meta {
        summary = "Events global"
        produces += ContentType.APPLICATION_JSON
        queries += listOf(offsetQuery, countQuery, fullStack, printStack)
        returning(Status.OK, faultSequence to Swagger.faultSequence())
    } bindContract Method.GET to { req ->
        get(faultSequence) {
            controller.faultSequenceGlobal(
                    offsetQuery[req] ?: 0L,
                    countQuery[req] ?: 0L,
                    fullStack[req] ?: false,
                    printStack[req] ?: false)
        }
    }


    private fun feedLimitsFaultTypeRoute() = "/feed/fault-type/limit" / uuid(::FaultTypeId) meta {
        summary = "Event limits for a fault type"
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, limit to Swagger.limit())
    } bindContract Method.GET to { faultId ->
        {
            get(limit) {
                controller feedLimitFaultType faultId
            }
        }
    }

    private fun feedLookupFaultTypeRoute() = "/feed/fault-type" / uuid(::FaultTypeId) meta {
        summary = "Events for a fault type"
        produces += ContentType.APPLICATION_JSON
        queries += listOf(offsetQuery, countQuery, fullStack, printStack)
        returning(Status.OK, faultSequence to Swagger.faultSequence(::FaultTypeId))
    } bindContract Method.GET to { faultTypeId ->
        { req ->
            get(faultSequence) {
                controller.faultSequence(faultTypeId,
                        offsetQuery[req] ?: 0L,
                        countQuery[req] ?: 0L,
                        fullStack[req] ?: false,
                        printStack[req] ?: false)
            }
        }
    }

    private fun feedLimitsFaultRoute() = "/feed/fault/limit" / uuid(::FaultId) meta {
        summary = "Event limits for a fault"
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, limit to Swagger.limit())
    } bindContract Method.GET to { faultId ->
        {
            get(limit) {
                controller feedLimitFault faultId
            }
        }
    }

    private fun feedLookupFaultRoute() = "/feed/fault" / uuid(::FaultId) meta {
        summary = "Events for a fault"
        produces += ContentType.APPLICATION_JSON
        queries += listOf(offsetQuery, countQuery, fullStack, printStack)
        returning(Status.OK, faultSequence to Swagger.faultSequence(::FaultId))
    } bindContract Method.GET to { faultId ->
        { req ->
            get(faultSequence) {
                controller.faultSequence(faultId,
                        offsetQuery[req] ?: 0L,
                        countQuery[req] ?: 0L,
                        fullStack[req] ?: false,
                        printStack[req] ?: false)
            }
        }
    }

    private fun printFaultRoute() = "/throwable" / uuid(::FaultId) meta {
        summary = "Print an exception"
        produces += ContentType.TEXT_PLAIN
        returning(Status.OK, exception to Swagger.exception())
    } bindContract Method.GET to { faultId ->
        {
            get(exception, type = ContentType.TEXT_PLAIN) {
                controller lookupThrowable faultId
            }
        }
    }

    private fun printFaultReduxRoute() = "/throwable-redux" / uuid(::FaultId) meta {
        summary = "Print an exception"
        produces += ContentType.TEXT_PLAIN
        returning(Status.OK, exception to Swagger.exception())
    } bindContract Method.GET to { faultId ->
        {
            get(exception, type = ContentType.TEXT_PLAIN) {
                controller lookupThrowableRedux faultId
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun <I, O> exchange(
            inl: BiDiBodyLens<I>,
            outl: BiDiBodyLens<O>,
            accept: (I) -> O
    ): HttpHandler = { req ->
        inl[req]?.let {
            accept(it)
        }?.let {
            outl.set(Response(Status.OK), it)
        } ?: Response(Status.BAD_REQUEST)
    }

    private fun <O> get(
            outLens: BiDiBodyLens<O>,
            type: ContentType = ContentType.APPLICATION_JSON,
            result: () -> O
    ): Response = outLens.set(withContentType(Response(Status.OK), type), result())

    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(Filter { nextHandler ->
                { request ->
                    handleErrors(nextHandler, request)
                }
            })
            .then(routes(
                    rerouteToSwagger("/doc", configuration.prefix),
                    rerouteToSwagger("/", configuration.prefix),
                    swaggerUiRoute(configuration.prefix),
                    app())
            )
            .asServer(
                    NettyConfig(configuration.host, configuration.port))

    private fun app(): RoutingHttpHandler {
        return configuration.prefix bind contract {
            renderer = OpenApi3(
                    apiInfo = ApiInfo("Unearth", "v1"),
                    json = JSON)
            descriptionPath = "/swagger.json"

            routes += listOf(
                    submitPrintedExceptionRoute()
            )
            routes += listOf(
                    lookupFaultRoute(),
                    lookupFaultTypeRoute(),
                    lookupFaultEventRoute(),
                    lookupCauseRoute(),
                    lookupCauseTypeRoute()
            )
            routes += listOf(
                    feedLimitsGlobalRoute(),
                    feedLimitsFaultRoute(),
                    feedLimitsFaultTypeRoute(),
                    feedLookupGlobalRoute(),
                    feedLookupFaultRoute(),
                    feedLookupFaultTypeRoute()
            )
            routes += listOf(
                    printFaultRoute(),
                    printFaultReduxRoute()
            )
        }
    }

    private fun submission(handling: HandlingPolicy) = Submission(
            handling.faultTypeId,
            handling.faultId,
            handling.faultEventId,
            handling.globalSequence,
            handling.faultTypeSequence,
            handling.faultSequence,
            handling.isLoggable)

    private fun handleErrors(next: HttpHandler, req: Request): Response =
            try {
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

    private fun handledFailedResponse(e: Throwable): Response {
        val handle = controller.submitRaw(e)
        logger.error("Failed: ${handle.faultEventId}", e)
        return internalError.set(
                withContentType(Response(Status.INTERNAL_SERVER_ERROR)),
                UnearthInternalError(
                        message = e.toString(),
                        submission = submission(handle)))

    }

    private fun simpleFailedResponse(e: Throwable): Response {
        val faultTypeId = FaultType.create(e).id
        logger.error("Failed: $faultTypeId", e)
        return internalError.set(
                Response(Status.INTERNAL_SERVER_ERROR),
                UnearthInternalError(Throwables.join(e, " <= ")))
    }

    private fun swaggerUiRoute(prefix: String): RoutingHttpHandler = "/doc/{path}" bind Method.GET to {
        try {
            Response(Status.OK).body(statik.read(it.path("path")))
        } catch (e: Exception) {
            logger.debug("Redirecting failed swagger-ui load: $it", e)
            swaggerRedirect(prefix)
        }
    }

    private fun rerouteToSwagger(path: String, prefix: String): RoutingHttpHandler =
            path bind Method.GET to { swaggerRedirect(prefix) }

    private fun swaggerRedirect(prefix: String) =
            Response(Status.FOUND).header("Location", "/doc/index.html?url=$prefix/swagger.json")

    private fun withContentType(res: Response, type: ContentType = ContentType.APPLICATION_JSON) =
            res.header("Content-Type", type.value)

    override fun toString(): String = "${javaClass.simpleName}[$server]"

    companion object {

        private val fullStack =
                Query.boolean().optional("fullStack", "Provide a fully destructured stack")

        private val printStack =
                Query.boolean().optional("printStack", "Provide a list of printed stacktrace elements")

        private val offsetQuery =
                Query.long().optional("offset", "Start point in feed")

        private val countQuery =
                Query.long().optional("count", "No. of elements to retrieve from start point in feed")

        private fun <T : Id> uuid(read: (UUID) -> T) = PathLens(
                meta = Meta(required = true, location = "path", paramMeta = ParamMeta.StringParam, name = "uuid"),
                get = { uuid -> read(UUID.fromString(uuid)) }
        )

        private val swaggerUiPattern =
                Pattern.compile("^.*swagger-ui-([\\d.]+).jar!.*$")

        private const val swaggerUiPrefix = "META-INF/resources/webjars/swagger-ui/"

        private val exception: BiDiBodyLens<Throwable> = BiDiBodyLens(
                metas = emptyList(),
                contentType = ContentType.TEXT_PLAIN,
                get = { msg ->
                    ThrowableParser.parse(msg.body.payload)
                },
                setLens = { thr, msg ->
                    msg.body(Throwables.string(thr))
                })

        private val limit: BiDiBodyLens<Long> = BiDiBodyLens(
                metas = emptyList(),
                contentType = ContentType.TEXT_PLAIN,
                get = { msg -> java.lang.Long.parseLong(msg.body.payload.asString()) },
                setLens = { v, msg -> msg.body(v.toString()) }
        )

        private val submission = Body.auto<Submission>().toLens()

        private val faultSequence = Body.auto<FaultSequence>().toLens()

        private val faultEvent = Body.auto<FaultEventDto>().toLens()

        private val fault = Body.auto<FaultDto>().toLens()

        private val faultType = Body.auto<FaultTypeDto>().toLens()

        private val cause = Body.auto<CauseDto>().toLens()

        private val causeType = Body.auto<CauseTypeDto>().toLens()

        private val internalError = Body.auto<UnearthInternalError>().toLens()

        private fun swaggerUi(classLoader: ClassLoader): String {
            return classLoader.getResource(swaggerUiPrefix)?.let { url ->
                swaggerUiPattern.matcher(url.toExternalForm()).let { matcher ->
                    return if (matcher.matches())
                        swaggerUiPrefix + matcher.group(1) + "/"
                    else
                        throw java.lang.IllegalStateException("No swagger-ui version found: $url")
                }
            } ?: throw IllegalStateException("No swagger-ui webjar found")
        }

        private val statik = Thread.currentThread().contextClassLoader.let { Statik(it, swaggerUi(it)) }
    }
}
