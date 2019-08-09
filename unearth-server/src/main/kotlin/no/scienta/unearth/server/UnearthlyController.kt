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
import no.scienta.unearth.munch.id.*
import no.scienta.unearth.munch.model.Cause
import no.scienta.unearth.munch.model.CauseStrand
import no.scienta.unearth.munch.model.Fault
import no.scienta.unearth.munch.model.FaultEvent
import no.scienta.unearth.munch.print.CauseFrame
import no.scienta.unearth.server.dto.*
import java.time.Clock
import java.time.ZoneId

class UnearthlyController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        private val renderer: UnearthlyRenderer,
        private val configuration: UnearthlyConfig,
        clock: Clock = Clock.systemDefaultZone()
) : AutoCloseable {
    val handler: FaultHandler = DefaultFaultHandler(storage, stats, clock)

    override fun close() {
        listOf(storage, feed, stats).forEach(AutoCloseable::close)
    }

    fun submitRaw(t: Throwable): HandlingPolicy = handler.handle(t)!!

    fun lookupFaultStrandDto(
            id: FaultStrandId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultStrandDto? =
            storage.getFaultStrand(id).orElse(null)?.let { faultStrand ->
                FaultStrandDto(
                        FaultStrandIdDto(faultStrand.hash, link(id), feed(id)),
                        faultStrand.causeStrands.map {
                            causeStrandDto(it, fullStack, printStack)
                        })
            }

    fun lookupFaultDto(
            faultId: FaultId,
            fullStack: Boolean = true,
            printStack: Boolean = false
    ): FaultDto? =
            storage.getFault(faultId).orElse(null)?.let { faultDto(it, fullStack, printStack) }

    fun lookupFaultEventDto(
            id: FaultEventId,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventDto? =
            storage.getFaultEvent(id).orElse(null)?.let { faultEventDto(it, fullStack, printStack, fullEvent) }

    fun lookupCauseStrandDto(
            causeStrandId: CauseStrandId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseStrandDto? =
            storage.getCauseStrand(causeStrandId).orElse(null)?.let { causeStrandDto(it, fullStack, printStack) }

    fun lookupCauseDto(
            causeId: CauseId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseDto? =
            storage.getCause(causeId).orElse(null)?.let { causeDto(it, fullStack, printStack) }

    fun lookupThrowable(faultId: FaultId): Throwable? =
            storage.getFault(faultId).orElse(null)?.let(Fault::toChameleon)

    fun feedLimit(faultId: FaultId): Long = feed.limit(faultId)

    fun feedLimit(faultStrandId: FaultStrandId): Long = feed.limit(faultStrandId)

    fun feedLimit() = feed.limit()

    fun feed(
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): EventSequenceDto =
            EventSequenceDto(
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
    ): FaultEventSequenceDto =
            FaultEventSequenceDto(
                    FaultIdDto(faultId.hash, link(faultId)),
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
    ): FaultStrandEventSequenceDto =
            FaultStrandEventSequenceDto(
                    FaultStrandIdDto(faultStrandId.hash, link(faultStrandId)),
                    feed.feed(faultStrandId, offset, count).map {
                        faultEventDto(it, fullStack, printStack, fullEvent)
                    })

    private fun faultEventDto(
            faultEvent: FaultEvent,
            fullStack: Boolean = false,
            printStack: Boolean = false,
            fullEvent: Boolean = false
    ): FaultEventDto =
            FaultEventDto(
                    FaultEventIdDto(faultEvent.hash, link(faultEvent)),
                    if (fullEvent) faultDto(faultEvent.fault, fullStack, printStack) else null,
                    faultEvent.time.atZone(ZoneId.of("UTC")),
                    faultEvent.globalSequenceNo,
                    faultEvent.faultSequenceNo,
                    faultEvent.faultStrandSequenceNo)

    fun reset() {
        storage.reset()
        feed.reset()
        stats.reset()
    }

    private fun faultDto(fault: Fault, fullStack: Boolean, printStack: Boolean): FaultDto =
            FaultDto(
                    id = FaultIdDto(fault.hash, link(fault), feed(fault)),
                    faultStrandId = FaultStrandIdDto(
                            fault.faultStrand.hash, link(fault.faultStrand), feed(fault.faultStrand)),
                    causes = fault.causes.map { cause ->
                        causeDto(cause, fullStack = fullStack, printStack = printStack)
                    })

    private fun causeStrandDto(
            causeStrand: CauseStrand,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): CauseStrandDto {
        return CauseStrandDto(
                CauseStrandIdDto(causeStrand.hash, link(causeStrand)),
                causeStrand.className,
                if (fullStack)
                    stackTrace(causeStrand.causeFrames)
                else
                    emptyList(),
                if (printStack && !fullStack)
                    simpleStackTrace(causeStrand.causeFrames)
                else
                    emptyList())
    }

    fun submission(handling: HandlingPolicy): Submission {
        return Submission(
                FaultStrandIdDto(
                        handling.faultStrandId.hash, link(handling.faultStrandId), feed(handling.faultStrandId)),
                FaultIdDto(
                        handling.faultId.hash, link(handling.faultId), feed(handling.faultId)),
                FaultEventIdDto(
                        handling.faultEventId.hash, link(handling.faultEventId)),
                handling.globalSequence,
                handling.faultStrandSequence,
                handling.faultSequence,
                action = Action.valueOf(handling.action.name),
                printout = toPrintout(handling))
    }

    private fun toPrintout(policy: HandlingPolicy): List<PrintoutDto> =
            renderer.rendering(policy)?.map {
                PrintoutDto(it.className, it.message, it.stack)
            } ?: emptyList()

    private fun feed(id: Identifiable<*>): String = feed(id.id)

    private fun feed(id: Id): String = "${configuration.prefix}/feed/${id.type}/${id.hash}"

    private fun link(id: Identifiable<*>): String = link(id.id)

    private fun link(id: Id): String = "${configuration.prefix}/${id.type}/${id.hash}"

    private fun causeDto(
            cause: Cause,
            fullStack: Boolean = true,
            printStack: Boolean = false
    ): CauseDto =
            CauseDto(
                    id = CauseIdDto(cause.hash, link(cause)),
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
