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

package no.scienta.unearth.server

import no.scienta.unearth.dto.*
import no.scienta.unearth.munch.util.Throwables
import java.time.ZonedDateTime
import java.util.*

object Ex {

    private val random = Random()

    fun uuid() = UUID.randomUUID()

    fun submission() =
            Submission(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

    fun faultEventDtos(): FaultEventDto = faultEventDtos(uuid())

    fun faultEventDtos(faultTypeId: UUID): FaultEventDto {
        return FaultEventDto(uuid(),
                faultTypeId,
                random.nextLong(),
                random.nextLong(),
                random.nextLong(),
                ZonedDateTime.now(),
                wiredException())
    }

    fun faultTypeDto() = uuid().let { id ->
        FaultTypeDto(id, listOf(faultEventDtos(id)))
    }

    fun wiredException(): UnearthedException =
            UnearthedException(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    stacktrace = stack())

    fun exception(): Throwable = RuntimeException("Example throwable")

    fun exceptionOut(): String = Throwables.string(exception())

    fun stack() = CauseDto("foo.bar.BadException", emptyList(), emptyList(), uuid())
}
