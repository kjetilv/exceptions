package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.ThrowablesSensor;
import link.stuf.exceptions.core.ThrowablesStats;
import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.time.Instant;

public class DefaultThrowablesHandler implements ThrowablesHandler {

    private final ThrowablesStorage storage;

    private final ThrowablesSensor sensor;

    private final ThrowablesStats stats;

    public DefaultThrowablesHandler(
        ThrowablesStorage storage,
        ThrowablesSensor sensor,
        ThrowablesStats stats
    ) {
        this.storage = storage;
        this.sensor = sensor;
        this.stats = stats;
    }

    @Override
    public SimpleHandlingPolicy handle(Throwable throwable) {
        ThrowableSpecies digest = ThrowableSpecies.create(throwable);
        ThrowableSpecimen occurrence = ThrowableSpecimen.create(throwable, digest, Instant.now());

        ThrowableSpecies existingDigest = storage.store(digest, occurrence);
        ThrowableSpecies canonicalDigest = existingDigest == null ? digest : existingDigest;

        sensor.registered(canonicalDigest, occurrence);

        return new SimpleHandlingPolicy(canonicalDigest, throwable, existingDigest == null);
    }

    @Override
    public Throwable lookup(java.util.UUID uuid) {
        return storage.getDigest(uuid)
            .map(ThrowableSpecies::toThrowable)
            .orElseThrow(() ->
                new IllegalArgumentException(uuid.toString()));
    }
}
