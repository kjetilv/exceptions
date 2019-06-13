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
import no.scienta.unearth.munch.id.*
import java.time.ZonedDateTime
import java.util.*

object Swagger {

    private val random = Random()

    internal fun submission() = Submission(
            FaultTypeId(UUID.randomUUID()),
            FaultId(UUID.randomUUID()),
            FaultEventId(UUID.randomUUID()),
            random.nextLong() % 1000,
            1000L + random.nextLong() % 1000,
            2000L + random.nextLong() % 1000,
            random.nextBoolean())

    internal fun faultSequence(id: ((UUID) -> Id)? = null): FaultSequence =
            FaultSequence(id?.let { it(uuid()) }, SequenceType.FAULT, listOf(faultEventDtos()))

    internal fun faultEventDtos(): FaultEventDto = faultEventDtos(FaultTypeId(uuid()))

    internal fun faultTypeDto() = FaultTypeDto(FaultTypeId(uuid()), listOf(causeTypeDto()))

    internal fun faultDto() = FaultDto(FaultId(uuid()), FaultTypeId(uuid()), listOf(causeDto()));

    internal fun exception() = RuntimeException("Example throwable")

    internal fun causeDto() = CauseDto(CauseId(uuid()), "Bad stuff", causeTypeDto())

    internal fun causeTypeDto() =
            CauseTypeDto(CauseTypeId(uuid()), "BadStuffException", emptyList(), emptyList())

    private fun unearthedException(): UnearthedException =
            UnearthedException(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    causeType = causeTypeDto())

    private fun uuid(): UUID = UUID.randomUUID()

    private fun faultEventDtos(faultTypeId: FaultTypeId): FaultEventDto = FaultEventDto(
            FaultEventId(uuid()),
            FaultId(uuid()),
            faultTypeId,
            random.nextLong(),
            random.nextLong(),
            random.nextLong(),
            ZonedDateTime.now(),
            emptyList(),
            unearthedException())
}
