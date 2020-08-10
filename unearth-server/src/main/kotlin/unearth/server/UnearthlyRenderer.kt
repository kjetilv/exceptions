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

package unearth.server

import unearth.api.dto.*
import unearth.core.HandlingPolicy
import unearth.munch.id.Id
import unearth.munch.id.Identifiable
import unearth.munch.model.*
import unearth.munch.print.*
import java.time.ZoneId
import java.util.stream.Stream

class UnearthlyRenderer(private val linkPrefix: String) {

    fun submission(handlingPolicy: HandlingPolicy): Submission =
        Submission(
            FaultStrandIdDto(
                handlingPolicy.faultStrandId.hash, link(handlingPolicy.faultStrandId), feed(handlingPolicy.faultStrandId)
            ),
            FaultIdDto(
                handlingPolicy.faultId.hash, link(handlingPolicy.faultId), feed(handlingPolicy.faultId)
            ),
            FeedEntryIdDto(
                handlingPolicy.feedEntryId.hash, link(handlingPolicy.feedEntryId)
            ),
            handlingPolicy.globalSequence,
            handlingPolicy.faultStrandSequence,
            handlingPolicy.faultSequence,
            action = Action.valueOf(handlingPolicy.action.name),
            printout = toPrintout(handlingPolicy)
        )

    fun link(id: Id): String = "$linkPrefix/${id.type}/${id.hash}"

    fun faultDto(fault: Fault, fullStack: Boolean, printStack: Boolean): FaultDto =
        FaultDto(
            id = FaultIdDto(fault.hash, link(fault), feed(fault)),
            faultStrandId = FaultStrandIdDto(
                fault.faultStrand.id.hash,
                link(fault.faultStrand.id),
                feed(fault.faultStrand.id)
            ),
            causes = fault.causes.map { cause ->
                causeDto(cause, fullStack = fullStack, printStack = printStack)
            })

    fun faultStrandDto(
        faultStrand: FaultStrand,
        fullStack: Boolean,
        printStack: Boolean
    ): FaultStrandDto {
        return FaultStrandDto(
            FaultStrandIdDto(faultStrand.hash, link(faultStrand.id), feed(faultStrand)),
            faultStrand.causeStrands.map {
                causeStrandDto(it, fullStack, printStack)
            })
    }

    fun causeStrandDto(
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
                emptyList()
        )
    }

    fun causeDto(
        cause: Cause,
        fullStack: Boolean = true,
        printStack: Boolean = false
    ): CauseDto =
        CauseDto(
            id = CauseIdDto(cause.hash, link(cause)),
            message = cause.message,
            causeStrand = causeStrandDto(cause.causeStrand, fullStack, printStack)
        )

    fun feedEntryDto(
        feedEntry: FeedEntry,
        fault: Fault,
        fullStack: Boolean = false,
        printStack: Boolean = false
    ): FeedEntryDto {
        val faultDto = faultDto(fault, fullStack, printStack)
        return FeedEntryDto(
            FaultEventDto(
                FeedEntryIdDto(feedEntry.hash, link(feedEntry)),
                faultDto,
                faultDto.id,
                faultDto.faultStrandId,
                feedEntry.faultEvent.time.atZone(ZoneId.of("UTC"))
            ),
            feedEntry.globalSequenceNo,
            feedEntry.faultSequenceNo,
            feedEntry.faultStrandSequenceNo
        )
    }

    private fun link(id: Identifiable<*>): String = link(id.id)

    private fun feed(id: Identifiable<*>): String = feed(id.id)

    private fun feed(id: Id): String = "$linkPrefix/feed/${id.type}/${id.hash}"

    private fun simpleStackTrace(stackTrace: List<CauseFrame>): List<String> =
        stackTrace.map { it.toStackTraceElement().toString() }

    private fun stackTrace(
        stackTrace: List<CauseFrame>
    ): List<StackTraceElementDto> =
        stackTrace.map { element ->
            StackTraceElementDto(
                classLoaderName = element.classLoader().string(),
                moduleName = element.module().string(),
                moduleVersion = element.moduleVer().string(),
                declaringClass = element.className().string(),
                methodName = element.method().string(),
                fileName = element.file().string(),
                lineNumber = element.line()
            )
        }.toList()

    private fun rendering(policy: HandlingPolicy): CausesRendering? = renderers[policy.action]?.render(policy.fault)

    private val renderers: Map<HandlingPolicy.Action, CausesRenderer> = hashMapOf(
        HandlingPolicy.Action.LOG to SimpleCausesRenderer(ConfigurableStackRenderer()),
        HandlingPolicy.Action.LOG_SHORT to SimpleCausesRenderer(rendererFor(listOf("org.http4k", "io.netty"))),
        HandlingPolicy.Action.LOG_MESSAGES to SimpleCausesRenderer(ConfigurableStackRenderer().noStack())
    )

    private fun toPrintout(policy: HandlingPolicy): List<PrintoutDto> =
        rendering(policy)?.map {
            PrintoutDto(it.className, it.message, it.stack)
        } ?: emptyList()

    private fun rendererFor(groups: List<String>): StackRenderer =
        ConfigurableStackRenderer()
            .group(SimplePackageGrouper(groups))
            .squash { _, causeFrames ->
                Stream.of(" * [${causeFrames.size} hidden]")
            }
            .reshape(FrameFun.LIKE_JAVA_8)
            .reshape(FrameFun.SHORTEN_CLASSNAMES)
}
