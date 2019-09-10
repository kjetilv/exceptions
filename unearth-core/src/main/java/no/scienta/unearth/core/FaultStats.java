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

package no.scienta.unearth.core;

import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.model.FeedEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FaultStats extends AutoCloseable {

    @Override
    default void close() {
    }

    default long getFeedEntryCount(FaultStrandId id) {
        return getFeedEntryCount(id, null);
    }

    default long getFeedEntryCount(FaultStrandId id, Instant sinceTime) {
        return getFeedEntryCount(id, sinceTime, null);
    }

    default Optional<FeedEntry> getLastFeedEntry(FaultId id) {
        return getLastFeedEntry(id, null);
    }

    default Optional<FeedEntry> getLastFeedEntry(FaultStrandId id) {
        return getLastFeedEntry(id, null);
    }

    Optional<FeedEntry> getLastFeedEntry(FaultId id, Instant sinceTime);

    Optional<FeedEntry> getLastFeedEntry(FaultStrandId id, Instant sinceTime);

    Optional<FeedEntry> getLastFeedEntry(FaultId id, Instant sinceTime, Long ceiling);

    long getFeedEntryCount(Instant sinceTime, Duration interval);

    long getFeedEntryCount(FaultStrandId id, Instant sinceTime, Duration interval);

    long getFeedEntryCount(FaultId id, Instant sinceTime, Duration interval);

    default List<FeedEntry> getFeed(FaultStrandId id) {
        return getFeed(id, null);
    }

    default List<FeedEntry> getFeed(FaultStrandId id, Instant sinceTime) {
        return getFeed(id, sinceTime, null);
    }

    List<FeedEntry> getFeed(FaultStrandId id, Instant sinceTime, Duration period);

    default List<FeedEntry> getFeed() {
        return getFeed((Instant) null);
    }

    default List<FeedEntry> getFeed(Instant sinceTime) {
        return getFeed(sinceTime, null);
    }

    List<FeedEntry> getFeed(Instant sinceTime, Duration period);

    default List<FeedEntry> getFeed(FaultId id) {
        return getFeed(id, null);
    }

    default List<FeedEntry> getFeed(FaultId id, Instant sinceTime) {
        return getFeed(id, sinceTime, null);
    }

    List<FeedEntry> getFeed(FaultId id, Instant sinceTime, Duration period);

    void reset();
}
