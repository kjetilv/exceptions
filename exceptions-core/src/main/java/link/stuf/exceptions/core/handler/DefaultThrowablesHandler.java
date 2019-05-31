package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.*;
import link.stuf.exceptions.core.throwables.*;

public class DefaultThrowablesHandler implements ThrowablesHandler {

    private final ThrowablesStorage storage;

    private final ThrowablesFeed feed;

    private final ThrowablesSensor sensor;

    private final ThrowablesStats stats;

    public DefaultThrowablesHandler(
        ThrowablesStorage storage,
        ThrowablesFeed feed,
        ThrowablesSensor sensor,
        ThrowablesStats stats
    ) {
        this.storage = storage;
        this.feed = feed;
        this.sensor = sensor;
        this.stats = stats;
    }

    @Override
    public SimpleHandlingPolicy handle(Throwable throwable) {
        ThrowableSpecimen specimen = sensor.registered(
            storage.store(
                Throwables.create(throwable)));
        return new SimpleHandlingPolicy(specimen, throwable, false);
    }

}
