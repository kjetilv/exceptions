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

package no.scienta.unearth.jdbc;

import no.scienta.unearth.core.FaultFeed;
import no.scienta.unearth.core.FaultStats;
import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.jdbc.Session.Outcome;
import no.scienta.unearth.munch.base.Hashable;
import no.scienta.unearth.munch.id.*;
import no.scienta.unearth.munch.model.*;
import no.scienta.unearth.munch.print.CauseFrame;
import no.scienta.unearth.util.Streams;
import no.scienta.unearth.util.Util;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static no.scienta.unearth.jdbc.Session.Outcome.INSERTED;
import static no.scienta.unearth.jdbc.Session.Outcome.NOOP;

public class JdbcStorage implements FaultStorage, FaultFeed, FaultStats {

    private final DataSource dataSource;

    private final String schema;

    public JdbcStorage(DataSource dataSource, String schema) {
        this.dataSource = dataSource;
        this.schema = schema;
    }

    @Override
    public Runnable initStorage() {
        return new JdbcSetup(dataSource, schema);
    }

    @Override
    public FaultEvents store(LogEntry logEntry, Fault fault, Throwable throwable) {
        return inSession(session -> {
            Outcome faultStrandOutcome = storeFaultStrand(fault.getFaultStrand(), session);
            Outcome faultOutcome = storeFault(fault, session);
            if (faultOutcome == INSERTED) {
                if (faultStrandOutcome == INSERTED) {
                    Map<CauseStrand, Outcome> causeStrandOutcomes = storeCauseStrands(fault, session);
                    Map<Cause, Outcome> causeOutcomes = storeCauses(fault, session);

                    fault.getCauseStrands().forEach(causeStrand -> {
                        if (causeStrandOutcomes.get(causeStrand) == INSERTED) {
                            Outcome causeFramesOutcome = storeCauseFrames(session, causeStrand.getCauseFrames());
                            if (causeFramesOutcome == INSERTED) {
                                linkCauseFrames(session, causeStrand);
                            }
                        }
                    });
                    fault.getCauses().forEach(cause -> {
                        if (causeOutcomes.get(cause) == INSERTED) {
                        }
                    });
                }
            }
            FaultEvent stored = storeEvent(session, newFaultEvent(fault, logEntry, throwable));
            return new FaultEvents(stored, null);
        });
    }

    private FaultEvent newFaultEvent(Fault fault, LogEntry logEntry, Throwable throwable) {
        return new FaultEvent(
            throwable == null ? null : System.identityHashCode(throwable),
            fault,
            logEntry,
            Instant.now(),
            null);
    }

    private Map<CauseStrand, Outcome> storeCauseStrands(Fault fault, Session session) {
        return fault.getCauses().stream().map(Cause::getCauseStrand).collect(Collectors.toMap(
            Function.identity(),
            causeStrand ->
                storeCauseStrand(session, causeStrand)
        ));
    }

    private Map<Cause, Outcome> storeCauses(Fault fault, Session session) {
        return fault.getCauses().stream().collect(Collectors.toMap(
            Function.identity(),
            cause ->
                storeCause(session, cause)
        ));
    }

    @Override
    public Optional<Cause> getCause(CauseId causeId) {
        return inSession(session ->
            loadCause(session, causeId));
    }

    @Override
    public Optional<CauseStrand> getCauseStrand(CauseStrandId causeId) {
        return inSession(session ->
            loadCauseStrand(session, causeId));
    }

    @Override
    public Optional<FaultStrand> getFaultStrand(FaultStrandId faultStrandId) {
        return inSession(session ->
            loadFaultStrand(session, faultStrandId));
    }

    @Override
    public Optional<Fault> getFault(FaultId faultId) {
        return inSession(session ->
            loadFault(session, faultId));
    }

