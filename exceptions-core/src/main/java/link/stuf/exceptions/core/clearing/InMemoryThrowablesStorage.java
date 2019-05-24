package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.core.digest.ThrowableDigest;
import link.stuf.exceptions.core.digest.ThrowableOccurrence;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryThrowablesStorage implements ThrowablesStorage {

    private final Map<UUID, ThrowableDigest> digests = new ConcurrentHashMap<>();

    private final Map<UUID, Map<Instant, ThrowableOccurrence>> occurrenceTimes = new ConcurrentHashMap<>();

    private final Map<UUID, ThrowableOccurrence> occurrences = new ConcurrentHashMap<>();

    @Override
    public ThrowableDigest store(ThrowableDigest digest, ThrowableOccurrence occurrence) {
        ThrowableDigest existing = digests.putIfAbsent(digest.getId(), digest);
        occurrences.put(occurrence.getId(), occurrence);
        occurrenceTimes.computeIfAbsent(
            digest.getId(),
            id -> new ConcurrentHashMap<>()
        ).put(occurrence.getTime(), occurrence);
        return existing == null ? digest : existing;
    }

    @Override
    public Optional<ThrowableDigest> getDigest(UUID digestId) {
        return Optional.ofNullable(digests.get(digestId));
    }

    @Override
    public Optional<ThrowableOccurrence> getOccurrence(UUID occurrenceId) {
        return Optional.ofNullable(occurrences.get(occurrenceId));
    }

    @Override
    public Collection<ThrowableOccurrence> getOccurrences(UUID digestId) {
        return occurrenceTimes.getOrDefault(digestId, Collections.emptyMap()).values();
    }

}
