package link.stuf.exceptions.core.storage;

import link.stuf.exceptions.core.FaultFeed;
import link.stuf.exceptions.core.FaultStats;
import link.stuf.exceptions.core.FaultStorage;
import link.stuf.exceptions.munch.Id;
import link.stuf.exceptions.munch.Identified;
import link.stuf.exceptions.munch.data.CauseType;
import link.stuf.exceptions.munch.data.Fault;
import link.stuf.exceptions.munch.data.FaultEvent;
import link.stuf.exceptions.munch.data.FaultType;
import link.stuf.exceptions.munch.ids.CauseTypeId;
import link.stuf.exceptions.munch.ids.FaultEventId;
import link.stuf.exceptions.munch.ids.FaultId;
import link.stuf.exceptions.munch.ids.FaultTypeId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public class InMemoryThrowablesStorage
    implements FaultStorage, FaultStats, FaultFeed {

    private final Map<FaultId, Fault> faults = new HashMap<>();

    private final Map<FaultTypeId, FaultType> faultTypes = new HashMap<>();

    private final Map<FaultEventId, FaultEvent> events = new HashMap<>();

    private final Map<CauseTypeId, CauseType> causeTypes = new HashMap<>();


    private final Map<FaultTypeId, Collection<Fault>> typedFaults = new HashMap<>();

    private final Map<FaultTypeId, Collection<FaultEvent>> faultTypeEvents = new HashMap<>();

    private final Map<FaultId, Collection<FaultEvent>> faultEvents = new HashMap<>();


    private final Object lock = new boolean[]{};

    private final AtomicLong globalSequence = new AtomicLong();

    private final Map<FaultTypeId, AtomicLong> faultTypeSequence = new LinkedHashMap<>();

    private final Map<FaultId, AtomicLong> faultSequence = new LinkedHashMap<>();

    private final Clock clock;

    public InMemoryThrowablesStorage() {
        this(null);
    }

    public InMemoryThrowablesStorage(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public FaultEvent store(Fault fault) {
        synchronized (lock) {
            FaultEvent faultEvent = storedNew(this.events,
                new FaultEvent(
                    fault,
                    Instant.now(clock),
                    globalSequence.getAndIncrement(),
                    increment(this.faultTypeSequence, fault.getFaultType().getId()),
                    increment(this.faultSequence, fault.getId())));
            FaultType faultType = fault.getFaultType();
            put(this.faultTypes, faultType);
            put(this.faults, fault);
            faultType.getCauseTypes().forEach(put(this.causeTypes));

            addTo(typedFaults, faultType.getId(), fault);
            addTo(faultTypeEvents, faultType.getId(), faultEvent);
            addTo(faultEvents, fault.getId(), faultEvent);

            return faultEvent;
        }
    }

    @Override
    public FaultTypeId resolve(UUID id) {
        if (faultTypes.containsKey(new FaultTypeId(id))) {
            return new FaultTypeId(id);
        }
        if (faults.containsKey(new FaultId(id))) {
            return faults.get(new FaultId(id)).getFaultType().getId();
        }
        if (events.containsKey(new FaultEventId(id))) {
            FaultEvent throwableSpecimen = events.get(new FaultEventId(id));
            return throwableSpecimen.getFault().getFaultType().getId();
        }
        throw new IllegalStateException("No such fault type or fault: " + id);
    }

    @Override
    public FaultType getFaultType(FaultTypeId faultTypeId) {
        return get("faultType", faultTypeId, faultTypes);
    }

    @Override
    public Fault getFault(FaultId faultTypeId) {
        return get("fault", faultTypeId, faults);
    }

    @Override
    public CauseType getStack(CauseTypeId causeTypeId) {
        return get("cause", causeTypeId, causeTypes);
    }

    @Override
    public FaultEvent getFaultEvent(FaultEventId faultEventId) {
        return get("faultEvent", faultEventId, events);
    }

    @Override
    public Collection<FaultEvent> getEvents(FaultTypeId faultTypeId, long offset, long count) {
        return listLookup(this.faultTypeEvents, faultTypeId, offset, count);
    }

    @Override
    public Collection<FaultEvent> getEvents(FaultId faultId, long offset, long count) {
        return listLookup(faultEvents, faultId, offset, count);
    }

    @Override
    public long limit(FaultTypeId id) {
        return getLimit(this.faultTypeSequence, id);
    }

    @Override
    public long limit(FaultId id) {
        return getLimit(this.faultSequence, id);
    }

    @Override
    public List<FaultEvent> feed(FaultTypeId id, long offset, int count) {
        return feed(id, offset, count, this.faultTypeEvents, FaultEvent::getFaultTypeSequence);
    }

    @Override
    public List<FaultEvent> feed(FaultId id, long offset, int count) {
        return feed(id, offset, count, this.faultEvents, FaultEvent::getFaultSequence);
    }

    @Override
    public Optional<FaultEvent> lastFaultEvent(FaultTypeId id) {
        return streamLookup(faultTypeEvents, id)
            .max(Comparator.comparing(FaultEvent::getFaultTypeSequence));
    }

    @Override
    public long faultEventCount(FaultTypeId id, Instant sinceTime, Duration during) {
        return getEvents(id).size();
    }

    @Override
    public Stream<FaultEvent> faultEvents(FaultTypeId id, Instant sinceTime, Duration period) {
        return faultEvents(id);
    }

    private <I extends Id, T> T get(String type, I id, Map<I, T> map) {
        return Optional.ofNullable(map.get(id))
            .orElseThrow(() ->
                new IllegalArgumentException("No such " + type + ": " + id));
    }

    public Stream<FaultEvent> faultEvents(FaultTypeId id) {
        return streamLookup(typedFaults, id).flatMap(fault ->
            streamLookup(faultEvents, fault.getId()));
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

    private static <I extends Id> long getLimit(Map<I, AtomicLong> seq, I id) {
        return Optional.ofNullable(seq.get(id)).map(AtomicLong::longValue).orElse(0L);
    }

    private static <K> long increment(Map<K, AtomicLong> sequences, K id) {
        return sequences.computeIfAbsent(id, __ -> new AtomicLong()).getAndIncrement();
    }

    private static <K, V> Stream<V> streamLookup(Map<K, Collection<V>> map, K key) {
        return listLookup(map, key, null, null).stream();
    }

    private static <K, V> Collection<V> listLookup(Map<K, Collection<V>> map, K key) {
        return listLookup(map, key, null, null);
    }

    private static <K, V> Collection<V> listLookup(Map<K, Collection<V>> map, K key, Long offset, Long count) {
        if (offset != null && offset > Integer.MAX_VALUE || count != null && count > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException
                (InMemoryThrowablesStorage.class + " is for sub-ginormous data amounts ONLY!");
        }
        Collection<V> coll = map.getOrDefault(key, Collections.emptyList());
        if (offset == null || count == null || offset < 0 && count < 0 || offset == 0 && count <= coll.size()) {
            return List.copyOf(coll);
        }
        if (offset >= coll.size()) {
            return Collections.emptyList();
        }
        int lastIndex = Math.min(offset.intValue() + count.intValue(), coll.size());
        return List.copyOf(new ArrayList<>(coll).subList(offset.intValue(), lastIndex));
    }

    private static <I extends Id, T extends Identified<I>> Consumer<T> put(Map<I, T> map) {
        return t -> put(map, t);
    }

    private static <I extends Id, T extends Identified<I>> void put(Map<I, T> map, T t) {
        map.putIfAbsent(t.getId(), t);
    }

    private static <I extends Id, T extends Identified<I>> T storedNew(Map<I, T> map, T t) {
        T existing = map.putIfAbsent(t.getId(), t);
        if (existing != null) {
            throw new IllegalStateException("Already stored: " + existing.getId());
        }
        return t;
    }

    private static <K, V> void addTo(Map<K, Collection<V>> map, K k, V v) {
        map.computeIfAbsent(k, __ -> new ArrayList<>()).add(v);
    }
}
