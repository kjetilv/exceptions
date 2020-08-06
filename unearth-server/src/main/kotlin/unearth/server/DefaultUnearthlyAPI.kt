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

import unearth.api.UnearthlyAPI
import unearth.api.dto.*
import unearth.munch.parser.ThrowableParser
import java.util.*

class DefaultUnearthlyAPI(
    private val controller: UnearthlyController,
    private val renderer: UnearthlyRenderer
) : UnearthlyAPI {

    override fun cause(
        causeIdDto: CauseIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ) = unpack(
        controller.lookupCauseDto(causeIdDto, fullStack, printStack)
    )

    override fun causeStrand(
        causeStrandIdDto: CauseStrandIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ) = unpack(
        controller.lookupCauseStrandDto(causeStrandIdDto, fullStack, printStack)
    )

    override fun fault(
        faultId: FaultIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ) = unpack(
        controller.lookupFaultDto(faultId, fullStack, printStack)
    )

    override fun faultStrand(
        faultId: FaultStrandIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ) = unpack(
        controller.lookupFaultStrandDto(faultId, fullStack, printStack)
    )

    override fun feedEntry(
        faultEventIdDto: FeedEntryIdDto,
        fullStack: Boolean,
        printStack: Boolean
    ) = unpack(
        controller.lookupFeedEntryDto(faultEventIdDto, fullStack, printStack)
    )

    override fun faultFeed(
        faultId: FaultIdDto,
        offset: Long,
        count: Long,
        fullStack: Boolean,
        printStack: Boolean
    ) =
        controller.feed(faultId, offset, count, fullStack, printStack)

    override fun faultFeedLimit(faultId: FaultIdDto) = controller.feedLimit(faultId) ?: 0

    override fun faultStrandFeed(
        faultId: FaultStrandIdDto,
        offset: Long,
        count: Long,
        fullStack: Boolean,
        printStack: Boolean
    ) =
        controller.feed(faultId, offset, count, fullStack, printStack)

    override fun faultStrandFeedLimit(faultId: FaultStrandIdDto) =
        controller.feedLimit(faultId) ?: 0

    override fun globalFeed(offset: Long, count: Long, fullStack: Boolean, printStack: Boolean) =
        controller.feed(offset, count, fullStack, printStack)

    override fun globalFeedLimit() =
        controller.feedLimit() ?: 0

    override fun throwable(throwable: String?): Submission {
        val parsed = ThrowableParser.parse(throwable)
        val handlingPolicy = controller.submitRaw(parsed)
        return renderer.submission(handlingPolicy)
    }

    private fun <T> unpack(optT: T?): Optional<T> {
        return optT?.let { t -> Optional.ofNullable(t) } ?: Optional.empty()
    }

    private fun longish(limit: OptionalLong?): Long? =
        limit?.let {
            if (it.isEmpty) null else it.asLong
        }
}
