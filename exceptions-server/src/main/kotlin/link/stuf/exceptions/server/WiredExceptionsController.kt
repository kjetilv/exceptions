package link.stuf.exceptions.server

import link.stuf.exceptions.core.*
import link.stuf.exceptions.core.handler.DefaultThrowablesHandler
import link.stuf.exceptions.dto.*
import link.stuf.exceptions.munch.*
import link.stuf.exceptions.munch.data.CauseType
import link.stuf.exceptions.munch.ids.CauseTypeId
import link.stuf.exceptions.munch.ids.FaultEventId
import link.stuf.exceptions.munch.ids.FaultTypeId
import link.stuf.exceptions.munch.dto.ThrowableDto
import java.time.ZoneId
import java.util.*

class WiredExceptionsController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        sensor: FaultSensor
) {
    private val handler: FaultHandler = DefaultThrowablesHandler(storage, sensor)

    fun submit(throwableInBody: Throwable?): HandlingPolicy = handler.handle(throwableInBody)

    fun lookupFaultType(id: FaultTypeId, fullStack: Boolean = false): FaultTypeDto {
        val faultTypeId = storage.resolve(id.hash)
        val faultType = storage.getFaultType(faultTypeId)
        val events = storage.getEvents(faultTypeId)
        return FaultTypeDto(
                faultType.id.hash,
                events.toList().map { event ->
                    FaultEventDto(
                            event.id.hash,
                            faultType.id.hash,
                            event.globalSequence,
                            event.faultSequence,
                            event.faultTypeSequence,
                            event.time.atZone(ZoneId.of("UTC")),
                            wiredException(
                                    event.fault.toThrowableDto(), fullStack)
                    )
                }
        )
    }

    fun lookupEvent(
            id: FaultEventId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): FaultEventDto =
            storage.getFaultEvent(id).let { event ->
                FaultEventDto(
                        event.id.hash,
                        event.fault.id.hash,
                        event.globalSequence,
                        event.faultSequence,
                        event.faultTypeSequence,
                        event.time.atZone(ZoneId.of("UTC")),
                        wiredException(event.fault.toThrowableDto(), fullStack, simpleTrace))
            }

    fun lookupStack(
            causeTypeId: CauseTypeId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseDto =
            wiredStack(storage.getStack(causeTypeId), causeTypeId.hash, fullStack, simpleTrace)

    fun lookupPrintable(eventId: FaultEventId): String =
            Throwables.string(lookupThrowable(eventId))

    fun lookupThrowable(eventId: FaultEventId): Throwable =
            storage.getFaultEvent(eventId).fault.toThrowable()

    private fun wiredException(
            specimen: ThrowableDto,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): WiredException = WiredException(
            className = specimen.className,
            message = specimen.message,
            stacktrace = wiredStack(specimen.stack, specimen.stack.hash, fullStack, simpleTrace),
            cause = specimen.cause?.let { cause -> wiredException(cause, fullStack) })

    private fun wiredStack(
            throwableType: CauseType,
            stacktraceRef: UUID,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseDto {
        return CauseDto(
                throwableType.className,
                if (fullStack)
                    wiredStackTrace(throwableType.stackTrace)
                else
                    emptyList(),
                if (simpleTrace && !fullStack)
                    simpleStackTrace(throwableType.stackTrace)
                else
                    emptyList(),
                stacktraceRef)
    }

    private fun wiredStackTrace(
            stackTrace: List<StackTraceElement>
    ): List<WiredStackTraceElement>? =
            stackTrace.map { element ->
                WiredStackTraceElement(
                        classLoaderName = element.classLoaderName,
                        moduleName = element.moduleName,
                        moduleVersion = element.moduleVersion,
                        declaringClass = element.className,
                        methodName = element.methodName,
                        fileName = element.fileName,
                        lineNumber = element.lineNumber)
            }.toList()

    private fun simpleStackTrace(stackTrace: List<StackTraceElement>): List<String> =
            stackTrace.map { it.toString() }
}
