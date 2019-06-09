package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.HandlingPolicy;
import link.stuf.exceptions.munch.ids.FaultTypeId;
import link.stuf.exceptions.munch.data.FaultEvent;
import link.stuf.exceptions.munch.ids.FaultEventId;
import link.stuf.exceptions.munch.ids.FaultId;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final FaultEvent faultEvent;

    private final boolean isNew;

    SimpleHandlingPolicy(FaultEvent faultEvent, boolean isNew) {
        this.faultEvent = faultEvent;
        this.isNew = isNew;
    }

    @Override
    public FaultTypeId getFaultTypeId() {
        return faultEvent.getFault().getFaultType().getId();
    }

    public FaultId getFaultId() {
        return faultEvent.getFault().getId();
    }

    @Override
    public FaultEventId getFaultEventId() {
        return faultEvent.getId();
    }

    @Override
    public boolean isLoggable() {
        return isNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