    @Override
    public Optional<FaultEvent> getFaultEvent(FaultEventId faultEventId) {
        return loadFaultEvent(
            "select (" +
                "fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                ") from fault_event where id = ?",
            stmt -> Setter.byId(stmt, faultEventId)
                .set(faultEventId));
    }

    @Override
    public List<FaultEvent> getFaultEvents(Instant sinceTime, Duration period) {
        return doGetFaultEvents(null, sinceTime, period);
    }

    @Override
    public List<FaultEvent> getFaultEvents(FaultId id, Instant sinceTime, Duration period) {
        return doGetFaultEvents(id, sinceTime, period);
    }

    @Override
    public List<FaultEvent> getFaultEvents(FaultStrandId id, Instant sinceTime, Duration period) {
        return doGetFaultEvents(id, sinceTime, period);
    }

    @Override
    public OptionalLong limit() {
        return opt(inSession(this::limit));
    }

    @Override
    public OptionalLong limit(FaultStrandId id) {
        return opt(inSession(session ->
            limit(session, id, "select seq from fault_strand_sequence where id = ?")));
    }

    @Override
    public OptionalLong limit(FaultId id) {
        return opt(inSession(session ->
            limit(session, id, "select seq from fault_sequence where id = ?")));
    }

    @Override
    public List<FaultEvent> feed(long offset, long count) {
        return loadFaultEvents(
            "select (" +
                "  fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                ") from fault_event where global_seq >= ? limit ?",
            stmt -> stmt
                .set(offset)
                .set(count));
    }

    @Override
    public List<FaultEvent> feed(FaultStrandId id, long offset, long count) {
        return loadFaultEvents(
            "select (" +
                "  fault, fault_strand, time, global_seq, fault_strand_seq, fault_seq" +
                ") from fault_event where fault = ? and global_seq >= ? limit ?",
            stmt -> stmt
                .set(id)
                .set(offset)
                .set(count));
    }

    @Override
    public List<FaultEvent> feed(FaultId id, long offset, long count) {
        return loadFaultEvents(
            "select (" +
                FaultEventFields.list() +
                ") from fault_event where fault_strand = ? and global_seq >= ? limit ?",
            stmt -> stmt
                .set(id)
                .set(offset)
                .set(count));
    }

    @Override
    public Optional<FaultEvent> getLastFaultEvent(FaultId id, Instant sinceTime) {
        return loadFaultEvent(
            "select (" +
                FaultEventFields.list() +
                ") from fault_event where fault = ? and time >= ? order by fault_seq desc limit 1",
            stmt ->
                stmt.set(id));
    }

    @Override
    public Optional<FaultEvent> getLastFaultEvent(FaultStrandId id, Instant sinceTime) {
        return loadFaultEvent(
            "select (" +
                FaultEventFields.list() +
                ") from fault_event where fault_strand = ? and time >= ? order by fault_seq desc limit 1",
            stmt -> stmt
                .set(id)
                .set(sinceTime));
    }

    @Override
    public Optional<FaultEvent> getLastFaultEvent(FaultId id, Instant sinceTime, Long ceiling) {
        return loadFaultEvent(
            "select (" +
                FaultEventFields.list() +
                ") from fault_event" +
                " where fault = ? and time >= ? and fault_seq <= ? order by fault_seq desc limit 1",
            stmt -> stmt
                .set(id)
                .set(sinceTime)
                .set(ceiling));
    }

    @Override
    public long getFaultEventCount(Instant sinceTime, Duration interval) {
        return count("select count(*) from fault_event", null);
    }

    @Override
    public long getFaultEventCount(FaultStrandId id, Instant sinceTime, Duration interval) {
        return count("select count(*) from fault_event where fault_strand = ?", id);
    }

    @Override
    public long getFaultEventCount(FaultId id, Instant sinceTime, Duration interval) {
        return count("select count(*) from fault_event where fault = ?", id);
    }

    @Override
    public void close() {
    }

    @Override
    public void reset() {

    }

    private List<FaultEvent> doGetFaultEvents(Id id, Instant sinceTime, Duration period) {
        return inSession(session ->
            session.selectOpt(
                "select (" + FaultEventFields.list() + ") from fault_events" +
                    (id == null ? "" : " where " + (id instanceof FaultId ? "fault" : "fault_strand") + " = ?") +
                    (sinceTime == null ? ""
                        : " and time >= ?" + (
                        period == null ? ""
                            : " and time <= ? ")),
                stmt -> forPeriod(
                    sinceTime(id == null ? stmt : stmt.set(id), sinceTime),
                    sinceTime,
                    period),
                faultEventReader()));
    }

    private Session.Sel<Optional<FaultEvent>> faultEventReader() {
        return res ->
            res.ifNextOne(this::readActualFaultEvent);
    }

    private long count(String countSql, Id id) {
        Session.Set parSet = id == null ? null : stmt -> stmt.set(id);
        return inSession(session -> session.selectOne(
            countSql,
            parSet,
            Res::getLong)
        ).orElse(0L);
    }

    private <T> List<Indexed<T>> indexed(List<T> ts) {
        return IntStream.range(0, ts.size())
            .mapToObj(i ->
                new Indexed<>(i, ts.get(i)))
            .collect(Collectors.toList());
    }

    private <T> T inSession(Function<Session, T> fun) {
        try (Session session = new DefaultSession(dataSource, schema)) {
            return fun.apply(session);
        }
    }

    private long updateFaultSeq(Session session, FaultEvent faultEvent) {
        return updateFaultSeq(session, faultEvent.getFault().getId());
    }

    private long updateFaultSeq(Session session, FaultId id) {
        return updateSequence(session, id,
            "select seq from fault_sequence where id = ?",
            "update fault_sequence set seq = ? where id = ?",
            "insert into fault_sequence (seq, id) values (?, ?)");
    }

    private long updateFaultStrandSeq(Session session, FaultEvent faultEvent) {
        return updateFaultStrandSeq(session, faultEvent.getFault().getFaultStrand().getId());
    }

    private long updateFaultStrandSeq(Session session, FaultStrandId id) {
        return updateSequence(session, id,
            "select seq from fault_strand_sequence where id = ?",
            "update fault_strand_sequence set seq = ? where id = ?",
            "insert into fault_strand_sequence (seq, id) values (?, ?)");
    }

    private long updateSequence(
        Session session, Id hashable, String select, String update, String insert
    ) {
        return limit(session, hashable, select)
            .map(seq ->
                sequenced(session, hashable, update, seq + 1L))
            .orElseGet(() ->
                sequenced(session, hashable, insert, 1L));
    }

    private Long sequenced(Session session, Id hashable, String update, long inc) {
        session.update(update, stmt -> stmt
            .set(inc)
            .set(hashable));
        return inc;
    }

    private Optional<Long> limit(Session session, Id id, String select) {
        return session.selectOne(select, stmt -> stmt.set(id), Res::getLong);
    }

    private long getGlobalSeq(Session session) {
        return limit(session).map(seq -> {
            long inc = seq + 1L;
            session.update(
                "update global_sequence set seq = ? where id = 0",
                stmt ->
                    stmt.set(inc)
            );
            return inc;
        }).orElseGet(() -> {
            session.update(
                "insert into global_sequence (id, seq) values (0, ?)",
                stmt ->
                    stmt.set(1L)
            );
            return 1L;
        });
    }

    private Optional<Long> limit(Session session) {
        return session.selectOne(
            "select seq from global_sequence where id = 0",
            Res::getLong
        );
    }

    private Optional<FaultEvent> loadFaultEvent(String sql, Session.Set set) {
        return inSession(session ->
            session.selectMaybeOne(sql, set, faultEventReader()));
    }

    private Outcome storeCauseFrames(Session session, List<CauseFrame> causeFrames) {
        if (causeFrames.isEmpty()){
            return NOOP;
        }
        Map<UUID, CauseFrame> identifiedFrames = Util.byId(causeFrames, CauseFrame::getHash);
        return session.exists(
            "select id from cause_frame where id in" +
                " (" + causeFrames.stream().map(__ -> "?").collect(Collectors.joining(", ")) + ")",
            causeFrames,
            stmt ->
                Streams.quickReduce(causeFrames.stream(), (stmt1, causeFrame) -> stmt.set(causeFrame)),
            res ->
                identifiedFrames.get(res.getUUID())
        ).onInsert(newFrames ->
            session.updateBatch(
                "insert into cause_frame (" +
                    "  id, class_loader, module, module_ver, class_name, method, file, line, native" +
                    ") values (" +
                    "  ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    ")",
                newFrames,
                (stmt, cf) -> Setter.causeFrame(stmt, cf)
                    .set(cf.classLoader())
                    .set(cf.module())
                    .set(cf.moduleVer())
                    .set(cf.className())
                    .set(cf.method())
                    .set(cf.file())
                    .set(cf.line())
                    .set(cf.naytiv())
            )
        ).go();
    }

    private void linkCauseFrames(Session session, CauseStrand causeStrand) {
        session.updateBatch(
            "insert into cause_strand_2_cause_frame (" +
                "  cause_strand, seq, cause_frame" +
                ") values (" +
                "  ?, ?, ?" +
                ")",
            indexed(causeStrand.getCauseFrames()),
            (stmt, item) -> Setter.list(stmt, causeStrand)
                .set(item.getIndex())
                .set(item.getT()));
    }

    private Outcome storeCauseStrand(Session session, CauseStrand causeStrand) {
        return session.exists(
            "select id from cause_strand where id = ?", stmt -> stmt.set(causeStrand), getId()
        ).onInsert(() ->
            session.update(
                "insert into cause_strand (id, class_name) values (?, ?)",
                stmt -> Setter.causeStrand(stmt, causeStrand)
                    .set(causeStrand.getClassName()))
        ).go();
    }

    private Outcome storeCause(Session session, Cause cause) {
        return session.exists(
            "select id from cause where id = ?", setId(cause), getId()
        ).onInsert(() ->
            session.update(
                "insert into cause (id, cause_strand, message) values (?, ?, ?)",
                stmt -> Setter.cause(stmt, cause)
                    .set(cause.getCauseStrand())
                    .set(cause.getMessage())
            )).go();
    }

    private Outcome storeFault(Fault fault, Session session) {
        return session.exists(
            "select id from fault where id = ?", setId(fault), getId()
        ).onInsert(() ->
            session.update(
                "insert into fault (id, fault_strand) values (?, ?)",
                stmt -> Setter.fault(stmt, fault)
                    .set(fault.getFaultStrand()))
        ).go();
    }

    private Outcome storeFaultStrand(FaultStrand faultStrand, Session session) {
        return session.exists(
            "select id from fault_strand where id = ?", setId(faultStrand), getId()
        ).onInsert(() ->
            session.update(
                "insert into fault_strand (id) values (?)",
                stmt ->
                    Setter.faultStrand(stmt, faultStrand)
            )).go();
    }

    private Session.Sel<UUID> getId() {
        return Res::getUUID;
    }

    private Session.Set setId(Hashable fault1) {
        return stmt -> stmt.set(fault1);
    }

    private FaultEvent storeEvent(Session session, FaultEvent faultEvent) {
        FaultEvent event = faultEvent.sequence(
            getGlobalSeq(session),
            updateFaultStrandSeq(session, faultEvent),
            updateFaultSeq(session, faultEvent));
        session.update(
            "insert into fault_event " +
                "(id, fault, fault_strand, time, global_seq, fault_seq, fault_strand_seq)" +
                " values " +
                "(?, ?, ?, ?, ?, ?, ?)",
            stmt -> Setter.faultEvent(stmt, faultEvent)
                .set(event.getFault())
                .set(event.getFault().getFaultStrand())
                .set(event.getTime())
                .set(event.getGlobalSequenceNo())
                .set(event.getFaultSequenceNo())
                .set(event.getFaultStrandSequenceNo())
        );
        return event;
    }

    private List<FaultEvent> loadFaultEvents(String sql, Session.Set set) {
        return inSession(session ->
            session.selectOpt(sql, set, faultEventReader()));
    }

    private FaultEvent readActualFaultEvent(Res res) {
        return new FaultEvent(
            new FaultId(res.getUUID()),
            new FaultStrandId(res.getUUID()),
            res.getInstant()
        ).sequence(
            res.getLong(),
            res.getLong(),
            res.getLong());
    }

    private Optional<Fault> loadFault(Session session, FaultId faultId) {
        return session.selectOne(
            "select fault_strand from fault where id = ?",
            stmt ->
                stmt.set(faultId),
            res ->
                new FaultStrandId(res.getUUID())
        ).flatMap(faultStrandId ->
            loadFaultStrand(session, faultStrandId)
        ).map(faultStrand ->
            Fault.create(faultStrand, loadCauses(session, faultId)));
    }

    private Optional<FaultStrand> loadFaultStrand(Session session, FaultStrandId faultStrandId) {
        List<CauseStrand> causeStrands =
            loadCauseStrands(session, faultStrandId);
        return causeStrands.isEmpty()
            ? Optional.empty()
            : Optional.of(FaultStrand.create(causeStrands));
    }

    private List<Cause> loadCauses(Session session, FaultId faultId) {
        List<CauseId> causeIds = session.select(
            "select cause from fault_2_cause where id = ?",
            stmt ->
                stmt.set(faultId),
            res ->
                new CauseId(res.getUUID()));
        return causeIds.stream()
            .map(id -> loadCause(session, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<Cause> loadCause(Session session, CauseId causeId) {
        return session.selectOne(
            "select cause_strand from cause where id = ?",
            stmt ->
                stmt.set(causeId),
            res ->
                new CauseStrandId(res.getUUID())
        ).flatMap(causeStrandId ->
            loadCauseStrand(session, causeStrandId)
        ).flatMap(causeStrand ->
            session.selectOne(
                "select message from cause where id = ?",
                stmt ->
                    stmt.set(causeId),
                res ->
                    Cause.create(res.getString(), causeStrand)));
    }

    private List<CauseStrand> loadCauseStrands(Session session, FaultStrandId faultStrandId) {
        List<CauseStrandId> causeStrandIds = session.select(
            "select cause_strand from fault_strand_2_cause_strand " +
                "where fault_strand = ? " +
                "order by seq asc",
            stmt ->
                stmt.set(faultStrandId),
            res ->
                new CauseStrandId(res.getUUID()));
        return causeStrandIds.stream()
            .map(id -> loadCauseStrand(session, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<CauseStrand> loadCauseStrand(Session session, CauseStrandId causeStrandId) {
        return session.selectOne(
            "select (class_name) from cause_strand where id = ?",
            stmt ->
                stmt.set(causeStrandId),
            Res::getString
        ).map(className ->
        {
            List<CauseFrame> causeFrames = session.select(
                "select (" +
                    "  cf.id, cf.class_loader, cf.module, cf.module_ver, " +
                    "  cf.class_name, cf.method, cf.file, cf.line, cf.native" +
                    ") from cause_strand cs, cause_strand_2_cause_frame cs2cf, cause_frame c" +
                    " where cs2cf.cause_frame = cf.id and cs2cf.cause_strand = ?" +
                    " order by cs2cf.seq asc",
                stmt ->
                    stmt.set(causeStrandId),
                res ->
                    new CauseFrame(
                        CauseFrame.ClassLoader(res.getString()),
                        CauseFrame.Module(res.getString()),
                        CauseFrame.ModuleVer(res.getString()),
                        CauseFrame.ClassName(res.getString()),
                        CauseFrame.Method(res.getString()),
                        CauseFrame.File(res.getString()),
                        res.getInt(),
                        res.getBoolean()
                    ));
            return CauseStrand.create(className, causeFrames);
        });
    }

    private static Stmt forPeriod(Stmt stmt, Instant sinceTime, Duration period) {
        return sinceTime == null ? stmt : stmt.set(sinceTime.plus(period));
    }

    private static Stmt sinceTime(Stmt stmt, Instant sinceTime) {
        return sinceTime == null ? stmt : stmt.set(sinceTime);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static OptionalLong opt(Optional<Long> aLong) {
        return aLong.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }
}
