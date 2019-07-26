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

import java.util.Collection;

public interface FaultStorage {

    FaultEvent store(LogEntry logEntry, Fault fault);

    Fault getFault(FaultId faultId);

    FaultStrand getFaultStrand(FaultStrandId faultStrandId);

    default Collection<FaultEvent> getEvents(FaultStrandId faultStrandId) {
        return getEvents(faultStrandId, null, null);
    }

    Collection<FaultEvent> getEvents(FaultStrandId faultStrandId, Long offset, Long count);

    default Collection<FaultEvent> getEvents(FaultId faultId) {
        return getEvents(faultId, null, null);
    }

    Collection<FaultEvent> getEvents(FaultId faultId, Long offset, Long count);

    FaultEvent getFaultEvent(FaultEventId faultEventId);

    CauseStrand getCauseStrand(CauseStrandId causeStrandId);

    Cause getCause(CauseId causeId);
}
