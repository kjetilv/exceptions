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

import no.scienta.unearth.core.*
import no.scienta.unearth.core.handler.DefaultThrowablesHandler
import no.scienta.unearth.dto.*
import no.scienta.unearth.munch.data.Cause
import no.scienta.unearth.munch.data.CauseType
import no.scienta.unearth.munch.data.ChainedFault
import no.scienta.unearth.munch.data.FaultEvent
import no.scienta.unearth.munch.id.*
import no.scienta.unearth.munch.util.Throwables
import java.time.ZoneId

class UnearthController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        sensor: FaultSensor,
        val reducer: FaultReducer
) {
    private val handler: FaultHandler = DefaultThrowablesHandler(storage, sensor)

    fun submitRaw(t: Throwable): HandlingPolicy = handler.handle(t)

    fun lookupFaultTypeDto(id: FaultTypeId,
                           fullStack: Boolean = false,
                           simpleTrace: Boolean = false,
                           offset: Long? = null,
                           count: Long? = null
    ): FaultTypeDto = storage.getFaultType(id).let { faultType ->
        FaultTypeDto(
                faultType.id,
                faultType.causeTypes.map {
                    causeTypeDto(it, fullStack, simpleTrace)
                })
    }

//        val events = storage.getEvents(faultTypeId, offset, count)
//                events.toList().map { event ->
//                    FaultEventDto(
//                            event.id.hash,
//                            faultType.id.hash,
//                            event.globalSequence,
//                            event.faultSequence,
//                            event.faultTypeSequence,
//                            event.time.atZone(ZoneId.of("UTC")),
//                            unearthedException(
//                                    event.fault.toChainedFault(), fullStack)
//                    )

    fun lookupFaultDto(
            faultId: FaultId,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): FaultDto = storage.getFault(faultId).let { fault ->
        FaultDto(
                id = fault.id,
                faultTypeId = fault.faultType.id,
                causes = fault.causes.map { cause ->
                    causeDto(cause, fullStack = fullStack, simpleTrace = simpleTrace)
                })
    }

    fun lookupFaultEventDto(
            id: FaultEventId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false,
            printout: Printout = Printout.NONE
    ): FaultEventDto = storage.getFaultEvent(id).let { faultEvent ->
        FaultEventDto(
                faultEvent.id,
                faultEvent.fault.id,
                faultEvent.fault.faultType.id,
                faultEvent.globalSequence,
                faultEvent.faultSequence,
                faultEvent.faultTypeSequence,
                faultEvent.time.atZone(ZoneId.of("UTC")),
                faultEvent.fault.causes.map { causeDto(it) },
                unearthedException(faultEvent.fault.toChainedFault(), fullStack, simpleTrace),
                when (printout) {
                    Printout.ORIGINAL -> Throwables.string(faultEvent.fault.toCameleon())
                    Printout.BOILDOWN -> "REDUCE: ${Throwables.string(faultEvent.fault.toCameleon())}"
                    else -> null
                })
    }

    fun lookupCauseTypeDto(
            causeTypeId: CauseTypeId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseTypeDto = causeTypeDto(storage.getCauseType(causeTypeId), fullStack, simpleTrace)

    fun lookupCauseDto(
            causeId: CauseId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseDto = causeDto(storage.getCause(causeId), fullStack, simpleTrace)

    fun lookupThrowable(faultId: FaultId): Throwable = storage.getFault(faultId).toCameleon()

    fun lookupThrowableRedux(faultId: FaultId): Throwable = reducer.reduce(storage.getFault(faultId)).toCameleon()

    fun feedLimitFault(faultId: FaultId): Long = feed.limit(faultId)

    fun feedLimitFaultType(faultTypeId: FaultTypeId): Long = feed.limit(faultTypeId)

    fun feedLimitGlobal() = feed.limit()

    fun faultSequenceGlobal(offset: Long, count: Long, thin: Boolean = false): FaultSequence =
            FaultSequence(
                    null,
                    SequenceType.GLOBAL,
                    feed.feed(offset, count).map(toDto(thin)))

    fun faultSequence(faultId: FaultId, offset: Long, count: Long, thin: Boolean = false): FaultSequence =
            FaultSequence(
                    faultId,
                    SequenceType.FAULT,
                    feed.feed(faultId, offset, count).map(toDto(thin)))

    fun faultSequence(faultTypeId: FaultTypeId, offset: Long, count: Long, thin: Boolean = false): FaultSequence =
            FaultSequence(
                    faultTypeId,
                    SequenceType.FAULT_TYPE,
                    feed.feed(faultTypeId, offset, count).map(toDto(thin)))

    private fun toDto(thin: Boolean = false): (FaultEvent) -> FaultEventDto = { event ->
        FaultEventDto(
                event.id,
                event.fault.id,
                event.fault.faultType.id,
                event.globalSequence,
                event.faultSequence,
                event.faultTypeSequence,
                event.time.atZone(ZoneId.systemDefault()),
                emptyList(),
                unearthedException(event.fault.toChainedFault(), thin = thin))
    }

    private fun unearthedException(
            dto: ChainedFault,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false,
            thin: Boolean = false
    ): UnearthedException = UnearthedException(
            className = dto.cause.causeType.className,
            message = dto.cause.message,
            causeType = if (thin) null else
                causeTypeDto(dto.cause.causeType, fullStack, simpleTrace),
            cause = dto.chainedCause?.let {
                unearthedException(it, fullStack)
            })

    private fun causeTypeDto(
            causeType: CauseType,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseTypeDto = CauseTypeDto(
            causeType.id,
            causeType.className,
            if (fullStack)
                stackTrace(causeType.stackTrace)
            else
                emptyList(),
            if (simpleTrace && !fullStack)
                simpleStackTrace(causeType.stackTrace)
            else
                emptyList())

    private fun causeDto(
            cause: Cause,
            fullStack: Boolean = true,
            simpleTrace: Boolean = false
    ): CauseDto = CauseDto(
            id = cause.id,
            message = cause.message,
            causeType = causeTypeDto(cause.causeType, fullStack, simpleTrace))

    private fun simpleStackTrace(stackTrace: List<StackTraceElement>): List<String> = stackTrace.map { it.toString() }

    private fun stackTrace(
            stackTrace: List<StackTraceElement>
    ): List<UnearthedStackTraceElement>? = stackTrace.map { element ->
        UnearthedStackTraceElement(
                classLoaderName = element.classLoaderName,
                moduleName = element.moduleName,
                moduleVersion = element.moduleVersion,
                declaringClass = element.className,
                methodName = element.methodName,
                fileName = element.fileName,
                lineNumber = element.lineNumber)
    }.toList()
}
