package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.*;
import link.stuf.exceptions.munch.data.FaultEvent;
import link.stuf.exceptions.munch.data.Fault;

public class DefaultThrowablesHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultSensor sensor;

    public DefaultThrowablesHandler(
        FaultStorage storage,
        FaultSensor sensor
    ) {
        this.storage = storage;
        this.sensor = sensor;
    }

    @Override
    public SimpleHandlingPolicy handle(Throwable throwable) {
        Fault submitted = Fault.create(throwable);
        FaultEvent stored = storage.store(submitted);
        FaultEvent registered = sensor.registered(stored);
        return new SimpleHandlingPolicy(registered, stored.getFaultTypeSequence() == 0);
    }
}
