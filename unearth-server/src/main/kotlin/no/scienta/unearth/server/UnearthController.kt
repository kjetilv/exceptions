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
import no.scienta.unearth.munch.data.*
import no.scienta.unearth.munch.id.*
import java.time.ZoneId

class UnearthController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        sensor: FaultSensor,
        val reducer: FaultReducer
) {
    private val handler: FaultHandler = DefaultThrowablesHandler(storage, sensor)

    infix fun submitRaw(t: Throwable): HandlingPolicy = handler.handle(t)

    fun lookupFaultTypeDto(
            id: FaultTypeId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultTypeDto = storage.getFaultType(id).let { faultType ->
        FaultTypeDto(
                faultType.id,
                faultType.causeTypes.map {
                    causeTypeDto(it, fullStack, printStack)
                })
    }

    fun lookupFaultDto(
            faultId: FaultId,
            fullStack: Boolean = true,
            printStack: Boolean = false
    ): FaultDto = faultDto(storage.getFault(faultId), fullStack, printStack)

    fun lookupFaultEventDto(
            id: FaultEventId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultEventDto = faultEventDto(storage.getFaultEvent(id), fullStack, printStack)

    private fun faultEventDto(
            faultEvent: FaultEvent,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultEventDto = FaultEventDto(
            faultEvent.id,
            faultDto(faultEvent.fault, fullStack, printStack),
            faultEvent.time.atZone(ZoneId.of("UTC")),
            faultEvent.globalSequence,
            faultEvent.faultSequence,
            faultEvent.faultTypeSequence)

    fun lookupCauseTypeDto(
            causeTypeId: CauseTypeId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseTypeDto = causeTypeDto(storage.getCauseType(causeTypeId), fullStack, printStack)

    fun lookupCauseDto(
            causeId: CauseId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseDto = causeDto(storage.getCause(causeId), fullStack, printStack)

    infix fun lookupThrowable(faultId: FaultId): Throwable = storage.getFault(faultId).toCameleon()

    infix fun lookupThrowableRedux(faultId: FaultId): Throwable = reducer.reduce(storage.getFault(faultId)).toCameleon()

    infix fun feedLimitFault(faultId: FaultId): Long = feed.limit(faultId)

    infix fun feedLimitFaultType(faultTypeId: FaultTypeId): Long = feed.limit(faultTypeId)

    fun feedLimitGlobal() = feed.limit()

    fun faultSequenceGlobal(
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultSequence = FaultSequence(
            null,
            SequenceType.GLOBAL,
            feed.feed(offset, count).map { faultEventDto(it, fullStack, printStack) }
    )

    fun faultSequence(
            faultId: FaultId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultSequence = FaultSequence(
            faultId,
            SequenceType.FAULT,
            feed.feed(faultId, offset, count).map { faultEventDto(it, fullStack, printStack) }
    )

    fun faultSequence(
            faultTypeId: FaultTypeId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultSequence = FaultSequence(
            faultTypeId,
            SequenceType.FAULT_TYPE,
            feed.feed(faultTypeId, offset, count).map { faultEventDto(it, fullStack, printStack) }
    )

    private fun unearthedException(
            dto: ChainedFault,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            thin: Boolean = false
    ): UnearthedException = UnearthedException(
            className = dto.cause.causeType.className,
            message = dto.cause.message,
            causeType = if (thin) null else
                causeTypeDto(dto.cause.causeType, fullStack, printStack),
            cause = dto.chainedCause?.let {
                unearthedException(it, fullStack)
            })

    private fun faultDto(fault: Fault, fullStack: Boolean, printStack: Boolean): FaultDto {
        return FaultDto(
                id = fault.id,
                faultTypeId = fault.faultType.id,
                causes = fault.causes.map { cause ->
                    causeDto(cause, fullStack = fullStack, printStack = printStack)
                })
    }

    private fun causeTypeDto(
            causeType: CauseType,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseTypeDto = CauseTypeDto(
            causeType.id,
            causeType.className,
            if (fullStack)
                stackTrace(causeType.stackTrace)
            else
                emptyList(),
            if (printStack && !fullStack)
                simpleStackTrace(causeType.stackTrace)
            else
                emptyList())

    private fun causeDto(
            cause: Cause,
            fullStack: Boolean = true,
            printStack: Boolean = false
    ): CauseDto = CauseDto(
            id = cause.id,
            message = cause.message,
            causeType = causeTypeDto(cause.causeType, fullStack, printStack))

    private fun simpleStackTrace(stackTrace: List<StackTraceElement>): List<String> = stackTrace.map { it.toString() }

    private fun stackTrace(
            stackTrace: List<StackTraceElement>
    ): List<StackTraceElementDto>? = stackTrace.map { element ->
        StackTraceElementDto(
                classLoaderName = element.classLoaderName,
                moduleName = element.moduleName,
                moduleVersion = element.moduleVersion,
                declaringClass = element.className,
                methodName = element.methodName,
                fileName = element.fileName,
                lineNumber = element.lineNumber)
    }.toList()
}
