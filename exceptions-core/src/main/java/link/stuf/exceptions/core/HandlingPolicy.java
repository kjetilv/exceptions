package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ids.FaultTypeId;
import link.stuf.exceptions.munch.ids.FaultEventId;
import link.stuf.exceptions.munch.ids.FaultId;

public interface HandlingPolicy {

    FaultTypeId getFaultTypeId();

    FaultId getFaultId();

    FaultEventId getFaultEventId();

    boolean isLoggable();

    boolean isNew();
}
