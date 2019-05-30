package link.stuf.exceptions.core.storage;

import link.stuf.exceptions.core.ThrowablesStats;
import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.core.id.ThrowableSpeciesId;
import link.stuf.exceptions.core.id.ThrowableSpecimenId;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public class InMemoryThrowablesStorage implements ThrowablesStorage, ThrowablesStats {

    private final Map<ThrowableSpeciesId, ThrowableSpecies> species = new HashMap<>();

    private final Map<ThrowableSpeciesId, Map<Instant, ThrowableSpecimen>> specimens = new HashMap<>();

    private final Map<ThrowableSpecimenId, ThrowableSpecimen> specimenRegistry = new HashMap<>();

    private final Object lock = new boolean[]{};

    @Override
    public ThrowableSpecies store(ThrowableSpecimen specimen) {
        synchronized (lock) {
            ThrowableSpecies storedSpecies = specimen.getSpecies();
            ThrowableSpecies knownSpecies = this.species.compute(
                storedSpecies.getId(),
                (id, known) ->
                    known == null ? storedSpecies : known);
            this.specimenRegistry.put(specimen.getId(), specimen);
            this.specimens.computeIfAbsent(knownSpecies.getId(), id -> new LinkedHashMap<>())
                .put(specimen.getTime(), specimen);
            this.specimenRegistry.put(specimen.getId(), specimen);
            return knownSpecies;
        }
    }

    @Override
    public Optional<ThrowableSpecies> getSpecies(ThrowableSpeciesId speciesId) {
        return Optional.ofNullable(species.get(speciesId));
    }

    @Override
    public Optional<ThrowableSpecimen> getSpecimen(ThrowableSpecimenId specimenId) {
        return Optional.ofNullable(specimenRegistry.get(specimenId));
    }

    @Override
    public Collection<ThrowableSpecimen> getSpecimen(ThrowableSpeciesId speciesId) {
        return specimens.getOrDefault(speciesId, Collections.emptyMap()).values();
    }

    @Override
    public Optional<ThrowableSpecimen> lastOccurrence(ThrowableSpeciesId id) {
        return specimen(id).entrySet().stream().max(ENTRIES_BY_DATE)
            .map(Map.Entry::getValue);
    }

    private Map<Instant, ThrowableSpecimen> specimen(ThrowableSpeciesId id) {
        return specimens.getOrDefault(id, Collections.emptyMap());
    }

    @Override
    public long occurrenceCount(ThrowableSpeciesId id, Instant sinceTime, Duration during) {
        return getSpecimen(id).size();
    }

    @Override
    public Stream<ThrowableSpecimen> occurrences(ThrowableSpeciesId id, Instant sinceTime, Duration period) {
        return specimen(id).values().stream();
    }

    private static final Comparator<Map.Entry<Instant, ThrowableSpecimen>> ENTRIES_BY_DATE =
        Comparator.comparing(e -> e.getKey().toEpochMilli());
}
