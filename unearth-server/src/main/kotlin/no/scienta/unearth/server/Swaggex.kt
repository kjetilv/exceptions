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

package no.scienta.unearth.server

import no.scienta.unearth.dto.*
import java.time.ZonedDateTime
import java.util.*

object Swaggex {

    private val random = Random()

    internal fun submission() = Submission(
            FaultStrandIdDto(UUID.randomUUID()),
            FaultIdDto(UUID.randomUUID()),
            FaultEventIdDto(UUID.randomUUID()),
            random.nextLong() % 1000,
            1000L + random.nextLong() % 1000,
            2000L + random.nextLong() % 1000,
            if (random.nextBoolean()) Action.LOG else Action.LOG_SHORT)

    internal fun eventSequence(): EventSequence =
            EventSequence(listOf(faultEventDto()))

    internal fun faultEventSequence(): FaultEventSequence =
            FaultEventSequence(FaultIdDto(uuid()), listOf(faultEventDto()))

    internal fun faultStrandEventSequence(): FaultStrandEventSequence =
            FaultStrandEventSequence(FaultStrandIdDto(uuid()), listOf(faultEventDto()))

    internal fun faultStrandDto() = FaultStrandDto(FaultStrandIdDto(uuid()), listOf(causeStrandDto()))

    internal fun faultDto() = FaultDto(FaultIdDto(uuid()), FaultStrandIdDto(uuid()), listOf(causeDto()))

    internal fun exception() = RuntimeException("Example throwable")

    internal fun causeDto() = CauseDto(CauseIdDto(uuid()), "Bad stuff", causeStrandDto())

    internal fun faultEventDto(): FaultEventDto = FaultEventDto(
            FaultEventIdDto(uuid()),
            faultDto(),
            ZonedDateTime.now(),
            random.nextInt().toLong(),
            random.nextInt().toLong(),
            random.nextInt().toLong())

    internal fun causeStrandDto() =
            CauseStrandDto(CauseStrandIdDto(uuid()), "BadStuffException", emptyList(), emptyList())

    internal fun causeChainDto(): CauseChainDto =
            CauseChainDto(
                    "mymy.such.a.BadClass",
                    "Bad class!",
                    causeStrand = causeStrandDto())

    private fun uuid(): UUID = UUID.randomUUID()

    fun limit(): Long = random.nextInt().toLong()
}
