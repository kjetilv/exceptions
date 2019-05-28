package link.stuf.exceptions.core.storage;

import link.stuf.exceptions.core.ThrowableSpeciesId;
import link.stuf.exceptions.core.ThrowablesStats;
import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class InMemoryThrowablesStorage implements ThrowablesStorage, ThrowablesStats {

    private final Map<UUID, ThrowableSpecies> species = new ConcurrentHashMap<>();

    private final Map<UUID, Map<Instant, ThrowableSpecimen>> specimen = new ConcurrentHashMap<>();

    private final Map<UUID, ThrowableSpecimen> specimenRegistry = new ConcurrentHashMap<>();

    @Override
    public ThrowableSpecies store(ThrowableSpecies species, ThrowableSpecimen specimen) {
        ThrowableSpecies existing = this.species.putIfAbsent(species.getHash(), species);
        specimenRegistry.put(specimen.getHash(), specimen);
        this.specimen.computeIfAbsent(
            species.getHash(),
            id -> new ConcurrentHashMap<>()
        ).put(specimen.getTime(), specimen);
        return existing == null ? species : existing;
    }

    @Override
    public Optional<ThrowableSpecies> getDigest(UUID digestId) {
        return Optional.ofNullable(species.get(digestId));
    }

    @Override
    public Optional<ThrowableSpecimen> getOccurrence(UUID occurrenceId) {
        return Optional.ofNullable(specimenRegistry.get(occurrenceId));
    }

    @Override
    public Collection<ThrowableSpecimen> getOccurrences(UUID digestId) {
        return specimen.getOrDefault(digestId, Collections.emptyMap()).values();
    }

    @Override
    public Optional<ThrowableSpecimen> lastOccurrence(ThrowableSpeciesId id) {
        return getSpecimen(id)
            .entrySet().stream().max(BY_DATE)
            .map(Map.Entry::getValue);
    }

    private Map<Instant, ThrowableSpecimen> getSpecimen(ThrowableSpeciesId id) {
        return specimen.getOrDefault(id.getHash(), Collections.emptyMap());
    }

    @Override
    public long occurrenceCount(ThrowableSpeciesId id, Instant sinceTime, Duration during) {
        return getSpecimen(id).size();
    }

    @Override
    public Stream<ThrowableSpecimen> occurrences(ThrowableSpeciesId id, Instant sinceTime, Duration period) {
        return getSpecimen(id).values().stream();
    }

    private static final Comparator<Map.Entry<Instant, ThrowableSpecimen>> BY_DATE = Comparator.comparing(e -> e.getKey().toEpochMilli());
}
