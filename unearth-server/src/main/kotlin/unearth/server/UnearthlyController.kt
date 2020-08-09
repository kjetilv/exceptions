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
import unearth.core.*
import unearth.core.handler.DefaultFaultHandler
import unearth.munch.id.*
import unearth.munch.model.Fault
import java.time.Clock
import java.util.*

class UnearthlyController(
    private val storage: FaultStorage,
    private val feed: FaultFeed,
    private val stats: FaultStats,
    private val sensor: FaultSensor,
    private val renderer: UnearthlyRenderer,
    clock: Clock = Clock.systemDefaultZone()
) : UnearthlyResources {

    val handler: FaultHandler = DefaultFaultHandler(storage, sensor, stats, clock)

    override fun close() {
        listOf<AutoCloseable>(storage, feed, stats).forEach(AutoCloseable::close)
    }

    override fun submitRaw(t: Throwable): HandlingPolicy =
        handler.handle(t)!!

    override fun lookupFaultStrandDto(
        id: FaultStrandIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ): FaultStrandDto? =
        storage.getFaultStrand(FaultStrandId(id.uuid)).orElse(null)?.let { faultStrand ->
            renderer.faultStrandDto(faultStrand, fullStack, printStack)
        }

    override fun lookupFaultDto(
        id: FaultIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ): FaultDto? =
        storage.getFault(FaultId(id.uuid)).orElse(null)?.let { renderer.faultDto(it, fullStack, printStack) }

    override fun lookupFeedEntryDto(
        id: FeedEntryIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ): FeedEntryDto? =
        storage.getFeedEntry(FeedEntryId(id.uuid)).orElse(null)?.let { feedEntry ->
            storage.getRequiredFault(feedEntry.faultEvent.faultId).let { fault ->
                renderer.feedEntryDto(feedEntry, fault, fullStack, printStack)
            }
        }

    override fun lookupCauseStrandDto(
        id: CauseStrandIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ): CauseStrandDto? =
        storage.getCauseStrand(CauseStrandId(id.uuid)).orElse(null)?.let {
            renderer.causeStrandDto(it, fullStack, printStack)
        }

    override fun lookupCauseDto(
        id: CauseIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ): CauseDto? =
        storage.getCause(CauseId(id.uuid)).orElse(null)?.let { renderer.causeDto(it, fullStack, printStack) }

    override fun lookupThrowable(id: FaultIdDto): Throwable? =
        storage.getFault(FaultId(id.uuid)).map(Fault::toChameleon).orElse(null)

    override fun feedLimit(id: FaultIdDto): Long? =
        longish(feed.limit(FaultId(id.uuid)))

    override fun feedLimit(id: FaultStrandIdDto): Long? =
        longish(feed.limit(FaultStrandId(id.uuid)))

    override fun feedLimit(): Long? =
        longish(feed.limit())

    override fun feed(offset: Long, count: Long, fullStack: Boolean, printStack: Boolean): EventSequenceDto =
        EventSequenceDto(
            feed.feed(offset, count).map { feedEntry ->
                storage.getRequiredFault(feedEntry.faultEvent.faultId).let { fault ->
                    renderer.feedEntryDto(feedEntry, fault, fullStack, printStack)
                }
            }
        )

    override fun feed(
        id: FaultIdDto,
        offset: Long,
        count: Long,
        fullStack: Boolean,
        printStack: Boolean
    ): FaultEventSequenceDto =
        FaultEventSequenceDto(
            FaultIdDto(id.uuid, renderer.link(FaultId(id.uuid))),
            feed.feed(FaultId(id.uuid), offset, count).map { feedEntry ->
                storage.getRequiredFault(feedEntry.faultEvent.faultId).let { fault ->
                    renderer.feedEntryDto(feedEntry, fault, fullStack, printStack)
                }
            })

    override fun feed(
        id: FaultStrandIdDto,
        offset: Long,
        count: Long,
        fullStack: Boolean,
        printStack: Boolean
    ): FaultStrandEventSequenceDto =
        FaultStrandEventSequenceDto(
            FaultStrandIdDto(id.uuid, renderer.link(FaultId(id.uuid))),
            feed.feed(FaultId(id.uuid), offset, count).map { feedEntry ->
                storage.getRequiredFault(feedEntry.faultEvent.faultId).let { fault ->
                    renderer.feedEntryDto(feedEntry, fault, fullStack, printStack)
                }
            })

    override fun reset() {
        storage.reset()
        feed.reset()
        stats.reset()
    }

    private fun longish(limit: OptionalLong?): Long? =
        limit?.let {
            if (it.isEmpty) null else it.asLong
        }
}
