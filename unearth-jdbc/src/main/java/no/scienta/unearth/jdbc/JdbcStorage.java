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

import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.munch.id.*;
import no.scienta.unearth.munch.model.*;
import no.scienta.unearth.munch.print.CauseFrame;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JdbcStorage implements FaultStorage {

    private final DataSource dataSource;

    JdbcStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public FaultEvents store(LogEntry logEntry, Fault fault, Throwable throwable) {
        return inSession(session -> {
            fault.getCauses().forEach(cause -> {
                CauseStrand causeStrand = cause.getCauseStrand();
                insertCauseFrames(session, causeStrand.getCauseFrames());
                linkCauseFrames(session, causeStrand);
                insertCause(session, cause);
            });
            insertFaultStrand(fault.getFaultStrand(), session);
            insertFault(fault, session);
            FaultEvent event = storedEvent(session, new FaultEvent(
                System.identityHashCode(throwable),
                fault,
                logEntry,
                Instant.now(),
                null));
            return new FaultEvents(
                event,
                null);
        });
    }

    private FaultEvent storedEvent(Session session, FaultEvent faultEvent) {
        FaultEvent seqd = faultEvent.sequence(
            getGlobalSeq(session),
            getFaultStrandSeq(session, faultEvent),
            getFaultSeq(session, faultEvent));
        session.insert(
            "insert into fault_event (" +
                "  id, fault, fault_strand, time, global_seq, fault_seq, fault_strand_seq" +
                ") values (" +
                "  ?,?,?,?,?,?,?,?,?" +
                ")",
            stmt -> stmt
                .set(seqd)
                .set(seqd.getFault())
                .set(seqd.getFault().getFaultStrand())
                .set(seqd.getTime())
                .set(seqd.getGlobalSequenceNo())
                .set(seqd.getFaultSequenceNo())
                .set(seqd.getFaultStrandSequenceNo())
        );
        return seqd;
    }

    private long getGlobalSeq(Session session) {
        return session.selectOne(
            "select seq from global_sequence where id = 0",
            Res::getLong
        ).map(seq -> {
            long inc = seq + 1L;
            session.insert(
                "update global_sequence set seq = ? where id = 0",
                stmt ->
                    stmt.set(inc)
            );
            return inc;
        }).orElseGet(() -> {
            session.insert(
                "insert into global_sequence (id, seq) values (0, ?)",
                stmt ->
                    stmt.set(1L)
            );
            return 1L;
        });
    }

    private long getFaultStrandSeq(Session session, FaultEvent faultEvent) {
        return session.selectOne(
            "select seq from fault_strand_sequence where id = ?",
            stmt ->
                stmt.set(faultEvent.getFault().getFaultStrand()),
            Res::getLong
        ).map(seq -> {
            long inc = seq + 1L;
            session.insert(
                "update fault_strand_sequence set seq = ? where id = ?",
                stmt -> {
                    stmt.set(inc);
                    stmt.set(faultEvent.getFault().getFaultStrand());
                });
            return inc;
        }).orElseGet(() -> {
            session.insert(
                "insert into fault_strand_sequence (id, seq) values (?, ?)",
                stmt -> {
                    stmt.set(faultEvent.getFault().getFaultStrand());
                    stmt.set(1L);
                }
            );
            return 1L;
        });
    }

    private long getFaultSeq(Session session, FaultEvent faultEvent) {
        return session.selectOne(
            "select seq from fault_sequence where id = ?",
            stmt ->
                stmt.set(faultEvent.getFault()),
            Res::getLong
        ).map(seq -> {
            long inc = seq + 1L;
            session.insert(
                "update fault_sequence set seq = ? where id = ?",
                stmt -> {
                    stmt.set(inc);
                    stmt.set(faultEvent.getFault());
                });
            return inc;
        }).orElseGet(() -> {
            session.insert(
                "insert into fault_sequence (id, seq) values (?, ?)",
                stmt -> {
                    stmt.set(faultEvent.getFault());
                    stmt.set(1L);
                }
            );
            return 1L;
        });
    }

    @Override
    public Optional<Cause> getCause(CauseId causeId) {
        return inSession(session -> cause(session, causeId));
    }

    @Override
    public Optional<CauseStrand> getCauseStrand(CauseStrandId causeId) {
        return inSession(session -> causeStrand(session, causeId));
    }

    @Override
    public Optional<FaultStrand> getFaultStrand(FaultStrandId faultStrandId) {
        return inSession(session -> faultStrand(session, faultStrandId));
    }

    @Override
    public Optional<Fault> getFault(FaultId faultId) {
        return inSession(session -> fault(session, faultId));
    }

    @Override
    public Collection<FaultEvent> getEvents(FaultStrandId faultStrandId, Long offset, Long count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<FaultEvent> getEvents(FaultId faultId, Long offset, Long count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<FaultEvent> getFaultEvent(FaultEventId faultEventId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("reset");
    }

    private <T> List<Indexed<T>> indexed(List<T> ts) {
        return IntStream.range(0, ts.size())
            .mapToObj(i ->
                new Indexed<>(i, ts.get(i)))
            .collect(Collectors.toList());
    }

    private <T> T inSession(Function<Session, T> fun) {
        try (Session session = new Session(dataSource)) {
            return fun.apply(session);
        }
    }

    private void insertFault(Fault fault, Session session) {
        session.insert(
            "insert into fault (id, fault_strand) values (?, ?)",
            stmt -> stmt
                .set(fault.getHash())
                .set(fault.getFaultStrand().getHash()));
    }

    private void linkCauseFrames(Session session, CauseStrand causeStrand) {
        session.insertBatch(
            "insert into cause_strand_2_cause_frame (" +
                "  seq, cause_strand, cause_frame" +
                ") values (" +
                "  ?, ?, ?" +
                ")",
            indexed(causeStrand.getCauseFrames()),
            (stmt, item) -> {
                stmt.set(item.getIndex());
                stmt.set(causeStrand.getHash());
                stmt.set(item.getT().getHash());
            }
        );
    }

    private void insertFaultStrand(FaultStrand fault, Session session) {
        session.insert(
            "insert into fault_strand (id) values (?)",
            stmt -> stmt
                .set(fault.getHash()));
    }

    private void insertCause(Session session, Cause cause) {
        session.insert(
            "insert into cause (id, cause, message) values (?, ?, ?)",
            stmt -> stmt
                .set(cause.getHash())
                .set(cause.getCauseStrand().getHash())
                .set(cause.getMessage()));
    }

    private void insertCauseFrames(Session session, List<CauseFrame> causeFrames) {
        session.insertBatch(
            "insert into cause_frame (" +
                "  id, class_loader, module, module_ver, class_name, method, file, line, native" +
                ") values (" +
                "  ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ")",
            causeFrames,
            (stmt, cf) -> stmt
                .set(cf.getHash())
                .set(cf.classLoader())
                .set(cf.module())
                .set(cf.moduleVer())
                .set(cf.className())
                .set(cf.method())
                .set(cf.file())
                .set(cf.line())
                .set(cf.naytiv()));
    }

    private Optional<Fault> fault(Session session, FaultId faultId) {
        return session.selectOne(
            "select fault_strand from fault where id = ?",
            stmt ->
                stmt.set(faultId),
            res ->
                new FaultStrandId(res.getUUID())
        ).flatMap(faultStrandId ->
            faultStrand(session, faultStrandId)
        ).map(faultStrand ->
            Fault.create(faultStrand, causes(session, faultId)));
    }

    private Optional<FaultStrand> faultStrand(Session session, FaultStrandId faultStrandId) {
        List<CauseStrand> causeStrands =
            causeStrands(session, faultStrandId);
        return causeStrands.isEmpty()
            ? Optional.empty()
            : Optional.of(FaultStrand.create(causeStrands));
    }

    private List<Cause> causes(Session session, FaultId faultId) {
        List<CauseId> causeIds = session.select(
            "select cause from fault_2_cause where id = ?",
            stmt ->
                stmt.set(faultId),
            res ->
                new CauseId(res.getUUID()));
        return causeIds.stream()
            .map(id -> cause(session, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<Cause> cause(Session session, CauseId causeId) {
        return session.selectOne(
            "select cause_strand from cause where id = ?",
            stmt ->
                stmt.set(causeId),
            res ->
                new CauseStrandId(res.getUUID())
        ).flatMap(causeStrandId ->
            causeStrand(session, causeStrandId)
        ).flatMap(causeStrand ->
            session.selectOne(
                "select message from cause where id = ?",
                stmt ->
                    stmt.set(causeId),
                res ->
                    Cause.create(res.getString("message"), causeStrand)));
    }

    private List<CauseStrand> causeStrands(Session session, FaultStrandId faultStrandId) {
        List<CauseStrandId> causeStrandIds = session.select(
            "select cause_strand from fault_strand_2_cause_strand " +
                "where fault_strand = ? " +
                "order by seq asc",
            stmt ->
                stmt.set(faultStrandId),
            res ->
                new CauseStrandId(res.getUUID()));
        return causeStrandIds.stream()
            .map(id -> causeStrand(session, id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<CauseStrand> causeStrand(Session session, CauseStrandId causeStrandId) {
        return session.selectOne(
            "select (class_name) from cause_strand where id = ?",
            stmt ->
                stmt.set(causeStrandId),
            Res::getString
        ).map(className ->
            CauseStrand.create(className, session.select(
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
                        res.getString("cf.class_loader"),
                        res.getString("cf.module"),
                        res.getString("cf.module_ver"),
                        res.getString("cf.class_name"),
                        res.getString("cf.method"),
                        res.getString("cf.file"),
                        res.getInt("cf.line"),
                        res.getBoolean("cf.native")
                    ))));
    }

}
