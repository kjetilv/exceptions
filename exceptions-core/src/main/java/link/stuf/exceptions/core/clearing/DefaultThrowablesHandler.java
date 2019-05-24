package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.digest.ThrowableDigest;
import link.stuf.exceptions.core.digest.ThrowableOccurrence;
import link.stuf.exceptions.core.digest.ThrowablesReducer;

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
        ThrowableDigest digest = ThrowableDigest.create(throwable);
        ThrowableOccurrence occurrence = ThrowableOccurrence.create(throwable, digest, Instant.now());

        ThrowableDigest existingDigest = throwablesStorage.store(digest, occurrence);
        ThrowableDigest canonicalDigest = existingDigest == null ? digest : existingDigest;

        throwablesStorage.store(canonicalDigest, occurrence);

        listeners.forEach(listener ->
            listener.handled(digest, occurrence, throwable));

        return new SimpleHandlingPolicy(
            canonicalDigest,
            canonicalDigest.map(throwablesReducer::reduce),
            throwable,
            existingDigest == null);
    }

    @Override
    public Throwable lookup(java.util.UUID uuid) {
        return throwablesStorage.getDigest(uuid)
            .map(ThrowableDigest::toThrowable)
            .orElseThrow(() ->
                new IllegalArgumentException(uuid.toString()));
    }
}
