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

import no.scienta.unearth.munch.data.*;
import no.scienta.unearth.munch.id.*;

import java.util.Collection;

public interface FaultStorage {

    FaultEvent store(Fault fault);

    Fault getFault(FaultId faultId);

    FaultType getFaultType(FaultTypeId digestId);

    default Collection<FaultEvent> getEvents(FaultTypeId faultTypeId) {
        return getEvents(faultTypeId, null, null);
    }

    Collection<FaultEvent> getEvents(FaultTypeId faultTypeId, Long offset, Long count);

    default Collection<FaultEvent> getEvents(FaultId faultId) {
        return getEvents(faultId, null, null);
    }

    Collection<FaultEvent> getEvents(FaultId faultId, Long offset, Long count);

    FaultEvent getFaultEvent(FaultEventId faultEventId);

    CauseType getCauseType(CauseTypeId causeTypeId);

    Cause getCause(CauseId causeId);
}
