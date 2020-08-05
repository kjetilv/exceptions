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
import unearth.api.dto.CauseDto
import unearth.api.dto.CauseIdDto
import unearth.munch.id.CauseId
import java.util.*

class DefaultUnearthlyAPI(private val controller: UnearthlyController) : UnearthlyAPI {

    override fun cause(causeIdDto: CauseIdDto, fullStack: Boolean, printStack: Boolean): Optional<CauseDto> {
        val causeDto = controller.lookupCauseDto(CauseId(causeIdDto.uuid), fullStack, printStack)
        val let = causeDto?.let { Optional.ofNullable(it) }
        return let ?: Optional.empty()
    }
}
