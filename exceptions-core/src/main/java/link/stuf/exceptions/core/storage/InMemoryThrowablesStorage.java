package link.stuf.exceptions.core.storage;

import link.stuf.exceptions.core.ThrowablesFeed;
import link.stuf.exceptions.core.ThrowablesStats;
import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.munch.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryThrowablesStorage
    implements ThrowablesStorage, ThrowablesStats, ThrowablesFeed {

    private final Map<ThrowableSpeciesId, ThrowableSpecies> species = new HashMap<>();

    private final Map<ThrowableSubspeciesId, ThrowableSubspecies> subspecies = new HashMap<>();

    private final Map<ThrowableSpecimenId, ThrowableSpecimen> specimens = new HashMap<>();

    private final Map<ThrowableSpeciesId, Collection<ThrowableSubspecies>> subspeciesOfSpecies = new HashMap<>();

    private final Map<ThrowableSpeciesId, Collection<ThrowableSpecimen>> specimensOfSpecies = new HashMap<>();

    private final Map<ThrowableSubspeciesId, Collection<ThrowableSpecimen>> specimensOfSubspecies = new HashMap<>();

    private final Map<ThrowableStackId, ThrowableStack> stacks = new HashMap<>();

    private final Object lock = new boolean[]{};

    private final AtomicLong globalSequence = new AtomicLong();

    private final Map<ThrowableSpeciesId, AtomicLong> speciesSequences = new LinkedHashMap<>();

    private final Map<ThrowableSubspeciesId, AtomicLong> subspeciesSequences = new LinkedHashMap<>();

    private final Clock clock;

    public InMemoryThrowablesStorage() {
        this(null);
    }

    public InMemoryThrowablesStorage(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public ThrowableSpecimen store(ThrowableSpecimen newSpecimen) {
        synchronized (lock) {
            putNew(this.specimens, newSpecimen);
            ThrowableSpecimen sequencedSpecimen = sequenced(newSpecimen);
            replace(this.specimens, sequencedSpecimen, newSpecimen);
            ThrowableSubspecies subspecies = sequencedSpecimen.getSubspecies();
            ThrowableSpecies species = sequencedSpecimen.getSubspecies().getSpecies();
            species.stacks().forEach(put(this.stacks));
            put(this.species, species);
            put(this.subspecies, subspecies);

            addTo(subspeciesOfSpecies, species.getId(), subspecies);
            addTo(specimensOfSpecies, species.getId(), sequencedSpecimen);
            addTo(specimensOfSubspecies, subspecies.getId(), sequencedSpecimen);

            return sequencedSpecimen;
        }
    }

    @Override
    public ThrowableSpeciesId resolve(UUID id) {
        if (species.containsKey(new ThrowableSpeciesId(id))) {
            return new ThrowableSpeciesId(id);
        }
        if (subspecies.containsKey(new ThrowableSubspeciesId(id))) {
            return subspecies.get(new ThrowableSubspeciesId(id)).getSpecies().getId();
        }
        if (specimens.containsKey(new ThrowableSpecimenId(id))) {
            ThrowableSpecimen throwableSpecimen = specimens.get(new ThrowableSpecimenId(id));
            return throwableSpecimen.getSubspecies().getSpecies().getId();
        }
        throw new IllegalStateException("No such species or specimen: " + id);
    }

    @Override
    public ThrowableSpecies getSpecies(ThrowableSpeciesId speciesId) {
        return get("species", speciesId, species);
    }

    @Override
    public Collection<ThrowableSpecimen> getSpecimensOf(ThrowableSubspeciesId id) {
        return listLookup(specimensOfSubspecies, id);
    }

    @Override
    public ThrowableStack getStack(ThrowableStackId stackId) {
        return get("stack", stackId, stacks);
    }

    @Override
    public ThrowableSpecimen getSpecimen(ThrowableSpecimenId specimenId) {
        return get("specimen", specimenId, specimens);
    }

    @Override
    public Collection<ThrowableSpecimen> getSpecimensOf(ThrowableSpeciesId speciesId) {
        return listLookup(this.specimensOfSpecies, speciesId);
    }

    @Override
    public long limit(ThrowableSpeciesId id) {
        return Optional.ofNullable(speciesSequences.get(id))
            .map(AtomicLong::longValue)
            .orElse(0L);
    }

    @Override
    public long limit(ThrowableSubspeciesId id) {
        return Optional.ofNullable(subspeciesSequences.get(id))
            .map(AtomicLong::longValue)
            .orElse(0L);
    }

    @Override
    public List<ThrowableSpecimen> feed(ThrowableSpeciesId id, long offset, int count) {
        return feed(id, offset, count, this.specimensOfSpecies, ThrowableSpecimen::getSpeciesSequence);
    }

    @Override
    public List<ThrowableSpecimen> feed(ThrowableSubspeciesId id, long offset, int count) {
        return feed(id, offset, count, this.specimensOfSubspecies, ThrowableSpecimen::getSubspeciesSequence);
    }

    @Override
    public Optional<ThrowableSpecimen> lastSpecimen(ThrowableSpeciesId id) {
        return streamLookup(specimensOfSpecies, id)
            .max(Comparator.comparing(ThrowableSpecimen::getSpeciesSequence));
    }

    @Override
    public long specimenCount(ThrowableSpeciesId id, Instant sinceTime, Duration during) {
        return getSpecimensOf(id).size();
    }

    @Override
    public Stream<ThrowableSpecimen> specimens(ThrowableSpeciesId id, Instant sinceTime, Duration period) {
        return specimen(id).stream();
    }

    private ThrowableSpecimen sequenced(ThrowableSpecimen specimen) {
        ThrowableSubspecies subspecies = specimen.getSubspecies();
        ThrowableSpecies species = subspecies.getSpecies();
        return specimen.sequenced(
            Instant.now(clock),
            globalSequence.getAndIncrement(),
            increment(this.speciesSequences, species.getId()),
            increment(this.subspeciesSequences, subspecies.getId()));
    }

    private <I extends Id, T> T get(String type, I id, Map<I, T> species1) {
        return Optional.ofNullable(species1.get(id))
            .orElseThrow(() ->
                new IllegalArgumentException("No such " + type + ": " + id));
    }

    private Collection<ThrowableSpecimen> specimen(ThrowableSpeciesId id) {
        return streamLookup(subspeciesOfSpecies, id).flatMap(subspecies ->
            streamLookup(specimensOfSubspecies, subspecies.getId())
        ).collect(Collectors.toList());
    }

    private static <I extends Id, T> List<T> feed(
        I id,
        long offset,
        int count,
        Map<I, Collection<T>> map,
        Function<T, Long> sequencer
    ) {
        return listLookup(map, id)
            .stream()
            .filter(specimen ->
                sequencer.apply(specimen) > offset)
            .limit(count)
            .collect(Collectors.toUnmodifiableList());
    }

    private static <K> long increment(Map<K, AtomicLong> speciesSequences, K id) {
        return speciesSequences.computeIfAbsent(id, __ -> new AtomicLong()).getAndIncrement();
    }

    private static <K, V> Stream<V> streamLookup(Map<K, Collection<V>> map, K key) {
        return listLookup(map, key).stream();
    }

    private static <K, V> Collection<V> listLookup(Map<K, Collection<V>> map, K key) {
        return List.copyOf(map.getOrDefault(key, Collections.emptyList()));
    }

    private static <I extends Id, T extends Identified<I>> Consumer<T> put(Map<I, T> map) {
        return t -> put(map, t);
    }

    private static <I extends Id, T extends Identified<I>> void put(Map<I, T> map, T t) {
        map.putIfAbsent(t.getId(), t);
    }

    private static <I extends Id, T extends Identified<I>> void putNew(Map<I, T> map, T t) {
        T existing = map.putIfAbsent(t.getId(), t);
        if (existing != null) {
            throw new IllegalStateException("Already stored: " + existing.getId());
        }
    }

    private static <I extends Id, T extends Identified<I>> void replace(Map<I, T> map, T t, T expected) {
        map.compute(t.getId(), (id, previous) -> {
            if (previous == null) {
                throw new IllegalStateException("Did not exist: " + id);
            }
            if (previous != expected) {
                throw new IllegalStateException("Wrong value to be overwritten: " + previous.getId());
            }
            return t;
        });
    }

    private static <K, V> void addTo(Map<K, Collection<V>> map, K k, V v) {
        map.computeIfAbsent(k, __ -> new ArrayList<>()).add(v);
    }
}
