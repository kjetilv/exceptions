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

package unearth.jdbc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.core.FaultFeed;
import unearth.core.FaultStats;
import unearth.core.FaultStorage;
import unearth.jdbc.Session.Outcome;
import unearth.munch.base.Hashable;
import unearth.munch.base.Hashed;
import unearth.munch.id.CauseId;
import unearth.munch.id.CauseStrandId;
import unearth.munch.id.FaultId;
import unearth.munch.id.FaultStrandId;
import unearth.munch.id.FeedEntryId;
import unearth.munch.id.Id;
import unearth.munch.model.Cause;
import unearth.munch.model.CauseStrand;
import unearth.munch.model.Fault;
import unearth.munch.model.FaultEvent;
import unearth.munch.model.FaultStrand;
import unearth.munch.model.FeedEntry;
import unearth.munch.model.LogEntry;
import unearth.munch.print.CauseFrame;
import unearth.util.Streams;

import static unearth.jdbc.Session.Outcome.INSERTED;
import static unearth.jdbc.Session.Outcome.INSERTED_AND_UPDATED;
import static unearth.jdbc.Session.Outcome.NOOP;
import static unearth.jdbc.Sql.*;

public class JdbcStorage implements FaultStorage, FaultFeed, FaultStats {
    
    private static final Logger log = LoggerFactory.getLogger(JdbcStorage.class);
    
    private final DataSource dataSource;
    
    private final String schema;
    
    private final Clock clock;
    
    public JdbcStorage(DataSource dataSource, String schema, Clock clock) {
        this.dataSource = dataSource;
        this.schema = schema;
        this.clock = clock;
    }
    
    @Override
    public Runnable initStorage() {
        return new JdbcSetup(dataSource, schema);
    }
    
    @Override
    public void close() {
    }
    
    @Override
    public FeedEntry store(LogEntry logEntry, Fault fault, Throwable throwable) {
        return inSession(session -> {
            store(fault, session);
            FaultEvent event = newEvent(logEntry, fault, throwable);
            return store(session, event);
        });
    }
    
    @Override
    public Optional<Fault> getFault(FaultId faultId) {
        return inSession(session ->
            loadFault(session, faultId));
    }
    
    @Override
    public Optional<FaultStrand> getFaultStrand(FaultStrandId faultStrandId) {
        return inSession(session ->
            loadFaultStrand(session, faultStrandId));
    }
    
    @Override
    public Optional<FeedEntry> getFeedEntry(FeedEntryId faultEventId) {
        return inSession(session ->
            loadFeedEntry(session, faultEventId));
    }
    
    @Override
    public Optional<CauseStrand> getCauseStrand(CauseStrandId causeId) {
        return inSession(session ->
            loadCauseStrand(session, causeId));
    }
    
    @Override
    public Optional<Cause> getCause(CauseId causeId) {
        return inSession(session ->
            loadCause(session, causeId));
    }
    
    @Override
    public void reset() {
    }
    
    @Override
    public OptionalLong limit() {
        return opt(inSession(Sql::loadLimit));
    }
    
    @Override
    public OptionalLong limit(FaultStrandId id) {
        return opt(inSession(session ->
            loadLimit(session, id, "select seq from fault_strand_sequence where id = ?")));
    }
    
    @Override
    public OptionalLong limit(FaultId id) {
        return opt(inSession(session ->
            loadLimit(session, id, "select seq from fault_sequence where id = ?")));
    }
    
    @Override
    public List<FeedEntry> feed(long offset, long count) {
        return count <= 0 ? Collections.emptyList()
            : loadFaultEvents(
                "select fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                    "  from feed_entry " +
                    "  where global_seq > ? limit ?",
                stmt -> stmt
                    .set(offset)
                    .set(count));
    }
    
    @Override
    public List<FeedEntry> feed(FaultStrandId id, long offset, long count) {
        return count <= 0 ? Collections.emptyList()
            : loadFaultEvents(
                "select fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                    "   from feed_entry" +
                    "   where fault_strand = ? and global_seq > ? limit ?",
                stmt -> stmt
                    .set(id)
                    .set(offset)
                    .set(count));
    }
    
    @Override
    public List<FeedEntry> feed(FaultId id, long offset, long count) {
        return count <= 0 ? Collections.emptyList()
            : loadFaultEvents(
                "select fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                    "  from feed_entry" +
                    "  where fault = ? and global_seq > ? limit ?",
                stmt -> stmt
                    .set(id)
                    .set(offset)
                    .set(count));
    }
    
