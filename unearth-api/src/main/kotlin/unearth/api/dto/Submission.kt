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

import java.util.Collections.emptyList

data class Submission(

    val faultStrandId: FaultStrandIdDto,

    val faultId: FaultIdDto,

    val feedEntryId: FeedEntryIdDto,

    val globalSequenceNo: Long,

    val faultStrandSequenceNo: Long,

    val faultSequenceNo: Long,

    val action: Action?,

    val printout: List<PrintoutDto> = emptyList()
)

data class PrintoutDto(
    val exception: String,

    val message: String = "null",

    val stack: Collection<String>? = emptyList()
)

@Suppress("EnumEntryName", "unused")
enum class Action {

    LOG,

    LOG_SHORT,

    LOG_MESSAGES,

    LOG_ID
}
