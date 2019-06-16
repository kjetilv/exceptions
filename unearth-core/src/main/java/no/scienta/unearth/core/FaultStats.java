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

package no.scienta.unearth.core;

import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.model.FaultEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface FaultStats {

    Optional<FaultEvent> lastFaultEvent(FaultStrandId id);

    default long faultEventCount(FaultStrandId id) {
        return faultEventCount(id, null);
    }

    default long faultEventCount(FaultStrandId id, Instant sinceTime) {
        return faultEventCount(id, sinceTime, null);
    }

    long faultEventCount(FaultStrandId id, Instant sinceTime, Duration during);

    default Stream<FaultEvent> faultEvents(FaultStrandId id) {
        return faultEvents(id, null);
    }

    default Stream<FaultEvent> faultEvents(FaultStrandId id, Instant sinceTime) {
        return faultEvents(id, sinceTime, null);
    }

    Stream<FaultEvent> faultEvents(FaultStrandId id, Instant sinceTime, Duration period);
}
