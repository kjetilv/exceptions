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

import java.time.ZonedDateTime
import java.util.*

data class FaultEventDto(

        val faultId: UUID,

        val faultTypeId: UUID,

        val seqNo: Long,

        val faultSeqNo: Long,

        val faultTypeSeqNo: Long,

        val time: ZonedDateTime,

        val cause: UnearthedException,

        val printout: String? = null
)
