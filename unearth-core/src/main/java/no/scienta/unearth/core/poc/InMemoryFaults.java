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
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.core.poc;

import no.scienta.unearth.core.FaultFeed;
import no.scienta.unearth.core.FaultSensor;
import no.scienta.unearth.core.FaultStats;
import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.munch.base.AbstractHashable;
import no.scienta.unearth.munch.id.*;
import no.scienta.unearth.munch.model.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryFaults
    implements FaultStorage, FaultStats, FaultFeed {

    private final Map<FaultId, Fault> faults = new HashMap<>();

    private final Map<FaultStrandId, FaultStrand> faultStrands = new HashMap<>();

    private final Map<FaultEventId, FaultEvent> events = new HashMap<>();

    private final Map<CauseStrandId, CauseStrand> causeStrands = new HashMap<>();

    private final Map<CauseId, Cause> causes = new HashMap<>();

    private final Collection<FaultEvent> faultEvents = new ArrayList<>();

    private final Map<Integer, UniqueIncident> hashCodes = new ConcurrentHashMap<>();

    private final Map<FaultStrandId, Collection<Fault>> faultStrandFaults = new HashMap<>();

    private final Map<FaultStrandId, Collection<FaultEvent>> faultStrandFaultEvents = new HashMap<>();

    private final Map<FaultId, Collection<FaultEvent>> faultFaultEvents = new HashMap<>();

    private final Object lock = new boolean[]{};

    private final AtomicLong globalSequence = new AtomicLong();

    private final Map<FaultStrandId, AtomicLong> faultStrandSequence = new LinkedHashMap<>();

    private final Map<FaultId, AtomicLong> faultSequence = new LinkedHashMap<>();

    private final FaultSensor faultSensor;

    private final Clock clock;

    public InMemoryFaults(FaultSensor faultSensor) {
        this(faultSensor, null);
    }

    public InMemoryFaults(FaultSensor faultSensor, Clock clock) {
        this.faultSensor = faultSensor;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public void reset() {
        Stream.of(
            faults,
            faultStrands,
            causes,
            causeStrands,
            faultStrandFaults,
            hashCodes,
            faultStrandSequence,
            faultSequence,
            faultStrandFaultEvents,
            faultFaultEvents,
            events
        ).forEach(Map::clear);
        faultEvents.clear();
        globalSequence.set(0);
    }

    @Override
    public void close() {
    }

    @Override
    public FaultEvents store(LogEntry logEntry, Fault fault, Throwable throwable) {
        synchronized (lock) {
            FaultEvent faultEvent = new FaultEvent(
                System.identityHashCode(throwable),
                fault,
                logEntry,
                Instant.now(clock),
                null);

            UniqueIncident existing = hashCodes.putIfAbsent(
                System.identityHashCode(throwable),
                new UniqueIncident(
                    System.identityHashCode(throwable),
                    new FaultEvents(faultEvent, eventBefore(faultEvent))));

            if (existing != null) {
                return existing.getFaultEvents();
            }

            FaultEvent sequenced = sequenced(faultEvent);
            return registered(sequenced, eventBefore(sequenced));
        }
    }

    private FaultEvent eventBefore(FaultEvent faultEvent) {
        long ceiling = faultEvent.isSequenced()
            ? faultEvent.getFaultSequenceNo()
            : currentSequence(faultSequence, faultEvent.getFault().getId()).get() + 1L;

        return getLastFaultEvent(
            faultEvent.getFault().getId(),
            null,
            ceiling
        ).orElse(null);
    }

    private FaultEvent sequenced(FaultEvent faultEvent) {
        return faultEvent.sequence(
            globalSequence.getAndIncrement(),
            increment(this.faultStrandSequence, faultEvent.getFault().getFaultStrand().getId()),
            increment(this.faultSequence, faultEvent.getFault().getId()));
    }

    private FaultEvents registered(FaultEvent sequenced, FaultEvent previous) {
        stored(this.events, sequenced);
        faultSensor.register(sequenced);
        FaultStrand faultStrand = sequenced.getFault().getFaultStrand();
        putInto(this.faultStrands, faultStrand);
        putInto(this.faults, sequenced.getFault());
        faultStrand.getCauseStrands().forEach(putInto(this.causeStrands));
        sequenced.getFault().getCauses().forEach(putInto(this.causes));

        faultEvents.add(sequenced);

        addTo(faultStrandFaults, faultStrand.getId(), sequenced.getFault());
        addTo(faultStrandFaultEvents, faultStrand.getId(), sequenced);
        addTo(faultFaultEvents, sequenced.getFault().getId(), sequenced);

        return new FaultEvents(sequenced, previous);
    }

    @Override
    public Optional<FaultStrand> getFaultStrand(FaultStrandId faultStrandId) {
        return get(faultStrandId, faultStrands);
    }

    @Override
    public Optional<Fault> getFault(FaultId faultStrandId) {
        return get(faultStrandId, faults);
    }

    @Override
    public Optional<CauseStrand> getCauseStrand(CauseStrandId causeStrandId) {
        return get(causeStrandId, causeStrands);
    }

    @Override
    public Optional<Cause> getCause(CauseId causeId) {
        return get(causeId, causes);
    }

    @Override
    public Optional<FaultEvent> getFaultEvent(FaultEventId faultEventId) {
        return get(faultEventId, events);
    }

    @Override
    public OptionalLong limit() {
        return opt(globalSequence.get());
    }

    @Override
    public OptionalLong limit(FaultStrandId id) {
        return getLimit(this.faultStrandSequence, id);
    }

    @Override
    public OptionalLong limit(FaultId id) {
        return getLimit(this.faultSequence, id);
    }

    @Override
    public List<FaultEvent> feed(long offset, long count) {
        return globalFeed(offset, count);
    }

    @Override
    public List<FaultEvent> feed(FaultStrandId id, long offset, long count) {
        return typedFeed(id, this.faultStrandFaultEvents, FaultEvent::getFaultStrandSequenceNo, offset, count);
    }

    @Override
    public List<FaultEvent> feed(FaultId id, long offset, long count) {
        return typedFeed(id, this.faultFaultEvents, FaultEvent::getFaultSequenceNo, offset, count);
    }

    @Override
    public Optional<FaultEvent> getLastFaultEvent(FaultId id, Instant sinceTime, Long ceiling) {
        return filter(
            sinceTime,
            streamLookup(faultFaultEvents, id),
            FaultEvent::getFaultSequenceNo,
            ceiling);
    }

    @Override
    public Optional<FaultEvent> getLastFaultEvent(FaultId id, Instant sinceTime) {
        return filter(
            null,
            streamLookup(faultFaultEvents, id),
            FaultEvent::getFaultStrandSequenceNo,
            null);
    }

    @Override
    public Optional<FaultEvent> getLastFaultEvent(FaultStrandId id, Instant sinceTime) {
        return filter(
            sinceTime,
            streamLookup(faultStrandFaultEvents, id),
            FaultEvent::getFaultStrandSequenceNo,
            null);
    }

    @Override
    public long getFaultEventCount(FaultStrandId id, Instant sinceTime, Duration interval) {
        return getFaultEvents(id, sinceTime, interval).size();
    }

    @Override
    public long getFaultEventCount(FaultId id, Instant sinceTime, Duration period) {
        return faultEvents.stream()
            .filter(event -> event.getFault().getId().equals(id))
            .filter(event -> event.getTime().isAfter(sinceTime) && event.getTime().isBefore(sinceTime.plus(period)))
            .count();
    }

    @Override
    public long getFaultEventCount(Instant sinceTime, Duration interval) {
        return faultEvents.size();
    }

    @Override
    public List<FaultEvent> getFaultEvents(FaultId id, Instant sinceTime, Duration period) {
        return faultEvents.stream()
            .filter(event -> event.getFaultId().equals(id))
            .filter(event -> event.getTime().isAfter(sinceTime) && event.getTime().isBefore(sinceTime.plus(period)))
            .collect(Collectors.toList());
    }

    @Override
    public List<FaultEvent> getFaultEvents(FaultStrandId id, Instant sinceTime, Duration period) {
        Instant limitTime = period == null ? null : sinceTime.plus(period);
        return getFaultEvents(id).stream()
            .filter(event -> event.getTime().isAfter(sinceTime) && event.getTime().isBefore(sinceTime.plus(period)))
            .collect(Collectors.toList());
    }

    @Override
    public List<FaultEvent> getFaultEvents(Instant sinceTime, Duration period) {
        return faultEvents.stream()
            .filter(event -> event.getTime().isAfter(sinceTime) && event.getTime().isBefore(sinceTime.plus(period)))
            .collect(Collectors.toList());
    }

    private <I extends Id, T> Optional<T> get(I id, Map<I, T> memoryMap) {
        return Optional.ofNullable(memoryMap.get(id));
    }

    public List<FaultEvent> getFaultEvents(FaultStrandId id) {
        return streamLookup(faultStrandFaults, id).flatMap(fault ->
            streamLookup(faultFaultEvents, fault.getId())).collect(Collectors.toList());
    }

    private Optional<FaultEvent> filter(
        Instant sinceTime,
        Stream<FaultEvent> faultEventStream,
        Function<FaultEvent, Long> getFaultSequenceNo,
        Long ceiling
    ) {
        return faultEventStream
            .filter(faultEvent ->
                sinceTime == null || faultEvent.getTime().isAfter(sinceTime))
            .filter(faultEvent ->
                ceiling == null || getFaultSequenceNo.apply(faultEvent) < ceiling)
            .max(Comparator.comparing(getFaultSequenceNo));
    }

    private List<FaultEvent> globalFeed(long offset, long count) {
        return delimited(
            faultEvents.stream()
                .filter(faultEvent ->
                    faultEvent.getGlobalSequenceNo() >= offset),
            count
        ).collect(Collectors.toList());
    }

    private static <I extends Id, T> List<T> typedFeed(
        I id,
        Map<I, Collection<T>> map,
        Function<T, Long> sequencer,
        long offset,
        long count
    ) {
        if (map.containsKey(id)) {
            return delimited(
                listLookup(map, id).stream()
                    .filter(t -> sequencer.apply(t) >= offset),
                count
            ).collect(Collectors.toList());
        }
        throw new IllegalArgumentException("No " + id + " events recorded");
    }

    private static <T> Stream<T> delimited(Stream<T> candidates, long count) {
        return count > 0 ? candidates.limit(count) : candidates;
    }

    private static <I extends Id> OptionalLong getLimit(Map<I, AtomicLong> seq, I id) {
        return opt(Optional.ofNullable(seq.get(id)).map(AtomicLong::longValue).orElse(0L));
    }

    private static <K> long increment(Map<K, AtomicLong> sequences, K id) {
        return currentSequence(sequences, id).getAndIncrement();
    }

    private static <K> AtomicLong currentSequence(Map<K, AtomicLong> sequences, K id) {
        return sequences.computeIfAbsent(id, __ -> new AtomicLong());
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
                (InMemoryFaults.class + " is for sub-ginormous data amounts ONLY!");
        }
        Collection<V> coll = map.getOrDefault(key, Collections.emptyList());
        if (offset == null || count == null || offset < 0 && count < 0 || offset == 0 && count <= coll.size()) {
            return Collections.unmodifiableCollection(coll);
        }
        if (offset >= coll.size()) {
            return Collections.emptyList();
        }
        int lastIndex = Math.min(offset.intValue() + count.intValue(), coll.size());
        return Collections.unmodifiableCollection(new ArrayList<>(coll).subList(offset.intValue(), lastIndex));
    }

    private static <I extends Id, T extends Identifiable<I>> Consumer<T> putInto(Map<I, T> map) {
        return t -> putInto(map, t);
    }

    private static <I extends Id, T extends Identifiable<I>> void putInto(Map<I, T> map, T t) {
        map.putIfAbsent(t.getId(), t);
    }

    private static <I extends Id, T extends Identifiable<I>> T stored(Map<I, T> map, T t) {
        T existing = map.putIfAbsent(t.getId(), t);
        if (existing == null) {
            return t;
        }
        throw new IllegalStateException("Already stored: " + existing.getId());
    }

    private static <K, V> void addTo(Map<K, Collection<V>> map, K k, V v) {
        map.computeIfAbsent(k, __ -> new ArrayList<>()).add(v);
    }

    private static OptionalLong opt(long l) {
        return l == 0 ? OptionalLong.empty() : OptionalLong.of(l);
    }

    static class UniqueIncident extends AbstractHashable {


        private final int systemHashCode;

        private final FaultEvents faultEvents;

        UniqueIncident(int systemHashCode, FaultEvents faultEvents) {
            this.systemHashCode = systemHashCode;
            this.faultEvents = Objects.requireNonNull(faultEvents, "faultEvent");
        }

        FaultEvents getFaultEvents() {
            return faultEvents;
        }

        @Override
        protected String toStringBody() {
            return systemHashCode + ":" + faultEvents;
        }

        @Override
        public void hashTo(Consumer<byte[]> h) {
            hash(h, systemHashCode);
            hash(h, faultEvents);
        }
    }
}
