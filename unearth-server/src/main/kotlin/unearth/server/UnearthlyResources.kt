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
import unearth.core.Resettable

interface UnearthlyResources : AutoCloseable, Resettable {

    fun submitRaw(t: Throwable)
            : HandlingPolicy

    fun lookupFaultStrandDto(id: FaultStrandIdDto, fullStack: Boolean = false, printStack: Boolean = false)
            : FaultStrandDto?

    fun lookupFaultDto(id: FaultIdDto, fullStack: Boolean = true, printStack: Boolean = false)
            : FaultDto?

    fun lookupFeedEntryDto(id: FeedEntryIdDto, fullStack: Boolean = false, printStack: Boolean = false)
            : FeedEntryDto?

    fun lookupCauseStrandDto(id: CauseStrandIdDto, fullStack: Boolean = false, printStack: Boolean = false)
            : CauseStrandDto?

    fun lookupCauseDto(id: CauseIdDto, fullStack: Boolean = false, printStack: Boolean = false)
            : CauseDto?

    fun lookupThrowable(id: FaultIdDto)
            : Throwable?

    fun feedLimit(id: FaultIdDto)
            : Long?

    fun feedLimit(id: FaultStrandIdDto)
            : Long?

    fun feedLimit()
            : Long?

    fun feed(offset: Long, count: Long, fullStack: Boolean = false, printStack: Boolean = false)
            : EventSequenceDto

    fun feed(id: FaultIdDto, offset: Long, count: Long, fullStack: Boolean = false, printStack: Boolean = false)
            : FaultEventSequenceDto

    fun feed(id: FaultStrandIdDto, offset: Long, count: Long, fullStack: Boolean = false, printStack: Boolean = false)
            : FaultStrandEventSequenceDto
}
