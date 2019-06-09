package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ids.FaultTypeId;
import link.stuf.exceptions.munch.data.FaultEvent;
import link.stuf.exceptions.munch.ids.FaultId;

import java.util.List;

public interface FaultFeed {

    long limit(FaultTypeId id);

    long limit(FaultId id);

    List<FaultEvent> feed(FaultTypeId id, long offset, int count);

    List<FaultEvent> feed(FaultId id, long offset, int count);
}
