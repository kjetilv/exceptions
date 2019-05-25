package link.stuf.exceptions.core.storage;

import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.core.digest.ThrowableSpecies;
import link.stuf.exceptions.core.digest.ThrowableSpecimen;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryThrowablesStorage implements ThrowablesStorage {

    private final Map<UUID, ThrowableSpecies> digests = new ConcurrentHashMap<>();

    private final Map<UUID, Map<Instant, ThrowableSpecimen>> occurrenceTimes = new ConcurrentHashMap<>();

    private final Map<UUID, ThrowableSpecimen> occurrences = new ConcurrentHashMap<>();

    @Override
    public ThrowableSpecies store(ThrowableSpecies digest, ThrowableSpecimen occurrence) {
        ThrowableSpecies existing = digests.putIfAbsent(digest.getHash(), digest);
        occurrences.put(occurrence.getHash(), occurrence);
        occurrenceTimes.computeIfAbsent(
            digest.getHash(),
            id -> new ConcurrentHashMap<>()
        ).put(occurrence.getTime(), occurrence);
        return existing == null ? digest : existing;
    }

    @Override
    public Optional<ThrowableSpecies> getDigest(UUID digestId) {
        return Optional.ofNullable(digests.get(digestId));
    }

    @Override
    public Optional<ThrowableSpecimen> getOccurrence(UUID occurrenceId) {
        return Optional.ofNullable(occurrences.get(occurrenceId));
    }

    @Override
    public Collection<ThrowableSpecimen> getOccurrences(UUID digestId) {
        return occurrenceTimes.getOrDefault(digestId, Collections.emptyMap()).values();
    }

}
