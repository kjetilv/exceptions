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

package no.scienta.unearth.persistence;

import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.munch.id.*;
import no.scienta.unearth.munch.model.*;
import no.scienta.unearth.munch.print.CauseFrame;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Function;

public class JdbcStorage implements FaultStorage {

    private final DataSource dataSource;

    JdbcStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public CauseStrand getCauseStrand(CauseStrandId causeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FaultEvent store(Fault fault) {
        return inSession(session -> {
            fault.getCauses().forEach(cause -> {
                CauseStrand causeStrand = cause.getCauseStrand();
                causeStrand.getCauseFrames().forEach(causeFrame ->
                    insertCauseFrame(session, causeFrame));
                insertCause(session, cause);
            });
            insertFaultStrand(fault.getFaultStrand(), session);
            insertFault(fault, session);

            return new FaultEvent(
                fault, Instant.now(), 1L, 1L, 1L);
        });
    }

    private void insertFaultStrand(FaultStrand fault, Session session) {
        session.insert(
            "insert into fault_strand (id) values (?)",
            prep -> prep
                .set(fault.getHash())
        );
    }

    private void insertFault(Fault fault, Session session) {
        session.insert(
            "insert into fault " +
                "(id," +
                " fault_strand" +
                ") values (?, ?)",
            prep -> prep
                .set(fault.getHash())
                .set(fault.getFaultStrand().getHash())
        );
    }

    private void insertCause(Session session, Cause cause) {
        session.insert(
            "insert into cause " +
                "(id," +
                " cause," +
                " message" +
                ") values(?, ?, ?)",
            prep -> prep
                .set(cause.getHash())
                .set(cause.getCauseStrand().getHash())
                .set(cause.getMessage())
        );
    }

    private void insertCauseFrame(Session session, CauseFrame causeFrame) {
        session.insert(
            "insert into cause_frame " +
                "(id," +
                " class_loader," +
                " module," +
                " module_ver," +
                " class_name," +
                " method," +
                " file," +
                " line," +
                " native" +
                ") values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            ps -> ps
                .set(causeFrame.getHash())
                .set(causeFrame.classLoader())
                .set(causeFrame.module())
                .set(causeFrame.moduleVer())
                .set(causeFrame.className())
                .set(causeFrame.method())
                .set(causeFrame.file())
                .set(causeFrame.line())
                .set(causeFrame.naytiv()));
    }

    @Override
    public Fault getFault(FaultId faultId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FaultStrand getFaultStrand(FaultStrandId faultStrandId) {
        throw new UnsupportedOperationException();
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
    public FaultEvent getFaultEvent(FaultEventId faultEventId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cause getCause(CauseId causeId) {
        throw new UnsupportedOperationException();
    }

    private <T> T inSession(Function<Session, T> fun) {
        try (Session session = session()) {
            return fun.apply(session);
        }
    }

    private Session session() {
        return new Session(dataSource);
    }

    interface Inserter {

        void setParams(Prep statement);
    }

    @SuppressWarnings("UnusedReturnValue")
    interface Prep extends AutoCloseable {

        Prep set(String string);

        Prep set(boolean bool);

        Prep set(int i);

        Prep set(UUID uuid);
    }
}
