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
import no.scienta.unearth.munch.id.*
import no.scienta.unearth.munch.model.*
import no.scienta.unearth.munch.print.PackageGrouper
import no.scienta.unearth.munch.print.StackTraceReshaper
import java.time.ZoneId
import java.util.*

class UnearthController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        sensor: FaultSensor,
        val reducer: FaultReducer
) {
    private val handler: FaultHandler = DefaultThrowablesHandler(storage, sensor)

    infix fun submitRaw(t: Throwable): HandlingPolicy = handler.handle(t)

    fun lookupFaultStrandDto(
            id: FaultStrandId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultStrandDto = storage.getFaultStrand(id).let { faultStrand ->
        FaultStrandDto(
                faultStrand.id,
                faultStrand.causeStrands.map {
                    causeStrandDto(it, fullStack, printStack)
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

    fun lookupCauseStrandDto(
            causeStrandId: CauseStrandId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseStrandDto = causeStrandDto(storage.getCauseStrand(causeStrandId), fullStack, printStack)

    fun lookupCauseDto(
            causeId: CauseId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseDto = causeDto(storage.getCause(causeId), fullStack, printStack)

    infix fun lookupThrowable(faultId: FaultId): Throwable = storage.getFault(faultId).toCameleon()

    infix fun feedLimit(faultId: FaultId): Long = feed.limit(faultId)

    infix fun feedLimit(faultStrandId: FaultStrandId): Long = feed.limit(faultStrandId)

    fun feedLimit() = feed.limit()

    fun feed(
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultEventSequence = FaultEventSequence(
            null,
            SequenceType.GLOBAL,
            feed.feed(offset, count).map {
                faultEventDto(it, fullStack, printStack)
            })

    fun feed(
            faultId: FaultId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultEventSequence = FaultEventSequence(
            faultId,
            SequenceType.FAULT,
            feed.feed(faultId, offset, count).map {
                faultEventDto(it, fullStack, printStack)
            })

    fun feed(
            faultStrandId: FaultStrandId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultEventSequence = FaultEventSequence(
            faultStrandId,
            SequenceType.FAULT_STRAND,
            feed.feed(faultStrandId, offset, count).map {
                faultEventDto(it, fullStack, printStack)
            })

    private fun faultEventDto(
            faultEvent: FaultEvent,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultEventDto = FaultEventDto(
            faultEvent.id,
            faultDto(faultEvent.fault, fullStack, printStack),
            faultEvent.time.atZone(ZoneId.of("UTC")),
            faultEvent.globalSequenceNo,
            faultEvent.faultSequenceNo,
            faultEvent.faultStrandSequenceNo)

    fun rewriteThrowable(faultId: FaultId, groups: Collection<String>?): CauseChainDto {
        val stackTraceReshaper = StackTraceReshaper.create()
                .group(PackageGrouper(Collections.singleton(groups)))
                .reshape({ cf -> StackTraceReshaper.shortenClassname(cf) })
        return causeChainDto(
                storage.getFault(faultId).toCauseChain()
                        .rewriteStackTrace(stackTraceReshaper::prettified))
    }

    private fun causeChainDto(
            chain: CauseChain,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            thin: Boolean = false
    ): CauseChainDto = CauseChainDto(
            className = chain.className,
            message = chain.message,
            printedCauseFrames = if (chain.printedCauseFrames.isEmpty()) null else chain.printedCauseFrames,
            causeStrand = if (thin) null else
                causeStrandDto(chain.cause.causeStrand, fullStack, printStack),
            cause = chain.causeChain?.let {
                causeChainDto(it, fullStack)
            })

    private fun faultDto(fault: Fault, fullStack: Boolean, printStack: Boolean): FaultDto {
        return FaultDto(
                id = fault.id,
                faultStrandId = fault.faultStrand.id,
                causes = fault.causes.map { cause ->
                    causeDto(cause, fullStack = fullStack, printStack = printStack)
                })
    }

    private fun causeStrandDto(
            causeStrand: CauseStrand,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseStrandDto = CauseStrandDto(
            causeStrand.id,
            causeStrand.className,
            if (fullStack)
                stackTrace(causeStrand.causeFrames)
            else
                emptyList(),
            if (printStack && !fullStack)
                simpleStackTrace(causeStrand.causeFrames)
            else
                emptyList())

    private fun causeDto(
            cause: Cause,
            fullStack: Boolean = true,
            printStack: Boolean = false
    ): CauseDto = CauseDto(
            id = cause.id,
            message = cause.message,
            causeStrand = causeStrandDto(cause.causeStrand, fullStack, printStack))

    private fun simpleStackTrace(stackTrace: List<CauseFrame>): List<String> = stackTrace.map { it.toString() }

    private fun stackTrace(
            stackTrace: List<CauseFrame>
    ): List<StackTraceElementDto>? = stackTrace.map { element ->
        StackTraceElementDto(
                classLoaderName = element.classLoader(),
                moduleName = element.module(),
                moduleVersion = element.moduleVer(),
                declaringClass = element.className(),
                methodName = element.method(),
                fileName = element.file(),
                lineNumber = element.line())
    }.toList()
}
