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

import no.scienta.unearth.munch.id.*;
import no.scienta.unearth.munch.model.*;

import java.util.Optional;

public interface FaultStorage extends AutoCloseable {

    default Runnable initStorage() {
        return () -> {
        };
    }

    @Override
    default void close() {
    }

    FaultEvents store(LogEntry logEntry, Fault fault, Throwable throwable);

    Optional<Fault> getFault(FaultId faultId);

    Optional<FaultStrand> getFaultStrand(FaultStrandId faultStrandId);

    Optional<FaultEvent> getFaultEvent(FaultEventId faultEventId);

    Optional<CauseStrand> getCauseStrand(CauseStrandId causeStrandId);

    Optional<Cause> getCause(CauseId causeId);

    void reset();
}
