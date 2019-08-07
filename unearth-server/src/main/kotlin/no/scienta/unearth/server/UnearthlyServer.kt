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
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("UNNECESSARY_SAFE_CALL", "USELESS_ELVIS")

package no.scienta.unearth.server

import no.scienta.unearth.munch.id.*
import no.scienta.unearth.munch.model.Fault
import no.scienta.unearth.munch.parser.ThrowableParser
import no.scienta.unearth.server.JSON.auto
import no.scienta.unearth.server.dto.*
import no.scienta.unearth.statik.Statik
import no.scienta.unearth.util.Throwables
import org.http4k.asString
import org.http4k.contract.contract
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.*
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
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
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*
import java.util.jar.JarFile
import java.util.regex.Pattern

class UnearthlyServer(
        private val configuration: UnearthlyConfig = UnearthlyConfig(),
        val controller: UnearthlyController
) {

    private val server = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)
            .then(ServerFilters.GZipContentTypes(setOf(APPLICATION_JSON, TEXT_PLAIN, TEXT_HTML)))
            .then(errorFilter())
            .then(routes(
                    rerouteToSwagger("/doc", configuration.prefix),
                    rerouteToSwagger("/", configuration.prefix),
                    swaggerUiRoute(configuration.prefix),
                    contracts()))
            .asServer(NettyConfig(configuration.host, configuration.port))

    fun start(after: (Http4kServer) -> Unit = {}): UnearthlyServer = apply {
        server.start()
        after(server)
    }

    fun reset() {
        controller.reset()
    }

    fun stop(after: (Http4kServer) -> Unit = {}): UnearthlyServer = apply {
        server.stop()
        after(server)
    }

    fun port(): Int = server.port()

    private fun pingRoute() =
            "ping" meta {
                summary = "Ping"
                consumes += TEXT_PLAIN
                produces += TEXT_PLAIN
            } bindContract GET to { request ->
                Response(OK).body("pong\n")
            }

    private fun submitExceptionRoute() =
            "catch" meta {
                summary = "Submit an exception"
                consumes += TEXT_PLAIN
                produces += APPLICATION_JSON
                returning(OK, submission to Swaggex.submission())
            } bindContract POST to { request ->
                val throwable = ThrowableParser.parse(request.body.payload)
                submission.set(Response(OK), controller.submission(controller.submitRaw(throwable)))
            }

    private fun retrieveExceptionRoute() =
            "throwable" / uuid(::FaultId) meta {
                summary = "Print an exception"
                produces += TEXT_PLAIN
                returning(OK, exception to Swaggex.exception())
            } bindContract GET to { faultId ->
                simpleGet(exception, type = TEXT_PLAIN) {
                    controller.lookupThrowable(faultId)
                }
            }

    private fun retrieveExceptionReduxRoute() =
            "throwable-redux" / uuid(::FaultId) meta {
                summary = "Print an exception"
                produces += APPLICATION_JSON
                queries += groupsQuery
                returning(OK, causeChain to Swaggex.causeChainDto())
            } bindContract GET to { faultId ->
                { req ->
                    get(causeChain) {
                        controller.rewriteThrowable(faultId, groupsQuery[req])
                    }
                }
            }

    private fun faultEventRoute() =
            "fault-event" / uuid(::FaultEventId) meta {
                summary = "Lookup a fault event"
                queries += listOf(fullStack, printStack)
                produces += APPLICATION_JSON
                returning(OK, faultEvent to Swaggex.faultEventDto())
            } bindContract GET to { faultEventId ->
                { req ->
                    get(faultEvent) {
                        controller.lookupFaultEventDto(faultEventId,
                                fullStack[req] ?: false,
                                printStack[req] ?: false,
                                true
                        )
                    }
                }
            }

    private fun faultStrandRoute() =
            "fault-strand" / uuid(::FaultStrandId) meta {
                summary = "Lookup a fault strand"
                queries += listOf(fullStack, printStack, offsetQuery, countQuery)
                produces += APPLICATION_JSON
                returning(OK, faultStrand to Swaggex.faultStrandDto())
            } bindContract GET to { faultStrandId ->
                { req ->
                    get(faultStrand) {
                        controller.lookupFaultStrandDto(faultStrandId,
                                fullStack[req] ?: false,
                                printStack[req] ?: false)
                    }
                }
            }

    private fun faultRoute() =
            "fault" / uuid(::FaultId) meta {
                summary = "Lookup a fault"
                queries += listOf(fullStack, printStack)
                produces += APPLICATION_JSON
                returning(OK, fault to Swaggex.faultDto())
            } bindContract GET to { faultId ->
                { req ->
                    get(fault) {
                        controller.lookupFaultDto(faultId,
                                fullStack[req] ?: false,
                                printStack[req] ?: false)
                    }
                }
            }

    private fun causeStrandRoute() =
            "cause-strand" / uuid(::CauseStrandId) meta {
                summary = "Lookup a cause strand"
                queries += listOf(fullStack, printStack)
                produces += APPLICATION_JSON
                returning(OK, causeStrand to Swaggex.causeStrandDto())
            } bindContract GET to { causeStrandId ->
                { req ->
                    get(causeStrand) {
                        controller.lookupCauseStrandDto(causeStrandId,
                                fullStack[req] ?: false,
                                printStack[req] ?: false)
                    }
                }
            }

    private fun causeRoute() =
            "cause" / uuid(::CauseId) meta {
                summary = "Lookup a cause"
                queries += listOf(fullStack, printStack)
                produces += APPLICATION_JSON
                returning(OK, cause to Swaggex.causeDto())
            } bindContract GET to { causeId ->
                { req ->
                    get(cause) {
                        controller.lookupCauseDto(causeId,
                                fullStack[req] ?: false,
                                printStack[req] ?: false)
                    }
                }
            }

    private fun globalLimit() =
            "feed/limit" meta {
                summary = "Event limits global"
                produces += APPLICATION_JSON
                returning(OK, limit to Swaggex.limit())
            } bindContract GET to simpleGet(limit) {
                controller.feedLimit()
            }

    private fun feedLimitsFaultRoute() =
            "feed/limit/fault" / uuid(::FaultId) meta {
                summary = "Event limits for a fault"
                produces += APPLICATION_JSON
                returning(OK, limit to Swaggex.limit())
            } bindContract GET to { faultId ->
                simpleGet(limit) {
                    controller.feedLimit(faultId)
                }
            }


    private fun faultStrandLimit() =
            "feed/limit/fault-strand" / uuid(::FaultStrandId) meta {
                summary = "Event limits for a fault strand"
                produces += APPLICATION_JSON
                returning(OK, limit to Swaggex.limit())
            } bindContract GET to { faultId ->
                simpleGet(limit) {
                    controller.feedLimit(faultId)
                }
            }

    private fun globalFeedRoute() =
            "feed" meta {
                summary = "Events global"
                produces += APPLICATION_JSON
                queries += listOf(offsetQuery, countQuery, fullStack, printStack)
                returning(OK, sequence to Swaggex.eventSequence())
            } bindContract GET to { req ->
                get(sequence) {
                    controller.feed(
                            offsetQuery[req] ?: 0L,
                            countQuery[req] ?: 0L,
                            fullStack[req] ?: false,
                            printStack[req] ?: false)
                }
            }

    private fun feedLookupFaultRoute() =
            "feed/fault" / uuid(::FaultId) meta {
                summary = "Events for a fault"
                produces += APPLICATION_JSON
                queries += listOf(offsetQuery, countQuery, fullStack, printStack, fullEvent)
                returning(OK, faultSequence to Swaggex.faultEventSequence())
            } bindContract GET to { faultId ->
                { req ->
                    get(faultSequence) {
                        controller.feed(
                                faultId,
                                offsetQuery[req] ?: 0L,
                                countQuery[req] ?: 0L,
                                fullStack[req] ?: false,
                                printStack[req] ?: false,
                                fullEvent[req] ?: false)
                    }
                }
            }

    private fun feedLookupFaultStrandRoute() =
            "feed/fault-strand" / uuid(::FaultStrandId) meta {
                summary = "Events for a fault strand"
                produces += APPLICATION_JSON
                queries += listOf(offsetQuery, countQuery, fullStack, printStack)
                returning(OK, faultStrandSequence to Swaggex.faultStrandEventSequence())
            } bindContract GET to { faultStrandId ->
                { req ->
                    get(faultStrandSequence) {
                        controller.feed(faultStrandId,
                                offsetQuery[req] ?: 0L,
                                countQuery[req] ?: 0L,
                                fullStack[req] ?: false,
                                printStack[req] ?: false)
                    }
                }
            }

    private fun <O> get(
            outLens: BiDiBodyLens<O>,
            type: ContentType = APPLICATION_JSON,
            result: () -> O?
    ): Response =
            try {
                result()?.let {
                    outLens.set(withContentType(Response(OK), type), it)
                } ?: Response(NOT_FOUND)
            } catch (e: Exception) {
                throw IllegalStateException("Failed GET", e)
            }

    private fun <O> simpleGet(
            outLens: BiDiBodyLens<O>,
            type: ContentType = APPLICATION_JSON,
            result: () -> O?
    ): (Request) -> Response = {
        try {
            result()?.let<O, Response> {
                outLens.set(withContentType(Response(OK), type), it)
            } ?: withContentType(Response(NOT_FOUND))
        } catch (e: Exception) {
            throw IllegalStateException("Failed GET", e)
        }
    }

    private fun errorFilter() = Filter { handler ->
        { request ->
            try {
                handledResponse(handler(request), request)
            } catch (e: Throwable) {
                handledException(e, request)
            }
        }
    }

    private fun contracts(): RoutingHttpHandler =
            configuration.prefix bind contract {
                renderer = OpenApi3(
                        apiInfo = ApiInfo("Unearth", "v1", "Taking exceptions seriously"),
                        json = JSON
                )
                descriptionPath =
                        "/swagger.json"
                routes +=
                        submitExceptionRoute()
                routes += listOf(
                        faultRoute(),
                        faultStrandRoute(),
                        faultEventRoute(),
                        causeRoute(),
                        causeStrandRoute()
                )
                routes += listOf(
                        globalLimit(),
                        feedLimitsFaultRoute(),
                        faultStrandLimit(),
                        globalFeedRoute(),
                        feedLookupFaultRoute(),
                        feedLookupFaultStrandRoute()
                )
                routes += listOf(
                        retrieveExceptionRoute(),
                        retrieveExceptionReduxRoute()
                )
                routes += pingRoute()
            }

    private fun handledResponse(response: Response, request: Request): Response =
            when {
                response.status.successful || response.status.redirection || response.status.clientError ->
                    response
                else ->
                    handledException(
                            RuntimeException("Internal server error: ${request.uri}"), request, response)
            }

    private fun handledException(e: Throwable, request: Request, response: Response? = null): Response {
        val message = "Exception occurred: ${request?.method ?: "?"} ${request?.uri ?: "?"}"
        return if (configuration.selfDiagnose)
            try {
                selfDiagnosedErrorResponse(e, response, response?.status)
            } catch (sde: Exception) {
                logger.warn("Failed to self-diagnose", sde)
                internalErrorResponse(e)
            } finally {
                logger.warn(message, e)
            }
        else {
            logger.error(message, e)
            internalErrorResponse(e)
        }
    }

    private fun selfDiagnosedErrorResponse(
            error: Throwable,
            response: Response? = null,
            status: Status? = INTERNAL_SERVER_ERROR
    ): Response {
        val policy = try {
            controller.submitRaw(error)
        } catch (e: Exception) {
            return bareBonesErrorResponse("Failed to submit self-diagnosed error", e)
        }
        return internalError.set(
                withContentType((response ?: Response(status ?: INTERNAL_SERVER_ERROR))
                        .header("X-Fault-SeqNo", policy?.faultSequence?.toString() ?: "-1")
                        .header("X-FaultStrand-SeqNo", policy?.faultStrandSequence?.toString() ?: "-1")
                        .header("X-SeqNo", policy?.globalSequence?.toString() ?: "-1")
                        .header("X-Fault-Id", policy?.faultId?.toHashString() ?: "-")
                        .header("X-Fault-Strand-Id", policy?.faultStrandId?.toHashString() ?: "-")
                        .header("X-Fault-Event-Id", policy?.faultEventId?.toHashString() ?: "-")),
                UnearthlyError(
                        message = error.toString(),
                        submission = policy?.let { controller.submission(it) }))
    }

    private fun internalErrorResponse(e: Throwable): Response {
        val fault = try {
            Fault.create(e)
        } catch (e: Exception) {
            return bareBonesErrorResponse("Failed to analyze failure", e)
        }
        return internalError.set(
                Response(INTERNAL_SERVER_ERROR)
                        .header("X-Fault-Id", fault?.id?.toString() ?: "Unknown")
                        .header("X-FaultStrand-Id", fault?.faultStrand?.id?.toString() ?: "Unknown"),
                simpleUnearthlyError(e))
    }

    private fun bareBonesErrorResponse(msg: String, e: Exception): Response {
        val reference = UUID.randomUUID().toString()
        logger.error(msg, e)
        return Response(INTERNAL_SERVER_ERROR)
                .header("X-${INTERNAL_SERVER_ERROR.code}-Ref", reference)
    }

    private fun simpleUnearthlyError(e: Throwable) =
            try {
                UnearthlyError(Throwables.join(e, " <= "))
            } catch (e2: Throwable) {
                logger.error("Failed to provide error for $e", e2)
                UnearthlyError(message = "Error processing failed")
            }

    private fun swaggerUiRoute(prefix: String): RoutingHttpHandler = "/doc/{path}" bind GET to {
        try {
            statik.read(it.path("path")).map { file ->
                Response(OK).body(file)
            }.orElseGet {
                swaggerRedirect(prefix)
            }
        } catch (e: Exception) {
            logger.warn("Redirecting failed swagger-ui load: $it", e)
            swaggerRedirect(prefix)
        }
    }

    private fun rerouteToSwagger(path: String, prefix: String): RoutingHttpHandler =
            path bind GET to { swaggerRedirect(prefix) }

    private fun swaggerRedirect(prefix: String) =
            Response(Status.FOUND).header("Location", "/doc/index.html?url=$prefix/swagger.json")

    private fun withContentType(response: Response, type: ContentType = APPLICATION_JSON): Response =
            response.header("Content-Type")
                    ?.let { response }
                    ?: response.header("Content-Type", type.value)

    override fun toString(): String = "${javaClass.simpleName}[$server]"

    companion object {

        private val logger = LoggerFactory.getLogger(UnearthlyServer::class.java)

        private const val swaggerUiPrefix = "META-INF/resources/webjars/swagger-ui"

        private val fullStack =
                Query.boolean().optional("fullStack", "Provide a fully destructured stack")

        private val printStack =
                Query.boolean().optional("printStack", "Provide a list of printed stacktrace elements")

        private val offsetQuery =
                Query.long().optional("offset", "Start point in feed")

        private val fullEvent =
                Query.boolean().optional("fullEvent", "Add fault data to each feed element")

        private val countQuery =
                Query.long().optional("count", "No. of elements to retrieve from start point in feed")

        private val groupsQuery =
                Query.string().multi.optional("group", "Group to collapse")

        private val swaggerUiPattern =
                Pattern.compile("^.*swagger-ui-([\\d.]+).jar!.*$")

        private val swaggerUiJarPattern =
                Pattern.compile("^.*swagger-ui/([\\d.]+)/.*$")

        private val exception: BiDiBodyLens<Throwable> = BiDiBodyLens(
                metas = emptyList(),
                contentType = TEXT_PLAIN,
                get = { msg ->
                    loggingLens(msg) {
                        ThrowableParser.parse(msg.body.payload)
                    }
                },
                setLens = { thr, msg ->
                    msg.body(Throwables.string(thr))
                })

        private fun <T : Id> uuid(read: (UUID) -> T): PathLens<T> = PathLens(
                meta = Meta(required = true, location = "path", paramMeta = ParamMeta.StringParam, name = "uuid"),
                get = { uuid ->
                    loggingLens(uuid) {
                        read(UUID.fromString(uuid))
                    }
                })

        private val limit: BiDiBodyLens<Long> = BiDiBodyLens(
                metas = emptyList(),
                contentType = TEXT_PLAIN,
                get = { msg ->
                    loggingLens(msg) {
                        java.lang.Long.parseLong(msg.body.payload.asString())
                    }
                },
                setLens = { v, msg -> msg.body(v.toString()) }
        )

        private fun <F, T> loggingLens(msg: F, transform: (F) -> T): T =
                try {
                    transform(msg)
                } catch (e: Exception) {
                    logger.warn("Exception in lens", e)
                    throw IllegalStateException("Exception in lens", e)
                }

        private val submission = Body.auto<Submission>().toLens()

        private val sequence = Body.auto<EventSequenceDto>().toLens()

        private val faultSequence = Body.auto<FaultEventSequenceDto>().toLens()

        private val faultStrandSequence = Body.auto<FaultStrandEventSequenceDto>().toLens()

        private val faultEvent = Body.auto<FaultEventDto>().toLens()

        private val fault = Body.auto<FaultDto>().toLens()

        private val faultStrand = Body.auto<FaultStrandDto>().toLens()

        private val cause = Body.auto<CauseDto>().toLens()

        private val causeChain = Body.auto<CauseChainDto>().toLens()

        private val causeStrand = Body.auto<CauseStrandDto>().toLens()

        private val internalError = Body.auto<UnearthlyError>().toLens()

        private fun swaggerUi(classLoader: ClassLoader): String {
            return classLoader.getResource(swaggerUiPrefix)?.let { url ->
                swaggerUiPattern.matcher(url.toExternalForm()).let { matcher ->
                    return if (matcher.matches())
                        swaggerUiPrefix + "/" + matcher.group(1) + "/"
                    else
                        jarSearch(url) ?: throw java.lang.IllegalStateException("No swagger-ui version found: $url")
                }
            } ?: throw IllegalStateException("No swagger-ui webjar found")
        }

        private fun jarSearch(url: URL): String? =
                jarFile(url).entries().toList().filter { entry ->
                    entry.name.contains("swagger-ui") && entry.name.contains("index.html")
                }.map { entry ->
                    swaggerUiJarPattern.matcher(entry.name)
                }.filter { matcher ->
                    matcher.matches()
                }.map { matcher ->
                    swaggerUiPrefix + "/" + matcher.group(1) + "/"
                }.first()

        private fun jarFile(url: URL): JarFile {
            val form = url.toExternalForm()
            val idx = form.indexOf("!")
            val substring =
                    form.substring(0, idx).substring(form.substring(0, idx).indexOf(":") + 1)
            return JarFile(File(URI.create(substring).toURL().file))
        }

        private val statik = Thread.currentThread().contextClassLoader.let { Statik(it, swaggerUi(it)) }
    }
}
