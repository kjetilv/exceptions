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

import unearth.server.dto.*
import java.time.ZonedDateTime
import java.util.*

object Swaggex {

    private val random = Random()

    internal fun submission() = Submission(
            FaultStrandIdDto(UUID.randomUUID()),
            FaultIdDto(UUID.randomUUID()),
            FeedEntryIdDto(UUID.randomUUID()),
            random.nextLong() % 1000,
            1000L + random.nextLong() % 1000,
            2000L + random.nextLong() % 1000,
            if (random.nextBoolean()) Action.LOG else Action.LOG_SHORT)

    internal fun eventSequence(): EventSequenceDto =
            EventSequenceDto(listOf(feedEntryDto()))

    internal fun faultEventSequence(): FeedEntrySequence =
            FeedEntrySequence(FaultIdDto(uuid()), listOf(feedEntryDto()))

    internal fun faultStrandEventSequence(): FaultStrandEventSequenceDto =
            FaultStrandEventSequenceDto(FaultStrandIdDto(uuid()), listOf(feedEntryDto()))

    internal fun faultStrandDto() = FaultStrandDto(FaultStrandIdDto(uuid()), listOf(causeStrandDto()))

    internal fun faultDto() = FaultDto(FaultIdDto(uuid()), FaultStrandIdDto(uuid()), listOf(causeDto()))

    internal fun exception() = RuntimeException("Example throwable")

    internal fun causeDto() = CauseDto(CauseIdDto(uuid()), "Bad stuff", causeStrandDto())

    internal fun feedEntryDto(): FeedEntryDto {
        faultDto().let {
            return FeedEntryDto(
                    FaultEventDto(
                            FeedEntryIdDto(uuid()),
                            it,
                            it.id,
                            it.faultStrandId,
                            ZonedDateTime.now()),
                    random.nextInt().toLong(),
                    random.nextInt().toLong(),
                    random.nextInt().toLong())
        }
    }

    internal fun causeStrandDto() =
            CauseStrandDto(CauseStrandIdDto(uuid()), "BadStuffException", emptyList(), emptyList())

    private fun uuid(): UUID = UUID.randomUUID()

    fun limit(): Long = random.nextInt().toLong()
}