    @Override
    public Optional<FeedEntry> getLastFeedEntry(FaultId id, Instant sinceTime) {
        return inSession(session ->
            loadFeedEntries(id, session).stream().findFirst());
    }
    
    @Override
    public Optional<FeedEntry> getLastFeedEntry(FaultStrandId id, Instant sinceTime) {
        return inSession(session ->
            Sql.loadFeedEntries(session, id, sinceTime).stream().findFirst());
    }
    
    @Override
    public Optional<FeedEntry> getLastFeedEntry(FaultId id, Instant sinceTime, Long ceiling) {
        return inSession(session ->
            Sql.loadFeedEntries(session, id, sinceTime, ceiling).stream().findFirst());
    }
    
    @Override
    public long getFeedEntryCount(Instant sinceTime, Duration interval) {
        return count("select count(*) from feed_entry", null);
    }
    
    @Override
    public long getFeedEntryCount(FaultStrandId id, Instant sinceTime, Duration interval) {
        return count("select count(*) from feed_entry where fault_strand = ?", id);
    }
    
    @Override
    public long getFeedEntryCount(FaultId id, Instant sinceTime, Duration interval) {
        return count("select count(*) from feed_entry where fault = ?", id);
    }
    
    @Override
    public List<FeedEntry> getFeed(FaultStrandId id, Instant sinceTime, Duration period) {
        return getFaultEntries(id, sinceTime, period);
    }
    
    @Override
    public List<FeedEntry> getFeed(Instant sinceTime, Duration period) {
        return getFaultEntries(null, sinceTime, period);
    }
    
    @Override
    public List<FeedEntry> getFeed(FaultId id, Instant sinceTime, Duration period) {
        return getFaultEntries(id, sinceTime, period);
    }
    
    private <T> T inSession(Function<Session, T> fun) {
        try (Session session = new DefaultSession(dataSource, schema)) {
            return fun.apply(session);
        }
    }
    
    private List<FeedEntry> loadFaultEvents(String sql, Session.Set set) {
        return inSession(session ->
            session.select(sql, set, Sql::readFeedEntry));
    }
    
    private List<FeedEntry> getFaultEntries(Id id, Instant sinceTime, Duration period) {
        return inSession(session -> session.select(
            "select fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq from feed_entry" +
                (id == null ? "" : " where " + (id instanceof FaultId ? "fault" : "fault_strand") + " = ?") +
                (sinceTime == null ? ""
                    : " and time > ?" + (
                        period == null ? ""
                            : " and time <= ? ")),
            stmt ->
                forPeriod(
                    sinceTime(id == null ? stmt : stmt.set(id), sinceTime),
                    sinceTime,
                    period),
            Sql::readFeedEntry));
    }
    
    private long count(String countSql, Id id) {
        Session.Set parSet = id == null ? null : stmt -> stmt.set(id);
        return inSession(session -> session.selectOne(
            countSql,
            parSet,
            Session.Res::getLong)
        ).orElse(0L);
    }
    
    private FaultEvent newEvent(LogEntry logEntry, Fault fault, Throwable throwable) {
        return new FaultEvent(
            throwable == null ? null : System.identityHashCode(throwable),
            fault,
            logEntry,
            Instant.now(clock));
    }
    
    private static Session.Existence<FaultStrandId> ifExistsId(
        Session session,
        String sql,
        Hashed id,
        Function<UUID, FaultStrandId> gen
    ) {
        return session.exists(sql, stmt -> stmt.set(id), res -> gen.apply(res.getUUID()));
    }
    
