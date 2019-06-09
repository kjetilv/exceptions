package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.data.CauseType;
import link.stuf.exceptions.munch.data.Fault;
import link.stuf.exceptions.munch.data.FaultEvent;
import link.stuf.exceptions.munch.data.FaultType;
import link.stuf.exceptions.munch.ids.CauseTypeId;
import link.stuf.exceptions.munch.ids.FaultEventId;
import link.stuf.exceptions.munch.ids.FaultId;
import link.stuf.exceptions.munch.ids.FaultTypeId;

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
