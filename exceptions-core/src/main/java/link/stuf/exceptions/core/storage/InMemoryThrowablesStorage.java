package link.stuf.exceptions.core.storage;

import link.stuf.exceptions.core.ThrowablesFeed;
import link.stuf.exceptions.core.ThrowablesStats;
import link.stuf.exceptions.core.ThrowablesStorage;
import link.stuf.exceptions.munch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryThrowablesStorage
    implements ThrowablesStorage, ThrowablesStats, ThrowablesFeed {

    private static final Logger log = LoggerFactory.getLogger(InMemoryThrowablesStorage.class);

    private static final LinkedList<ThrowableSpecimen> EMPTY = new LinkedList<>();

    private final Map<ThrowableSpeciesId, ThrowableSpecies> species = new HashMap<>();

    private final Map<ThrowableSpeciesId, Collection<ThrowableSpecimen>> specimens = new HashMap<>();

    private final Map<ThrowableSpecimenId, ThrowableSpecimen> specimenRegistry = new HashMap<>();

    private final Map<ThrowableStackId, ThrowableStack> stacks = new HashMap<>();

    private final Object lock = new boolean[]{};

    private final AtomicLong globalSequence = new AtomicLong();

    private final Map<ThrowableSpeciesId, AtomicLong> typeSequences = new LinkedHashMap<>();

    private final Clock clock;

    public InMemoryThrowablesStorage() {
        this(null);
    }

    public InMemoryThrowablesStorage(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public ThrowableSpecimen store(ThrowableSpecimen specimen) {
        synchronized (lock) {
            return stored(sequenced(specimen));
        }
    }

    @Override
    public ThrowableSpecies getSpecies(ThrowableSpeciesId speciesId) {
        return get("species", speciesId, species);
    }

    @Override
    public ThrowableStack getStack(ThrowableStackId stackId) {
        return get("stack", stackId, stacks);
    }

    @Override
    public ThrowableSpecimen getSpecimen(ThrowableSpecimenId specimenId) {
        return get("specimen", specimenId, specimenRegistry);
    }

    @Override
    public Collection<ThrowableSpecimen> getSpecimen(ThrowableSpeciesId speciesId) {
        return List.copyOf(specimens.getOrDefault(speciesId, EMPTY));
    }

    @Override
    public long limit(ThrowableSpeciesId id) {
        return Optional.ofNullable(typeSequences.get(id))
            .map(AtomicLong::longValue)
            .orElse(0L);
    }

    @Override
    public List<ThrowableSpecimen> feed(ThrowableSpeciesId id, long offset, int count) {
        return specimens.getOrDefault(id, Collections.emptyList())
            .stream()
            .filter(specimen ->
                specimen.getTypeSequence() > offset)
            .limit(count)
            .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Optional<ThrowableSpecimen> lastOccurrence(ThrowableSpeciesId id) {
        return specimen(id).stream()
            .max(Comparator.comparing(ThrowableSpecimen::getTypeSequence));
    }

    @Override
    public long occurrenceCount(ThrowableSpeciesId id, Instant sinceTime, Duration during) {
        return getSpecimen(id).size();
    }

    @Override
    public Stream<ThrowableSpecimen> occurrences(ThrowableSpeciesId id, Instant sinceTime, Duration period) {
        return specimen(id).stream();
    }

    private ThrowableSpecimen sequenced(ThrowableSpecimen specimen) {
        return specimen.sequenced(
            Instant.now(clock),
            globalSequence.getAndIncrement(),
            typeSequences.computeIfAbsent(
                specimen.getSpecies().getId(),
                id1 ->
                    new AtomicLong()
            ).getAndIncrement());
    }

    private <I extends Id, T> T get(String type, I id, Map<I, T> species1) {
        return Optional.ofNullable(species1.get(id))
            .orElseThrow(() ->
                new IllegalArgumentException("No such " + type + ": " + id));
    }

    private ThrowableSpecimen stored(ThrowableSpecimen sequenced) {
        ThrowableSpecimenId specimenId = sequenced.getId();
        ThrowableSpecimen existingSpecimen = this.specimenRegistry.put(specimenId, sequenced);
        if (existingSpecimen == null) {
            ThrowableSpeciesId speciesId = sequenced.getSpecies().getId();
            sequenced.getSpecies().stacks().forEach(stack ->
                this.stacks.putIfAbsent(stack.getId(), stack));
            this.species.putIfAbsent(speciesId, sequenced.getSpecies());
            this.specimens.computeIfAbsent(speciesId, __ -> new ArrayList<>()
            ).add(sequenced);
        } else {
            log.warn("Logged again: " + sequenced);
        }
        return sequenced;
    }

    private Collection<ThrowableSpecimen> specimen(ThrowableSpeciesId id) {
        return specimens.getOrDefault(id, Collections.emptyList());
    }
}
