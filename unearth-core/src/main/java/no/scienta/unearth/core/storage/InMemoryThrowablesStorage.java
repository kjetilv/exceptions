/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.core.storage;

import no.scienta.unearth.core.FaultFeed;
import no.scienta.unearth.core.FaultStats;
import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.munch.data.*;
import no.scienta.unearth.munch.id.*;

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

    private final Map<CauseId, Cause> causes = new HashMap<>();


    private final Collection<FaultEvent> faultEvents = new ArrayList<>();

    private final Map<FaultTypeId, Collection<Fault>> faultTypeFaults = new HashMap<>();

    private final Map<FaultTypeId, Collection<FaultEvent>> faultTypeFaultEvents = new HashMap<>();

    private final Map<FaultId, Collection<FaultEvent>> faultFaultEvents = new HashMap<>();


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
            FaultEvent faultEvent = stored(this.events,
                new FaultEvent(
                    fault,
                    Instant.now(clock),
                    globalSequence.getAndIncrement(),
                    increment(this.faultTypeSequence, fault.getFaultType().getId()),
                    increment(this.faultSequence, fault.getId()))
            );
            FaultType faultType = fault.getFaultType();
            put(this.faultTypes, faultType);
            put(this.faults, fault);
            faultType.getCauseTypes().forEach(put(this.causeTypes));
            faultEvent.getFault().getCauses().forEach(put(this.causes));

            faultEvents.add(faultEvent);
            addTo(faultTypeFaults, faultType.getId(), fault);
            addTo(faultTypeFaultEvents, faultType.getId(), faultEvent);
            addTo(faultFaultEvents, fault.getId(), faultEvent);

            return faultEvent;
        }
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
    public CauseType getCauseType(CauseTypeId causeTypeId) {
        return get("cause", causeTypeId, causeTypes);
    }

    @Override
    public Cause getCause(CauseId causeId) {
        return get("cause", causeId, causes);
    }

    @Override
    public FaultEvent getFaultEvent(FaultEventId faultEventId) {
        return get("faultEvent", faultEventId, events);
    }

    @Override
    public Collection<FaultEvent> getEvents(FaultTypeId faultTypeId, Long offset, Long count) {
        return listLookup(this.faultTypeFaultEvents, faultTypeId, offset, count);
    }

    @Override
    public Collection<FaultEvent> getEvents(FaultId faultId, Long offset, Long count) {
        return listLookup(faultFaultEvents, faultId, offset, count);
    }

    @Override
    public long limit() {
        return globalSequence.get();
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
    public List<FaultEvent> feed(long offset, long count) {
        return globalFeed(offset, count);
    }

    @Override
    public List<FaultEvent> feed(FaultTypeId id, long offset, long count) {
        return typedFeed(id, this.faultTypeFaultEvents, FaultEvent::getFaultTypeSequence, offset, count);
    }

    @Override
    public List<FaultEvent> feed(FaultId id, long offset, long count) {
        return typedFeed(id, this.faultFaultEvents, FaultEvent::getFaultSequence, offset, count);
    }

    @Override
    public Optional<FaultEvent> lastFaultEvent(FaultTypeId id) {
        return streamLookup(faultTypeFaultEvents, id)
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
        return streamLookup(faultTypeFaults, id).flatMap(fault ->
            streamLookup(faultFaultEvents, fault.getId()));
    }

    private List<FaultEvent> globalFeed(long offset, long count) {
        return delimited(
            faultEvents.stream()
                .filter(faultEvent ->
                    faultEvent.getGlobalSequence() >= offset),
            count
        ).collect(Collectors.toUnmodifiableList());
    }

    private static <I extends Id, T> List<T> typedFeed(
        I id,
        Map<I, Collection<T>> map,
        Function<T, Long> sequencer,
        long offset,
        long count
    ) {
        return delimited(
            listLookup(map, id).stream()
                .filter(specimen ->
                    sequencer.apply(specimen) >= offset),
            count
        ).collect(Collectors.toUnmodifiableList());
    }

    private static <T> Stream<T> delimited(Stream<T> candidates, long count) {
        return count > 0 ? candidates.limit(count) : candidates;
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

    private static <I extends Id, T extends Identifiable<I>> Consumer<T> put(Map<I, T> map) {
        return t -> put(map, t);
    }

    private static <I extends Id, T extends Identifiable<I>> void put(Map<I, T> map, T t) {
        map.putIfAbsent(t.getId(), t);
    }

    private static <I extends Id, T extends Identifiable<I>> T stored(Map<I, T> map, T t) {
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
