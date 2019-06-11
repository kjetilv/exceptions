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
import no.scienta.unearth.munch.data.CauseType
import no.scienta.unearth.munch.data.ChainedFault
import no.scienta.unearth.munch.data.Fault
import no.scienta.unearth.munch.data.FaultEvent
import no.scienta.unearth.munch.id.CauseTypeId
import no.scienta.unearth.munch.id.FaultEventId
import no.scienta.unearth.munch.id.FaultId
import no.scienta.unearth.munch.id.FaultTypeId
import no.scienta.unearth.munch.util.Throwables
import java.time.ZoneId
import java.util.*

class UnearthController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        sensor: FaultSensor
) {
    private val handler: FaultHandler = DefaultThrowablesHandler(storage, sensor)

    fun submit(t: Throwable): HandlingPolicy = handler.handle(t)

    fun submit(fault: FaultDto): HandlingPolicy = handler.handle(toFault(fault))

    fun lookupFaultType(id: FaultTypeId,
                        fullStack: Boolean = false,
                        offset: Long? = null,
                        count: Long? = null
    ): FaultTypeDto {
        val faultTypeId = storage.resolveFaultType(id.hash)
        val faultType = storage.getFaultType(faultTypeId)
        val events = storage.getEvents(faultTypeId, offset, count)
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
                            unearthedException(
                                    event.fault.toChainedFault(), fullStack)
                    )
                }
        )
    }

    fun lookupEvent(
            id: FaultEventId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false,
            printout: Printout = Printout.NONE
    ): FaultEventDto = storage.getFaultEvent(id).let { event ->
        FaultEventDto(
                event.id.hash,
                event.fault.id.hash,
                event.globalSequence,
                event.faultSequence,
                event.faultTypeSequence,
                event.time.atZone(ZoneId.of("UTC")),
                unearthedException(event.fault.toChainedFault(), fullStack, simpleTrace),
                when (printout) {
                    Printout.ORIGINAL -> Throwables.string(event.fault.toCameleon())
                    Printout.BOILDOWN -> "REDUCE: ${Throwables.string(event.fault.toCameleon())}"
                    else -> null
                })
    }

    fun lookupCause(
            causeTypeId: CauseTypeId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseDto = unearthedStack(storage.getStack(causeTypeId), causeTypeId, fullStack, simpleTrace)

    fun lookupFault(uuid: UUID): Throwable = storage.getFault(storage.resolveFault(uuid)).toCameleon()

    fun feedLimit(type: SequenceType, uuid: UUID): Long =
            when (type) {
                SequenceType.FAULT -> feed.limit(FaultId(uuid))
                SequenceType.FAULT_TYPE -> feed.limit(FaultTypeId(uuid))
                else -> feedLimit()
            }

    fun feedLimit() = feed.limit()

    fun faultSequence(
            offset: Long,
            count: Long,
            thin: Boolean = false
    ): FaultSequence = faultSequence(SequenceType.GLOBAL, null, offset, count, thin)

    fun faultSequence(
            type: SequenceType,
            uuid: UUID?,
            offset: Long,
            count: Long,
            thin: Boolean = false
    ): FaultSequence = FaultSequence(
            type,
            events(type, uuid, offset, count).map(toDto(thin)))

    private fun toFault(fault: FaultDto): Fault = TODO("Haven't started accepting fault dto's yet!")

    private fun toDto(thin: Boolean = false): (FaultEvent) -> FaultEventDto = { event ->
        FaultEventDto(
                event.fault.hash,
                event.fault.faultType.hash,
                event.globalSequence,
                event.faultSequence,
                event.faultTypeSequence,
                event.time.atZone(ZoneId.systemDefault()),
                unearthedException(event.fault.toChainedFault(), thin = thin))
    }

    private fun events(type: SequenceType, uuid: UUID?, offset: Long, count: Long): List<FaultEvent> =
            when (type) {
                SequenceType.FAULT_TYPE -> feed.feed(FaultTypeId(uuid), offset, count)
                SequenceType.FAULT -> feed.feed(FaultId(uuid), offset, count)
                else -> feed.feed(offset, count)
            } ?: emptyList()

    private fun unearthedException(
            dto: ChainedFault,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false,
            thin: Boolean = false
    ): UnearthedException = UnearthedException(
            className = dto.causeType.className,
            message = dto.message,
            stacktrace = if (thin) null else unearthedStack(dto.causeType, dto.causeType.id, fullStack, simpleTrace),
            stacktraceId = dto.causeType.hash,
            cause = dto.cause?.let { cause ->
                unearthedException(cause, fullStack)
            })

    private fun unearthedStack(
            throwableType: CauseType,
            causeTypeId: CauseTypeId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseDto = CauseDto(
            if (fullStack)
                stackTrace(throwableType.stackTrace)
            else
                emptyList(),
            if (simpleTrace && !fullStack)
                simpleStackTrace(throwableType.stackTrace)
            else
                emptyList(),
            causeTypeId.hash)

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
