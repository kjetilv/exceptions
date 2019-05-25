package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.HandlerListener;
import link.stuf.exceptions.core.digest.ThrowableSpecies;
import link.stuf.exceptions.core.digest.ThrowableSpecimen;
import link.stuf.exceptions.core.ThrowablesReducer;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class DefaultThrowablesHandler implements ThrowablesHandler {

    private final ThrowablesStorage throwablesStorage;

    private final ThrowablesReducer throwablesReducer;

    private final List<HandlerListener> listeners;

    public DefaultThrowablesHandler(
        ThrowablesStorage throwablesStorage,
        ThrowablesReducer throwableReducer,
        HandlerListener... listeners
    ) {
        this.throwablesStorage = throwablesStorage;
        this.throwablesReducer = throwableReducer;
        this.listeners = Arrays.asList(listeners.clone());
    }

    @Override
    public SimpleHandlingPolicy handle(Throwable throwable) {
        ThrowableSpecies digest = ThrowableSpecies.create(throwable);
        ThrowableSpecimen occurrence = ThrowableSpecimen.create(throwable, digest, Instant.now());

        ThrowableSpecies existingDigest = throwablesStorage.store(digest, occurrence);
        ThrowableSpecies canonicalDigest = existingDigest == null ? digest : existingDigest;

        throwablesStorage.store(canonicalDigest, occurrence);

        listeners.forEach(listener ->
            listener.handled(digest, occurrence, throwable));

        return new SimpleHandlingPolicy(
            canonicalDigest,
            throwable,
            existingDigest == null);
    }

    @Override
    public Throwable lookup(java.util.UUID uuid) {
        return throwablesStorage.getDigest(uuid)
            .map(ThrowableSpecies::toThrowable)
            .orElseThrow(() ->
                new IllegalArgumentException(uuid.toString()));
    }
}
