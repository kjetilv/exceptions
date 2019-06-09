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
import no.scienta.unearth.munch.data.FaultEvent
import no.scienta.unearth.munch.data.ThrowableDto
import no.scienta.unearth.munch.ids.CauseTypeId
import no.scienta.unearth.munch.ids.FaultEventId
import no.scienta.unearth.munch.ids.FaultId
import no.scienta.unearth.munch.ids.FaultTypeId
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

    fun submit(throwableInBody: Throwable?): HandlingPolicy = handler.handle(throwableInBody)

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
                        unearthedException(event.fault.toThrowableDto(), fullStack, simpleTrace))
            }

    fun lookupStack(
            causeTypeId: CauseTypeId,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): CauseDto =
            unearthedStack(storage.getStack(causeTypeId), causeTypeId.hash, fullStack, simpleTrace)

    fun lookupPrintable(eventId: FaultEventId): String =
            Throwables.string(storage.getFaultEvent(eventId).fault.toThrowable())

    fun lookupFault(uuid: UUID): Throwable =
            storage.getFault(storage.resolveFault(uuid)).toThrowable()

    fun feedLimit(type: SequenceType, uuid: UUID): Long =
            when (type) {
                SequenceType.FAULT -> feed.limit(FaultId(uuid))
                SequenceType.FAULT_TYPE -> feed.limit(FaultTypeId(uuid))
                else -> feed.limit()
            }

    fun faultSequence(
            type: SequenceType,
            uuid: UUID,
            offset: Long,
            count: Long,
            thin: Boolean = false
    ): FaultSequence = FaultSequence(
            type,
            events(type, uuid, offset, count).map { event ->
                FaultEventDto(
                        event.fault.hash,
                        event.fault.faultType.hash,
                        event.globalSequence,
                        event.faultSequence,
                        event.faultTypeSequence,
                        event.time.atZone(ZoneId.systemDefault()),
                        unearthedException(event.fault.toThrowableDto()))
            })

    private fun events(type: SequenceType, uuid: UUID, offset: Long, count: Long): List<FaultEvent> =
            when (type) {
                SequenceType.FAULT_TYPE -> feed.feed(FaultTypeId(uuid), offset, count)
                SequenceType.FAULT -> feed.feed(FaultId(uuid), offset, count)
                else -> feed.feed(offset, count)
            }!!

    private fun unearthedException(
            dto: ThrowableDto,
            fullStack: Boolean = false,
            simpleTrace: Boolean = false
    ): UnearthedException = UnearthedException(
            className = dto.causeType.className,
            message = dto.message,
            stacktrace = unearthedStack(dto.causeType, dto.causeType.hash, fullStack, simpleTrace),
            cause = dto.cause?.let { cause ->
                unearthedException(cause, fullStack)
            })

    private fun unearthedStack(
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
    ): List<UnearthedStackTraceElement>? =
            stackTrace.map { element ->
                UnearthedStackTraceElement(
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
