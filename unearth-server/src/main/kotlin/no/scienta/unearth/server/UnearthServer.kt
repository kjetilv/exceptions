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
import no.scienta.unearth.munch.id.CauseTypeId
import no.scienta.unearth.munch.id.FaultEventId
import no.scienta.unearth.munch.id.FaultTypeId
import no.scienta.unearth.munch.util.Throwables
import no.scienta.unearth.server.JSON.auto
import no.scienta.unearth.statik.Statik
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
import java.time.ZonedDateTime
import java.util.*
import java.util.regex.Pattern

class UnearthServer(
        private val configuration: UnearthConfig = UnearthConfig(),
        val controller: UnearthController
) {
    private val logger: Logger = LoggerFactory.getLogger(UnearthServer::class.java)

    private fun submitPrintedExceptionRoute() = "/throwable" meta {
        summary = "Submit an exception"
        consumes += ContentType.TEXT_PLAIN
        produces += ContentType.APPLICATION_JSON
        receiving(exception to Example.exception())
        returning(Status.OK, submission to Example.submission())
    } bindContract Method.POST to bodyExchange(
            exception, submission, ::submitPrintedException
    )

    private fun submitStructuredFaultRoute() = "/fault" meta {
        summary = "Submit an exception"
        consumes += ContentType.APPLICATION_JSON
        produces += ContentType.APPLICATION_JSON
        receiving(exception to Example.exception())
        returning(Status.OK, submission to Example.submission())
    } bindContract Method.POST to bodyExchange(
            fault, submission, ::submitStructuredFault
    )

    private fun lookupFaultRoute() = "/fault" / uuidPath meta {
        summary = "Lookup an exception"
        queries += listOf(fullStack, simpleTrace, printout)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, faultEvent to Example.faultEventDtos())
    } bindContract Method.GET to { uuid ->
        { req ->
            responseWith(faultEvent) {
                lookupFault(
                        uuid,
                        isSet(req, fullStack),
                        isSet(req, simpleTrace),
                        printout[req] ?: Printout.NONE
                )
            }
        }
    }

    private fun lookupFaultsRoute() = "/fault-type" / uuidPath meta {
        summary = "Lookup a fault type, with fault events"
        queries += listOf(fullStack, simpleTrace, offsetQuery, countQuery)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, faultType to Example.faultTypeDto())
    } bindContract Method.GET to { uuid ->
        { req ->
            responseWith(faultType) {
                lookupFaults(uuid, isSet(req, fullStack), offsetQuery[req], countQuery[req])
            }
        }
    }

    private fun lookupCauseRoute() = "/cause" / uuidPath meta {
        summary = "Lookup a stack"
        queries += listOf(fullStack, simpleTrace)
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK, cause to Example.stack())
    } bindContract Method.GET to { uuid ->
        { req ->
            responseWith(cause) {
                lookupCause(
                        uuid,
                        isSet(req, fullStack, true),
                        isSet(req, simpleTrace))
            }
        }
    }

    private fun printFaultRoute() = "/fault/" / uuidPath meta {
        summary = "Print an exception"
        produces += ContentType.TEXT_PLAIN
        queries += listOf(printout)
        returning(Status.OK, exception to Example.exception())
    } bindContract Method.GET to { uuid ->
        {
            responseWith(exception, type = ContentType.TEXT_PLAIN) {
                lookupThrowable(uuid)
            }
        }
    }

    private fun feedLimitsRoute() = "/feed/limit" / sequenceType / uuidPath meta {
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

    private fun feedLimitsGlobalRoute() = "/feed/limit" meta {
        summary = "Event limits"
        produces += ContentType.APPLICATION_JSON
        returning(Status.OK)
    } bindContract Method.GET to {
        responseWith {
            lookupFeedLimit().toString()
        }
    }

    private fun feedLookupRoute() = "/feed" / sequenceType / uuidPath / offsetPath meta {
        summary = "Events"
        produces += ContentType.APPLICATION_JSON
        queries += listOf(countQuery, thinFeedQuery)
        returning(Status.OK, faultSequence to Example.faultSequence())
    } bindContract Method.GET to { type, uuid, offset ->
        { req ->
            responseWith(faultSequence) {
                lookupFaultSequence(
                        type,
                        uuid,
                        offset,
                        countQuery[req] ?: 0L,
                        isSet(req, thinFeedQuery))
            }
        }
    }

    private fun feedLookupGlobalRoute() = "/feed" / offsetPath meta {
        summary = "Events"
        produces += ContentType.APPLICATION_JSON
        queries += listOf(countQuery, thinFeedQuery)
        returning(Status.OK, faultSequence to Example.faultSequence())
    } bindContract Method.GET to { offset ->
        { req ->
            responseWith(faultSequence) {
                lookupFaultSequence(
                        offset,
                        countQuery[req] ?: 0L,
                        isSet(req, thinFeedQuery))
            }
        }
    }

    private fun <I, O> bodyExchange(
            inLens: BiDiBodyLens<I>,
            outLens: BiDiBodyLens<O>,
            accept: (I) -> O
    ) = { req: Request ->
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
    ): Response = outLens.set(withContentType(Response(Status.OK), type), result())

    private fun responseWith(
            type: ContentType = ContentType.APPLICATION_JSON,
            result: () -> String
    ): Response = withContentType(Response(Status.OK), type).body(result())

    fun start(after: (Http4kServer) -> Unit = {}): UnearthServer = apply {
        server.start()
        after(server)
    }

    fun stop(after: (Http4kServer) -> Unit = {}): UnearthServer = apply {
        server.stop()
        after(server)
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
                                submitPrintedExceptionRoute(),
                                submitStructuredFaultRoute(),
                                lookupFaultRoute(),
                                lookupFaultsRoute(),
                                lookupCauseRoute(),
                                feedLimitsGlobalRoute(),
                                feedLookupGlobalRoute(),
                                feedLimitsRoute(),
                                feedLookupRoute(),
                                printFaultRoute())
                    },
                    rerouteToSwagger("/doc", configuration.prefix),
                    rerouteToSwagger("/", configuration.prefix),
                    swaggerUiRoute(configuration.prefix)))
            .asServer(
                    NettyConfig(configuration.host, configuration.port)
            )

    private fun submitPrintedException(throwable: Throwable) = controller.submit(throwable).let(::submission)

    private fun submitStructuredFault(throwable: FaultDto) = controller.submit(throwable).let(::submission)

    private fun submission(it: HandlingPolicy) = Submission(
            it.faultTypeId.hash, it.faultId.hash, it.faultEventId.hash,
            it.globalSequence, it.faultTypeSequence, it.faultSequence,
            it.isLoggable)

    private fun lookupFault(
            uuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false,
            printout: Printout = Printout.NONE
    ): FaultEventDto = controller.lookupEvent(FaultEventId(uuid), fullStack, simpleTrace, printout)

    private fun lookupFaults(
            uuid: UUID,
            fullStack: Boolean = true,
            offset: Long?,
            count: Long?
    ): FaultTypeDto = controller.lookupFaultType(FaultTypeId(uuid), fullStack, offset, count)

    private fun lookupCause(
            pathUuid: UUID,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): CauseDto = controller.lookupCause(CauseTypeId(pathUuid), fullStack, simpleTrace)

    private fun lookupFeedLimit(): Long = controller.feedLimit()

    private fun lookupFeedLimit(type: SequenceType, uuid: UUID): Long = controller.feedLimit(type, uuid)

    private fun lookupFaultSequence(
            offset: Long,
            count: Long,
            thin: Boolean
    ): FaultSequence = controller.faultSequence(offset, count, thin)

    private fun lookupFaultSequence(
            type: SequenceType,
            uuid: UUID,
            offset: Long,
            count: Long,
            thin: Boolean
    ): FaultSequence = controller.faultSequence(type, uuid, offset, count, thin)

    private fun lookupThrowable(uuid: UUID): Throwable = controller.lookupFault(uuid)

    private fun errors(): (HttpHandler) -> (Request) -> Response =
            { next ->
                { req ->
                    handleErrors(next, req)
                }
            }

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
        val handle = controller.submit(e)
        logger.error("Failed: ${handle.faultEventId}", e)
        return internalError.set(
                withContentType(Response(Status.INTERNAL_SERVER_ERROR)),
                UnearthInternalError(
                        message = e.toString(),
                        storedAs = handle.faultEventId.hash
                ))
    }

    private fun simpleFailedResponse(e: Throwable): Response {
        val faultTypeId = FaultType.create(e).id
        logger.error("Failed: $faultTypeId", e)
        return internalError.set(
                Response(Status.INTERNAL_SERVER_ERROR),
                UnearthInternalError(
                        message = e.toString()
                ))
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

        private val fullStack = Query.boolean().optional("fullStack")

        private val simpleTrace = Query.boolean().optional("simpleTrace")

        private val offsetQuery = Query.long().optional("offset")

        private val countQuery = Query.long().optional("count")

        private val thinFeedQuery = Query.boolean().optional("thin")

        private val offsetPath = Path.long().of("offset")

        private val uuidPath = Path.uuid().of("uuid")

        private val swaggerUiPattern = Pattern.compile("^.*swagger-ui-([\\d.]+).jar!.*$")

        private const val swaggerUiPrefix = "META-INF/resources/webjars/swagger-ui/"

        private val submission = Body.auto<Submission>().toLens()

        private val sequenceType: PathLens<SequenceType> = PathLens(
                meta = Meta(true, "path", ParamMeta.StringParam, "type", "Sequence type"),
                get = { SequenceType.valueOf(it.toUpperCase()) })

        private val printout: QueryLens<Printout?> = QueryLens(
                meta = Meta(true, "path", ParamMeta.StringParam, "type", "Sequence type"),
                lensGet = { req ->
                    req.query("printout")?.let { s ->
                        Printout.valueOf(s.toUpperCase())
                    }
                })

        private val faultSequence = Body.auto<FaultSequence>().toLens()

        private val faultEvent = Body.auto<FaultEventDto>().toLens()

        private val fault = Body.auto<FaultDto>().toLens()

        private val reducedException: BiDiBodyLens<Throwable> = BiDiBodyLens(
                metas = emptyList(),
                contentType = ContentType.TEXT_PLAIN,
                get = { msg ->
                    ThrowableParser.parse(msg.body.payload)
                },
                setLens = { thr, msg ->
                    msg.body(Throwables.string(thr))
                })

        private val exception: BiDiBodyLens<Throwable> = BiDiBodyLens(
                metas = emptyList(),
                contentType = ContentType.TEXT_PLAIN,
                get = { msg ->
                    ThrowableParser.parse(msg.body.payload)
                },
                setLens = { thr, msg ->
                    msg.body(Throwables.string(thr))
                })

        private val faultType = Body.auto<FaultTypeDto>().toLens()

        private val cause = Body.auto<CauseDto>().toLens()

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

    object Example {

        private val random = Random()

        internal fun submission() = Submission(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                random.nextLong() % 1000,
                1000L + random.nextLong() % 1000,
                2000L + random.nextLong() % 1000,
                random.nextBoolean())

        internal fun faultSequence(): FaultSequence = FaultSequence(SequenceType.FAULT, listOf(faultEventDtos()))

        internal fun faultEventDtos(): FaultEventDto = faultEventDtos(uuid())

        internal fun faultTypeDto() = uuid().let { FaultTypeDto(it, listOf(faultEventDtos(it))) }

        internal fun exception(): Throwable = RuntimeException("Example throwable")

        internal fun stack() = CauseDto(emptyList(), emptyList(), uuid())

        private fun unearthedException(): UnearthedException =
                UnearthedException(
                        "mymy.such.a.BadClass",
                        "Bad class!",
                        stacktraceId = uuid(),
                        stacktrace = stack())

        private fun uuid(): UUID = UUID.randomUUID()

        private fun faultEventDtos(faultTypeId: UUID): FaultEventDto = FaultEventDto(uuid(),
                faultTypeId,
                random.nextLong(),
                random.nextLong(),
                random.nextLong(),
                ZonedDateTime.now(),
                unearthedException())
    }
}
