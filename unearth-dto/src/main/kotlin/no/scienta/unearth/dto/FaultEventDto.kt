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

package no.scienta.unearth.dto

import no.scienta.unearth.munch.id.FaultEventId
import no.scienta.unearth.munch.id.FaultId
import no.scienta.unearth.munch.id.FaultTypeId
import java.time.ZonedDateTime

data class FaultEventDto(

        val id: FaultEventId,

        val faultId: FaultId,

        val faultTypeId: FaultTypeId,

        val seqNo: Long,

        val faultSeqNo: Long,

        val faultTypeSeqNo: Long,

        val time: ZonedDateTime,

        val causes: List<CauseDto>
)
