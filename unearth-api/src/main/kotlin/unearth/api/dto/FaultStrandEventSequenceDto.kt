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

package unearth.api.dto

import java.util.*

data class FaultStrandEventSequenceDto(

    val id: FaultStrandIdDto,

    val events: List<FeedEntryDto> = Collections.emptyList(),

    val sequenceType: SequenceType = SequenceType.FAULT_STRAND,

    val requestedOffset: Long? = null,

    val requestedCount: Long? = null

) : AbstractEventSequence(events, sequenceType, requestedOffset, requestedCount)
