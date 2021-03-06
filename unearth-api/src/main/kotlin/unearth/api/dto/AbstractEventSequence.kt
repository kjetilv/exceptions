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

abstract class AbstractEventSequence(
    private val events: List<FeedEntryDto>,
    private val sequenceType: SequenceType,
    private val requestedOffset: Long? = null,
    private val requestedCount: Long? = null
) {

    val offset = seqs().min().orElse(0L)

    val count = events.size

    val last = seqs().max().orElse(-1L)

    private fun seqs() = events.stream().mapToLong(sequenceType)
}
