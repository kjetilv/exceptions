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
import no.scienta.unearth.munch.model.FaultEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface FaultStats {

    default long getFaultEventCount(FaultStrandId id) {
        return getFaultEventCount(id, null);
    }

    default long getFaultEventCount(FaultStrandId id, Instant sinceTime) {
        return getFaultEventCount(id, sinceTime, null);
    }

    default Stream<FaultEvent> getFaultEvents(FaultStrandId id) {
        return getFaultEvents(id, null);
    }

    default Stream<FaultEvent> getFaultEvents(FaultStrandId id, Instant sinceTime) {
        return getFaultEvents(id, sinceTime, null);
    }

    default Optional<FaultEvent> getLastFaultEvent(FaultId id) {
        return getLastFaultEvent(id, null);
    }

    default Optional<FaultEvent> getLastFaultEvent(FaultStrandId id) {
        return getLastFaultEvent(id, null);
    }

    Optional<FaultEvent> getLastFaultEvent(FaultId id, Instant sinceTime);

    Optional<FaultEvent> getLastFaultEvent(FaultStrandId id, Instant sinceTime);

    Optional<FaultEvent> getLastFaultEvent(FaultId id, Instant sinceTime, Long ceiling);

    long getFaultEventCount(FaultStrandId id, Instant sinceTime, Duration interval);

    Stream<FaultEvent> getFaultEvents(FaultStrandId id, Instant sinceTime, Duration period);
}
