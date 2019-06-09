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

import no.scienta.unearth.munch.data.CauseType;
import no.scienta.unearth.munch.data.Fault;
import no.scienta.unearth.munch.data.FaultEvent;
import no.scienta.unearth.munch.data.FaultType;
import no.scienta.unearth.munch.ids.CauseTypeId;
import no.scienta.unearth.munch.ids.FaultEventId;
import no.scienta.unearth.munch.ids.FaultId;
import no.scienta.unearth.munch.ids.FaultTypeId;

import java.util.Collection;
import java.util.UUID;

public interface FaultStorage {

    FaultEvent store(Fault fault);

    FaultTypeId resolve(UUID id);

    Fault getFault(FaultId faultId);

    FaultType getFaultType(FaultTypeId digestId);

    default Collection<FaultEvent> getEvents(FaultTypeId faultTypeId) {
        return getEvents(faultTypeId, -1, -1);
    }

    Collection<FaultEvent> getEvents(FaultTypeId faultTypeId, long offset, long count);

    default Collection<FaultEvent> getEvents(FaultId faultId) {
        return getEvents(faultId, -1, -1);
    }

    Collection<FaultEvent> getEvents(FaultId faultId, long offset, long count);

    FaultEvent getFaultEvent(FaultEventId specimenId);

    CauseType getStack(CauseTypeId stackId);
}
