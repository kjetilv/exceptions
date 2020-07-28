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

package unearth.core;

import java.util.List;
import java.util.OptionalLong;

import unearth.munch.id.FaultId;
import unearth.munch.id.FaultStrandId;
import unearth.munch.model.FeedEntry;

public interface FaultFeed extends AutoCloseable {

    @Override
    default void close() {
    }

    OptionalLong limit();

    OptionalLong limit(FaultStrandId id);

    OptionalLong limit(FaultId id);

    List<FeedEntry> feed(long offset, long count);

    List<FeedEntry> feed(FaultStrandId id, long offset, long count);

    List<FeedEntry> feed(FaultId id, long offset, long count);

    void reset();
}
