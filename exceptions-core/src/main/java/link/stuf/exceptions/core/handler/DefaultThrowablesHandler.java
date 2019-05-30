package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.ThrowablesSensor;
import link.stuf.exceptions.core.ThrowablesStats;
import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.core.id.ThrowableSpecimenId;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.time.Clock;
import java.time.Instant;

public class DefaultThrowablesHandler implements ThrowablesHandler {

    private final ThrowablesStorage storage;

    private final ThrowablesSensor sensor;

    private final ThrowablesStats stats;

    private final Clock clock;

    public DefaultThrowablesHandler(
        ThrowablesStorage storage,
        ThrowablesSensor sensor,
        ThrowablesStats stats
    ) {
        this(storage, sensor, stats, null);
    }

    public DefaultThrowablesHandler(
        ThrowablesStorage storage,
        ThrowablesSensor sensor,
        ThrowablesStats stats,
        Clock clock
    ) {
        this.storage = storage;
        this.sensor = sensor;
        this.stats = stats;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public SimpleHandlingPolicy handle(Throwable throwable) {
        ThrowableSpecimen specimen = ThrowableSpecimen.create(
            throwable,
            ThrowableSpecies.create(throwable),
            Instant.now(clock));

        ThrowableSpecies species = storage.store(specimen);
        sensor.registered(species, specimen);

        return new SimpleHandlingPolicy(specimen, throwable, false);
    }

    @Override
    public Throwable lookup(java.util.UUID id) {
        return storage.getSpecimen(new ThrowableSpecimenId(id))
            .map(ThrowableSpecimen::toThrowable)
            .orElseThrow(() ->
                new IllegalArgumentException(id.toString()));
    }
}
