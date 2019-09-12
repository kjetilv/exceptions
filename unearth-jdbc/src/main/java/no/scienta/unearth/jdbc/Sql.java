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

import no.scienta.unearth.munch.base.Hashed;
import no.scienta.unearth.munch.id.*;
import no.scienta.unearth.munch.model.*;
import no.scienta.unearth.munch.print.CauseFrame;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class Sql {

    private Sql() {
    }

    static List<CauseId> loadCauseIds(Session session, FaultId faultId) {
        return session.select(
            "select cause from fault_2_cause where fault = ?",
            stmt ->
                stmt.set(faultId),
            res ->
                new CauseId(res.getUUID()));
    }

    static Optional<Cause> loadCause(Session session, CauseId causeId, CauseStrand causeStrand) {
        return session.selectOne(
            "select message from cause where id = ?",
            stmt ->
                stmt.set(causeId),
            res ->
                Cause.create(res.getString(), causeStrand));
    }

    static Optional<CauseStrand> loadCauseStrand(Session session, CauseStrandId causeStrandId) {
        return session.selectOne(
            "select (class_name) from cause_strand where id = ?",
            stmt ->
                stmt.set(causeStrandId),
            Session.Res::getString
        ).map(className ->
            CauseStrand.create(className, loadCauseFrames(session, causeStrandId)));
    }

    static Optional<CauseStrandId> loadCauseStrandId(Session session, CauseId causeId) {
        return session.selectOne(
            "select cause_strand from cause where id = ?",
            stmt ->
                stmt.set(causeId),
            res ->
                new CauseStrandId(res.getUUID())
        );
    }

    static List<CauseStrandId> loadCauseStrandIds(Session session, FaultStrandId faultStrandId) {
        return session.select(
            "select cause_strand from fault_strand_2_cause_strand " +
                "where fault_strand = ? " +
                "order by seq asc",
            stmt ->
                stmt.set(faultStrandId),
            res ->
                new CauseStrandId(res.getUUID()));
    }

    static void saveFeedEntry(Session session, FeedEntry entry) {
        session.update(
            "insert into feed_entry " +
                "(id, fault, fault_strand, time, global_seq, fault_seq, fault_strand_seq)" +
                " values " +
                "(?, ?, ?, ?, ?, ?, ?)",
            stmt -> Setter.feedEntry(stmt, entry)
                .set(entry.getFaultEvent().getFaultId())
                .set(entry.getFaultEvent().getFaultStrandId())
                .set(entry.getFaultEvent().getTime())
                .set(entry.getGlobalSequenceNo())
                .set(entry.getFaultSequenceNo())
                .set(entry.getFaultStrandSequenceNo()));
    }

    static Session.Outcome storeCauseStrand(Session session, CauseStrand causeStrand) {
        return session.exists(
            "select id from cause_strand where id = ?", stmt -> stmt.set(causeStrand), getId()
        ).onInsert(() ->
            session.update(
                "insert into cause_strand (id, class_name) values (?, ?)",
                stmt -> Setter.causeStrand(stmt, causeStrand)
                    .set(causeStrand.getClassName()))
        ).go();
    }

    static Session.Sel<UUID> getId() {
        return Session.Res::getUUID;
    }

    static List<FeedEntry> loadFeedEntries(Session session, FaultStrandId id, Instant sinceTime) {
        return session.select(
            "select (" + FeedEntryFields.list() + ") from feed_entry" +
                " where fault_strand = ? and time >= ? order by fault_seq desc limit 1",
            stmt -> stmt
                .set(id)
                .set(sinceTime),
            Sql::readFeedEntry
        );
    }

    static FeedEntry readFeedEntry(Session.Res res) {
        return new FeedEntry(
            new FaultEvent(
                new FaultId(res.getUUID()),
                new FaultStrandId(res.getUUID()),
                res.getInstant()),
            res.getLong(),
            res.getLong(),
            res.getLong());
    }

    static Optional<Long> loadLimit(Session session) {
        return session.selectOne(
            "select seq from global_sequence where id = 0",
            Session.Res::getLong
        );
    }

    static Optional<FeedEntry> loadFeedEntry(Session session, FeedEntryId faultEventId) {
        return session.select(
            "select " + FeedEntryFields.list() + " from feed_entry where id = ?",
            stmt ->
                Setter.byId(stmt, faultEventId),
            Sql::readFeedEntry
        ).stream().findFirst();
    }

    static List<FeedEntry> loadFeedEntries(Session session, FaultId id, Instant sinceTime, Long ceiling) {
        return session.select(
            "select (" + FeedEntryFields.list() + ") from feed_entry" +
                " where fault = ? and time >= ? and fault_seq <= ? order by fault_seq desc limit 1",
            stmt -> stmt
                .set(id)
                .set(sinceTime)
                .set(ceiling),
            Sql::readFeedEntry
        );
    }

    static Optional<Long> loadLimit(Session session, Id id, String sql) {
        return session.selectOne(sql, stmt -> stmt.set(id), Session.Res::getLong);
    }

    static void insertFrames(Session session, Collection<CauseFrame> newFrames) {
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
        );
    }

    static void linkFaultToCause(Session session, Fault fault) {
        session.updateBatch(
            "insert into fault_2_cause (" +
                "  fault, seq, cause" +
                ") values (" +
                "  ?, ?, ?" +
                ")",
            indexed(fault.getCauses()),
            (stmt, item) ->
                Setter.list(stmt, fault)
                    .set(item.getIndex())
                    .set(item.getT())
        );
    }

    static void linkFaultStrandToCauseStrands(Session session, FaultStrand faultStrand) {
        session.updateBatch(
            "insert into fault_strand_2_cause_strand (" +
                "  fault_strand, seq, cause_strand" +
                ") values (" +
                "  ?, ?, ?" +
                ")",
            indexed(faultStrand.getCauseStrands()),
            (stmt, item) ->
                Setter.list(stmt, faultStrand)
                    .set(item.getIndex())
                    .set(item.getT())
        );
    }

    static void linkCauseStrandToCauseFrames(Session session, CauseStrand causeStrand) {
        session.updateBatch(
            "insert into cause_strand_2_cause_frame (cause_strand, seq, cause_frame) values (?, ?, ?)",
            indexed(causeStrand.getCauseFrames()),
            (stmt, item) ->
                Setter.list(stmt, causeStrand)
                    .set(item.getIndex())
                    .set(item.getT()));
    }

    static Session.Existence<FaultStrandId> ifExistsId(
        Session session,
        String sql,
        Hashed id,
        Function<UUID, FaultStrandId> gen
    ) {
        return session.exists(
            sql,
            stmt ->
                stmt.set(id),
            res ->
                gen.apply(res.getUUID()));
    }

    private static List<CauseFrame> loadCauseFrames(Session session, CauseStrandId causeStrandId) {
        return session.select(
            "select " +
                " cf.class_loader, cf.module, cf.module_ver," +
                " cf.class_name, cf.method, cf.file, cf.line, cf.native" +
                " from cause_strand cs, cause_strand_2_cause_frame cs2cf, cause_frame cf" +
                " where" +
                "  cs2cf.cause_frame = cf.id and cs2cf.cause_strand = ?" +
                " order by" +
                "  cs2cf.seq asc",
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
                    res.getBoolean()));
    }

    private static <T> List<Indexed<T>> indexed(List<T> ts) {
        return IntStream.range(0, ts.size())
            .mapToObj(i ->
                new Indexed<>(i, ts.get(i)))
            .collect(Collectors.toList());
    }
}
