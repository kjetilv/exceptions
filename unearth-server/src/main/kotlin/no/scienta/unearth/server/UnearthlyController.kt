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

package no.scienta.unearth.server

import no.scienta.unearth.core.*
import no.scienta.unearth.core.handler.DefaultFaultHandler
import no.scienta.unearth.dto.*
import no.scienta.unearth.munch.id.*
import no.scienta.unearth.munch.model.Cause
import no.scienta.unearth.munch.model.CauseStrand
import no.scienta.unearth.munch.model.Fault
import no.scienta.unearth.munch.model.FaultEvent
import no.scienta.unearth.munch.print.*
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.ZoneId

class UnearthlyController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        stats: FaultStats,
        sensor: FaultSensor,
        private val renderer: UnearthlyRenderer
) {
    val handler: FaultHandler = DefaultFaultHandler(storage, stats, sensor, Clock.systemUTC());

    private val submitLogger = LoggerFactory.getLogger(javaClass)

    fun submitRaw(t: Throwable): HandlingPolicy = handler.handle(t)!!

    private fun logged(handle: HandlingPolicy): HandlingPolicy {
        submitLogger.info(logMessage(handle))
        return handle;
    }

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
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventDto = faultEventDto(storage.getFaultEvent(id), fullStack, printStack, fullEvent)

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

    fun lookupThrowable(faultId: FaultId): Throwable = storage.getFault(faultId).toChameleon()

    fun feedLimit(faultId: FaultId): Long = feed.limit(faultId)

    fun feedLimit(faultStrandId: FaultStrandId): Long = feed.limit(faultStrandId)

    fun feedLimit() = feed.limit()

    fun feed(
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventSequence =
            FaultEventSequence(
                    null,
                    SequenceType.GLOBAL,
                    feed.feed(offset, count).map {
                        faultEventDto(it, fullStack, printStack, fullEvent)
                    })

    fun feed(
            faultId: FaultId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventSequence =
            FaultEventSequence(
                    faultId,
                    SequenceType.FAULT,
                    feed.feed(faultId, offset, count).map {
                        faultEventDto(it, fullStack, printStack, fullEvent)
                    })

    fun feed(
            faultStrandId: FaultStrandId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventSequence =
            FaultEventSequence(
                    faultStrandId,
                    SequenceType.FAULT_STRAND,
                    feed.feed(faultStrandId, offset, count).map {
                        faultEventDto(it, fullStack, printStack, fullEvent)
                    })

    fun render(policy: HandlingPolicy): CausesRendering? = renderer.rendering(policy)

    private fun logMessage(handle: HandlingPolicy) =
            "${handle.summary?.let { "$it " }
                    ?: ""}F:${handle.faultId.hash} E:${handle.faultEventId.hash} FS:${handle.faultStrandId.hash}"

    private fun faultEventDto(
            faultEvent: FaultEvent,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventDto =
            FaultEventDto(
                    faultEvent.id,
                    if (fullEvent) faultDto(faultEvent.fault, fullStack, printStack) else null,
                    faultEvent.time.atZone(ZoneId.of("UTC")),
                    faultEvent.globalSequenceNo,
                    faultEvent.faultSequenceNo,
                    faultEvent.faultStrandSequenceNo)

    fun rewriteThrowable(faultId: FaultId, groups: Collection<String>?): CauseChainDto {
        val fault = storage.getFault(faultId)
        val renderer = SimpleCausesRenderer(ConfigurableStackRenderer().group(
                SimplePackageGrouper(groups?.toList())))
        return causeChainDto(renderer.render(fault));
    }

    private fun causeChainDto(
            chain: CausesRendering,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            thin: Boolean = false
    ): CauseChainDto =
            CauseChainDto(
                    className = chain.className,
                    message = chain.message,
                    printedCauseFrames = if (chain.stack.isEmpty()) null else chain.stack,
//                    causeStrand = if (thin) null else
//                        causeStrandDto(chain.cause, fullStack, printStack),
                    cause = chain.cause?.let {
                        causeChainDto(it, fullStack)
                    })

    private fun faultDto(fault: Fault, fullStack: Boolean, printStack: Boolean): FaultDto =
            FaultDto(
                    id = fault.id,
                    faultStrandId = fault.faultStrand.id,
                    causes = fault.causes.map { cause ->
                        causeDto(cause, fullStack = fullStack, printStack = printStack)
                    })

    private fun causeStrandDto(
            causeStrand: CauseStrand,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseStrandDto =
            CauseStrandDto(
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
    ): CauseDto =
            CauseDto(
                    id = cause.id,
                    message = cause.message,
                    causeStrand = causeStrandDto(cause.causeStrand, fullStack, printStack))

    private fun simpleStackTrace(stackTrace: List<CauseFrame>): List<String> =
            stackTrace.map { it.toStackTraceElement().toString() }

    private fun stackTrace(
            stackTrace: List<CauseFrame>
    ): List<StackTraceElementDto>? =
            stackTrace.map { element ->
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
