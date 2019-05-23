package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.digest.Digest;
import link.stuf.exceptions.core.digest.Occurrence;
import link.stuf.exceptions.core.digest.ThrowableReducer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultThrowablesHandler implements ThrowablesHandler {

    private final Map<UUID, Digest> exceptions = new ConcurrentHashMap<>();

    private final Map<UUID, Map<Instant, Occurrence>> execptionInstances = new ConcurrentHashMap<>();

    private final ThrowableReducer throwableReducer;

    private final List<HandlerListener> listeners;

    public DefaultThrowablesHandler(ThrowableReducer throwableReducer,
                                    HandlerListener... listeners) {
        this.throwableReducer = throwableReducer;
        this.listeners = Arrays.asList(listeners.clone());
    }

    @Override
    public SimpleHandlingPolicy onException(Throwable throwable) {
        Digest digest = Digest.create(throwable);
        Occurrence occurrence = Occurrence.create(throwable, digest, Instant.now());

        Digest existingDigest = recorded(digest, occurrence);
        Digest canonicalDigest = existingDigest == null ? digest : existingDigest;

        listeners.forEach(listener -> listener.handled(digest, occurrence, throwable));

        return new SimpleHandlingPolicy(
            canonicalDigest,
            canonicalDigest.map(throwableReducer::reduce),
            throwable,
            existingDigest == null);
    }

    @Override
    public Throwable lookup(java.util.UUID uuid) {
        return Optional.ofNullable(exceptions.get(uuid))
            .map(Digest::toThrowable)
            .orElseThrow(() ->
                new IllegalArgumentException(uuid.toString()));
    }

    private Digest recorded(Digest chain, Occurrence messages) {
        Digest existing = exceptions.putIfAbsent(chain.getId(), chain);
        execptionInstances.computeIfAbsent(
            chain.getId(),
            id -> new ConcurrentHashMap<>()
        ).put(messages.getTime(), messages);
        return existing;
    }

}
