package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.*;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;
import link.stuf.exceptions.core.throwables.Throwables;

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
        ThrowableSpecimen submitted = Throwables.create(throwable);
        ThrowableSpecimen stored = storage.store(submitted);
        ThrowableSpecimen registered = sensor.registered(stored);
        return new SimpleHandlingPolicy(registered, false);
    }
}