    private static <I, T> Map<I, T> byId(Collection<T> vs, Function<T, I> identifier) {
        return vs.stream()
            .collect(Collectors.groupingBy(identifier, HashMap::new, Collectors.toSet()))
            .entrySet().stream()
            .filter(JdbcStorage::singleEntry)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().iterator().next()));
    }
    
    private static Optional<Fault> loadFault(Session session, FaultId faultId) {
        return ifExistsFaultStrandId(session, faultId)
            .thenLoad(faultStrandId ->
                loadFaultStrand(session, faultStrandId))
            .map(faultStrand ->
                Fault.create(faultStrand, loadCauses(session, faultId)));
    }
    
    private static Session.Existence<FaultStrandId> ifExistsFaultStrandId(Session session, Hashed faultId) {
        return ifExistsId(session,
            "select fault_strand from fault where id = ?", faultId, FaultStrandId::new);
    }
    
    private static Optional<FaultStrand> loadFaultStrand(Session session, FaultStrandId faultStrandId) {
        List<CauseStrand> causeStrands = loadCauseStrands(session, faultStrandId);
        return causeStrands.isEmpty()
            ? Optional.empty()
            : Optional.of(FaultStrand.create(causeStrands));
    }
    
    private static List<Cause> loadCauses(Session session, FaultId faultId) {
        return loadCauseIds(session, faultId).stream()
            .map(id -> loadCause(session, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    private static List<CauseStrand> loadCauseStrands(Session session, FaultStrandId faultStrandId) {
        List<CauseStrandId> causeStrandIds = loadCauseStrandIds(session, faultStrandId);
        return causeStrandIds.stream()
            .map(id -> loadCauseStrand(session, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }
    
    private static Optional<Cause> loadCause(Session session, CauseId causeId) {
        return loadCauseStrandId(session, causeId)
            .flatMap(causeStrandId ->
                loadCauseStrand(session, causeStrandId))
            .flatMap(causeStrand ->
                Sql.loadCause(session, causeId, causeStrand));
    }
    
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static OptionalLong opt(Optional<Long> aLong) {
        return aLong.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }
    
    private static List<FeedEntry> loadFeedEntries(FaultId id, Session session) {
        return session.select(
            "select fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                " from feed_entry" +
                " where fault = ? and time > ? order by fault_seq desc limit 1",
            stmt ->
                stmt.set(id),
            Sql::readFeedEntry);
    }
    
    private static Stmt forPeriod(Stmt stmt, Instant sinceTime, Duration period) {
        return sinceTime == null ? stmt : stmt.set(sinceTime.plus(period));
    }
    
    private static Stmt sinceTime(Stmt stmt, Instant sinceTime) {
        return sinceTime == null ? stmt : stmt.set(sinceTime);
    }
    
    private static void store(Fault fault, Session session) {
        log.debug("Storing {}", fault);
        Outcome faultStrandOutcome = storeFaultStrand(fault.getFaultStrand(), session);
        if (inserted(faultStrandOutcome)) {
            log.debug("Inserted fault strand: {}", fault.getFaultStrand());
            Map<CauseStrand, Outcome> causeStrandOutcomes = storeCauseStrands(fault, session);
            causeStrandOutcomes.forEach((causeStrand, strandOutcome) -> {
                log.debug("Inserted cause strand: {}", causeStrand);
                if (inserted(storeCauseFrames(session, causeStrand))) {
                    log.debug("Inserted cause frames for {}", causeStrand);
                } else {
                    log.debug("No new cause frames inserted for {}", causeStrand);
                }
                linkCauseStrandToCauseFrames(session, causeStrand);
            });
            linkFaultStrandToCauseStrands(session, fault.getFaultStrand());
        } else {
            log.debug("Already known fault strand: {}", fault.getFaultStrand());
        }
        Outcome faultOutcome = storeFault(fault, session);
        if (inserted(faultOutcome)) {
            log.debug("Inserted fault: {}", fault);
            if (inserted(faultOutcome)) {
                Map<Cause, Outcome> causeOutcomes = storeCauses(fault, session);
                causeOutcomes.forEach((cause, causeOutCome) -> {
                    if (inserted(causeOutCome)) {
                        log.debug("Inserted cause: {}", cause);
                    } else {
                        log.debug("Already known cause: {}", cause);
                    }
                });
                linkFaultToCauses(session, fault);
            }
        } else {
            log.debug("Already known fault: {}", fault);
        }
    }
    
    private static boolean inserted(Outcome outcome) {
        return outcome == INSERTED || outcome == INSERTED_AND_UPDATED;
    }
    
    private static Map<CauseStrand, Outcome> storeCauseStrands(Fault fault, Session session) {
        return fault.getCauses().stream().map(Cause::getCauseStrand).collect(Collectors.toMap(
            Function.identity(),
            causeStrand ->
                storeCauseStrand(session, causeStrand)));
    }
    
    private static Map<Cause, Outcome> storeCauses(Fault fault, Session session) {
        return fault.getCauses().stream().collect(Collectors.toMap(
            Function.identity(),
            cause ->
                storeCause(session, cause)));
    }
    
    private static long updateFaultStrandSeq(Session session, FaultStrandId id) {
        return updateSequence(session, id,
            "select seq from fault_strand_sequence where id = ?",
            "update fault_strand_sequence set seq = ? where id = ?",
            "insert into fault_strand_sequence (seq, id) values (?, ?)");
    }
    
    private static long updateFaultSeq(Session session, FaultId id) {
        return updateSequence(session, id,
            "select seq from fault_sequence where id = ?",
            "update fault_sequence set seq = ? where id = ?",
            "insert into fault_sequence (seq, id) values (?, ?)");
    }
    
    private static long updateSequence(Session session, Id hashable, String select, String update, String insert) {
        return loadLimit(session, hashable, select)
            .map(seq ->
                sequenced(session, hashable, update, seq + 1L))
            .orElseGet(() ->
                sequenced(session, hashable, insert, 1L));
    }
    
    private static Long sequenced(Session session, Id hashable, String update, long inc) {
        session.effect(update, stmt -> stmt.set(inc).set(hashable));
        return inc;
    }
    
    private static long updateGlobalSeq(Session session) {
        return loadLimit(session).map(seq -> {
            long inc = seq + 1L;
            session.effect(
                "update global_sequence set seq = ? where id = 0",
                stmt ->
                    stmt.set(inc)
            );
            return inc;
        }).orElseGet(() -> {
            session.effect(
                "insert into global_sequence (id, seq) values (0, ?)",
                stmt ->
                    stmt.set(1L)
            );
            return 1L;
        });
    }
    
    private static Outcome storeCauseFrames(Session session, CauseStrand causeStrand) {
        List<CauseFrame> causeFrames = causeStrand.getCauseFrames();
        if (causeFrames.isEmpty()) {
            return NOOP;
        }
        Map<UUID, CauseFrame> identifiedFrames = byId(causeFrames);
        Session.MultiExistence<CauseFrame> multiExistence = session.exists(
            "select id from cause_frame where id in (" + args(identifiedFrames.keySet()) + ')',
            causeFrames,
            stmt ->
                Streams.quickReduce(identifiedFrames.keySet(), stmt, Stmt::set),
            res ->
                identifiedFrames.get(res.getUUID()));
        return multiExistence
            .onInsert(newFrames ->
                insertFrames(session, newFrames))
            .go();
    }
    
    private static <T extends Hashed> Map<UUID, T> byId(List<T> hs) {
        return byId(hs, Hashed::getHash);
    }
    
    private static Outcome storeCause(Session session, Cause cause) {
        return session.exists(
            "select id from cause where id = ?", setId(cause), getId()
        ).onInsert(() ->
            session.effect(
                "insert into cause (id, cause_strand, message) values (?, ?, ?)",
                stmt -> Setter.cause(stmt, cause)
                    .set(cause.getCauseStrand())
                    .set(cause.getMessage())
            )).go();
    }
    
    private static Outcome storeFault(Fault fault, Session session) {
        return session.exists(
            "select id from fault where id = ?", setId(fault), getId()
        ).onInsert(() ->
            session.effect(
                "insert into fault (id, fault_strand) values (?, ?)",
                stmt -> Setter.fault(stmt, fault)
                    .set(fault.getFaultStrand()))
        ).go();
    }
    
    private static Outcome storeFaultStrand(FaultStrand faultStrand, Session session) {
        return ifExistsId(session, "select id from fault_strand where id = ?", faultStrand, FaultStrandId::new)
            .onInsert(() ->
                session.effect(
                    "insert into fault_strand (id) values (?)",
                    stmt ->
                        Setter.faultStrand(stmt, faultStrand)
                )).go();
    }
    
    private static Session.Set setId(Hashable fault1) {
        return stmt -> stmt.set(fault1);
    }
    
    private static FeedEntry store(Session session, FaultEvent event) {
        FeedEntry entry = new FeedEntry(
            event,
            updateGlobalSeq(session),
            updateFaultStrandSeq(session, event.getFaultStrandId()),
            updateFaultSeq(session, event.getFaultId()));
        saveFeedEntry(session, entry);
        return entry;
    }
    
    private static <K, V> boolean singleEntry(Map.Entry<K, Set<V>> e) {
        if (e.getValue().isEmpty()) {
            return false;
        }
        if (e.getValue().size() == 1) {
            return true;
        }
        throw new IllegalStateException("Multiple elements for " + e.getKey() + ": " + e.getValue());
    }
}
