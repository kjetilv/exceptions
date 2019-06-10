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
import java.time.ZonedDateTime
import java.util.*

object Example {

    private val random =
            Random()

    fun submission() =
            Submission(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    random.nextLong() % 1000,
                    1000L + random.nextLong() % 1000,
                    2000L + random.nextLong() % 1000,
                    random.nextBoolean())

    fun faultSequence(): FaultSequence =
            FaultSequence(
                    SequenceType.FAULT,
                    listOf(faultEventDtos())
            )

    fun faultEventDtos(): FaultEventDto =
            faultEventDtos(uuid())

    fun faultTypeDto() = uuid().let { id ->
        FaultTypeDto(id, listOf(faultEventDtos(id)))
    }

    private fun faultEventDtos(faultTypeId: UUID): FaultEventDto =
            FaultEventDto(uuid(),
                    faultTypeId,
                    random.nextLong(),
                    random.nextLong(),
                    random.nextLong(),
                    ZonedDateTime.now(),
                    unearthedException())

    private fun unearthedException(): UnearthedException =
            UnearthedException(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    stacktraceId = uuid(),
                    stacktrace = stack())

    private fun uuid(): UUID = UUID.randomUUID()

    fun exception(): Throwable = RuntimeException("Example throwable")

    fun stack() = CauseDto(emptyList(), emptyList(), uuid())
}
