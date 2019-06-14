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

object Swaggex {

    private val random = Random()

    internal fun submission() = Submission(
            FaultStrandId(UUID.randomUUID()),
            FaultId(UUID.randomUUID()),
            FaultEventId(UUID.randomUUID()),
            random.nextLong() % 1000,
            1000L + random.nextLong() % 1000,
            2000L + random.nextLong() % 1000,
            random.nextBoolean())

    internal fun faultSequence(id: ((UUID) -> Id)? = null): FaultEventSequence =
            FaultEventSequence(id?.let { it(uuid()) }, SequenceType.FAULT, listOf(faultEventDto()))

    internal fun faultStrandDto() = FaultStrandDto(FaultStrandId(uuid()), listOf(causeStrandDto()))

    internal fun faultDto() = FaultDto(FaultId(uuid()), FaultStrandId(uuid()), listOf(causeDto()))

    internal fun exception() = RuntimeException("Example throwable")

    internal fun causeDto() = CauseDto(CauseId(uuid()), "Bad stuff", causeStrandDto())

    internal fun faultEventDto(): FaultEventDto = FaultEventDto(
            FaultEventId(uuid()),
            faultDto(),
            ZonedDateTime.now(),
            random.nextInt().toLong(),
            random.nextInt().toLong(),
            random.nextInt().toLong())

    internal fun causeStrandDto() =
            CauseStrandDto(CauseStrandId(uuid()), "BadStuffException", emptyList(), emptyList())

    private fun unearthedException(): UnearthedException =
            UnearthedException(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    causeStrand = causeStrandDto())

    private fun uuid(): UUID = UUID.randomUUID()

    fun limit(): Long = random.nextInt().toLong()
}
