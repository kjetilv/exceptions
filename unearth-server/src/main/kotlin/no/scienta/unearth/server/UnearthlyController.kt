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
import no.scienta.unearth.munch.model.FeedEntry
import no.scienta.unearth.munch.print.CauseFrame
import no.scienta.unearth.server.dto.*
import java.time.Clock
import java.time.ZoneId
import java.util.*

class UnearthlyController(
        private val storage: FaultStorage,
        private val feed: FaultFeed,
        private val stats: FaultStats,
        private val sensor: FaultSensor,
        private val renderer: UnearthlyRenderer,
        private val configuration: UnearthlyConfig,
        clock: Clock = Clock.systemDefaultZone()
) : AutoCloseable {

    init {
        try {
            storage.initStorage().run()
        } catch (e: Exception) {
            throw IllegalStateException("$this failed to init $storage", e)
        }
    }

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

    fun lookupFeedEntryDto(
            id: FeedEntryId,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FeedEntryDto? =
            storage.getFeedEntry(id).orElse(null)?.let { feedEntryDto(it, fullStack, printStack) }

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
            storage.getFault(faultId).map(Fault::toChameleon).orElse(null)

    fun feedLimit(faultId: FaultId): OptionalLong = feed.limit(faultId)

    fun feedLimit(faultStrandId: FaultStrandId): OptionalLong = feed.limit(faultStrandId)

    fun feedLimit(): OptionalLong = feed.limit()

    fun feed(
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): EventSequenceDto =
            EventSequenceDto(
                    feed.feed(offset, count).map {
                        feedEntryDto(it, fullStack, printStack)
                    })

    fun feed(
            faultId: FaultId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FeedEntrySequence =
            FeedEntrySequence(
                    FaultIdDto(faultId.hash, link(faultId)),
                    feed.feed(faultId, offset, count).map {
                        feedEntryDto(it, fullStack, printStack)
                    })

    fun feed(
            faultStrandId: FaultStrandId,
            offset: Long,
            count: Long,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FaultStrandEventSequenceDto =
            FaultStrandEventSequenceDto(
                    FaultStrandIdDto(faultStrandId.hash, link(faultStrandId)),
                    feed.feed(faultStrandId, offset, count).map {
                        feedEntryDto(it, fullStack, printStack)
                    })

    private fun feedEntryDto(
            feedEntry: FeedEntry,
            fullStack: Boolean = false,
            printStack: Boolean = false
    ): FeedEntryDto {
        val fault =
                storage.getFault(feedEntry.faultEvent.faultId).orElseThrow {
                    IllegalStateException()
                }
        val faultDto =
                faultDto(fault, fullStack, printStack)
        val faultEventDto =
                FaultEventDto(
                        FeedEntryIdDto(feedEntry.hash, link(feedEntry)),
                        faultDto,
                        faultDto.id,
                        faultDto.faultStrandId,
                        feedEntry.faultEvent.time.atZone(ZoneId.of("UTC")))
        return FeedEntryDto(
                faultEventDto,
                feedEntry.globalSequenceNo,
                feedEntry.faultSequenceNo,
                feedEntry.faultStrandSequenceNo)
    }

    fun reset() {
        storage.reset()
        feed.reset()
        stats.reset()
    }

    private fun faultDto(fault: Fault, fullStack: Boolean, printStack: Boolean): FaultDto =
            FaultDto(
                    id = FaultIdDto(fault.hash, link(fault), feed(fault)),
                    faultStrandId = FaultStrandIdDto(
                            fault.faultStrand.id.hash,
                            link(fault.faultStrand.id),
                            feed(fault.faultStrand.id)),
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
                FeedEntryIdDto(
                        handling.feedEntryId.hash, link(handling.feedEntryId)),
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
    ): List<StackTraceElementDto> =
            stackTrace.map { element ->
                StackTraceElementDto(
                        classLoaderName = element.classLoader().value,
                        moduleName = element.module().value,
                        moduleVersion = element.moduleVer().value,
                        declaringClass = element.className().value,
                        methodName = element.method().value,
                        fileName = element.file().value,
                        lineNumber = element.line())
            }.toList()
}
