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
import no.scienta.unearth.munch.id.CauseTypeId;
import no.scienta.unearth.munch.id.FaultEventId;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultTypeId;

import java.util.Collection;
import java.util.UUID;

public interface FaultStorage {

    FaultEvent store(Fault fault);

    FaultTypeId resolveFaultType(UUID id);

    FaultId resolveFault(UUID id);

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

    FaultEvent getFaultEvent(FaultEventId specimenId);

    CauseType getStack(CauseTypeId stackId);
}
