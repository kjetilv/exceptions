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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import no.scienta.unearth.core.FaultSensor;
import no.scienta.unearth.munch.model.FaultEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CassandraSensor implements FaultSensor {

    public CassandraSensor(String host, int port) {
        this.cluster = Cluster.builder()
            .withoutJMXReporting()
            .addContactPoints(resolve(host))
            .withPort(port)
            .build();

        inSession(session -> {
            String version = session.execute(VERSION_QUERY).one().getString(RELEASE_VERSION);
            log.info("Connected to Cassandra @ {}: {}",
                cluster.getMetadata().getAllHosts().stream()
                    .map(Host::getSocketAddress)
                    .map(InetSocketAddress::getHostString)
                    .collect(Collectors.joining(", ")),
                version);
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
        });
    }

    @Override
    public void register(FaultEvent faultEvent) {
        inKeyspace(session -> {
            exec(session, "INSERT INTO fault (id, faultStrand) VALUES (?, ?)",
                faultEvent.getFault().getId().getHash(), faultEvent.getFault().getFaultStrand().getId().getHash());
            exec(session, "INSERT INTO faultStrand (id) VALUES (?)",
                faultEvent.getFault().getFaultStrand().getId().getHash());
        });
    }

    private void exec(Session session, String stmt, Object... args) {
        PreparedStatement prepared = session.prepare(stmt);
        session.execute(prepared.bind(args));
    }

    private <T> T inKeyspace(Function<Session, T> action) {
        return inSession(session -> {
            session.execute(USE_KEYSPACE);
            return action.apply(session);
        });
    }

    private <T> T inSession(Function<Session, T> action) {
        try (Session session = cluster.connect()) {
            return action.apply(session);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    private void inKeyspace(Consumer<Session> action) {
        inSession(session -> {
            session.execute(USE_KEYSPACE);
            action.accept(session);
        });
    }

    private void inSession(Consumer<Session> action) {
        try (Session session = cluster.connect()) {
            action.accept(session);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    private InetAddress resolve(String addr) {
        if (addr == null) {
            try {
                return InetAddress.getLoopbackAddress();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve localhost", e);
            }
        }
        try {
            return InetAddress.getByName(addr);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve " + addr, e);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(CassandraSensor.class);

    private static final String RELEASE_VERSION = "release_version";

    private static final String VERSION_QUERY = "select " + RELEASE_VERSION + " from system.local";

    private static final String KEYSPACE = "unearth";
    private static final String USE_KEYSPACE = " USE " + KEYSPACE;

    private final Cluster cluster;

    private static InetAddress inetAddr(String host) {
        try {
            return InetAddress.getByName(host);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid host: " + host, e);
        }
    }
}
