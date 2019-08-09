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

package no.scienta.unearth.analysis;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.api.core.metadata.Node;
import no.scienta.unearth.core.FaultSensor;
import no.scienta.unearth.munch.model.FaultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CassandraSensor implements FaultSensor {

    private final CqlSession cqlSession;

    public CassandraSensor(String host, int port, String dc) {
        this.cqlSession = CqlSession.builder()
            .addContactPoint(InetSocketAddress.createUnresolved(host, port))
            .withLocalDatacenter(dc)
            .build();

        inSession(session -> {
            Row row = session.execute(VERSION_QUERY).one();
            if (row == null) {
                throw new IllegalStateException("Failed to obtain version information");
            }
            log.info("Connected to Cassandra @ {}: {}",
                endPoints(session).map(EndPoint::asMetricPrefix).collect(Collectors.joining(", ")),
                row.getString(RELEASE_VERSION));
        });

        inSession(session -> {
            session.execute("CREATE KEYSPACE" +
                " IF NOT EXISTS " + KEYSPACE +
                " WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor' : 1}");
        });

        inKeyspace(session -> {
            session.execute(
                "CREATE TABLE IF NOT EXISTS fault (" +
                    "id uuid PRIMARY KEY," +
                    "faultStrand uuid" +
                    ")"
            );
            session.execute(
                "CREATE TABLE IF NOT EXISTS faultStrand(" +
                    "id uuid PRIMARY KEY" +
                    ")"
            );
            session.execute(
                "CREATE TABLE IF NOT EXISTS faultEvent(" +
                    "id uuid PRIMARY KEY," +
                    "fault uuid," +
                    "faultStrand uuid" +
                    ")"
            );
        });
    }

    @Override
    public void close() {
        cqlSession.close();
    }

    @Override
    public void register(FaultEvent faultEvent) {
        inKeyspace(session -> {
            exec(session, "INSERT INTO fault (id, faultStrand) VALUES (?, ?)",
                faultEvent.getFault().getId().getHash(), faultEvent.getFault().getFaultStrand().getId().getHash());
            exec(session, "INSERT INTO faultStrand (id) VALUES (?)",
                faultEvent.getFault().getFaultStrand().getId().getHash());
            exec(session, "INSERT INTO faultEvent (id, fault, faultStrand) VALUES (?, ?, ?)",
                faultEvent.getId().getHash(),
                faultEvent.getFault().getId().getHash(),
                faultEvent.getFault().getFaultStrand().getId().getHash());
        });
    }

    private void exec(CqlSession session, String stmt, Object... args) {
        PreparedStatement prepared = session.prepare(stmt);
        session.execute(prepared.bind(args));
    }

    private <T> T inKeyspace(Function<CqlSession, T> action) {
        return inSession(session -> {
            session.execute(USE_KEYSPACE);
            return action.apply(session);
        });
    }

    private <T> T inSession(Function<CqlSession, T> action) {
        try {
            return action.apply(cqlSession);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    private void inKeyspace(Consumer<CqlSession> action) {
        inSession(session -> {
            session.execute(USE_KEYSPACE);
            action.accept(session);
        });
    }

    private void inSession(Consumer<CqlSession> action) {
        try {
            action.accept(cqlSession);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    private Stream<EndPoint> endPoints(CqlSession session) {
        return session.getMetadata().getNodes().values().stream().map(Node::getEndPoint);
    }

    private static final Logger log = LoggerFactory.getLogger(CassandraSensor.class);

    private static final String RELEASE_VERSION = "release_version";

    private static final String VERSION_QUERY = "select " + RELEASE_VERSION + " from system.local";

    private static final String KEYSPACE = "unearth";

    private static final String USE_KEYSPACE = " USE " + KEYSPACE;
}
