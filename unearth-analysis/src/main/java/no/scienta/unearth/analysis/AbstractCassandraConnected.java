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

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.api.core.metadata.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AbstractCassandraConnected {

    private final CqlSession cqlSession;

    AbstractCassandraConnected(String host, int port, String dc, String keyspace) {
        this.cqlSession = builder(host, port, dc, keyspace).build();

        inSession(session -> {
            Row row = session.execute(VERSION_QUERY).one();
            if (row == null) {
                throw new IllegalStateException("Failed to obtain version information");
            }
            log.info("Connected to Cassandra @ {}: {}",
                endPoints(session).map(EndPoint::asMetricPrefix).collect(Collectors.joining(", ")),
                row.getString(RELEASE_VERSION));
        });
    }

    private CqlSessionBuilder builder(String host, int port, String dc, String keyspace) {
        CqlSessionBuilder cqlSessionBuilder = CqlSession.builder()
            .addContactPoint(InetSocketAddress.createUnresolved(host, port))
            .withLocalDatacenter(dc);
        if (keyspace == null) {
            return cqlSessionBuilder;
        }
        return cqlSessionBuilder
            .withKeyspace(CqlIdentifier.fromCql("\"" + keyspace + "\""));
    }

    void exec(CqlSession session, String stmt, Object... args) {
        PreparedStatement prepared = session.prepare(stmt);
        session.execute(prepared.bind(args));
    }

    <T> T inSession(Function<CqlSession, T> action) {
        try {
            return action.apply(cqlSession);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    void inSession(Consumer<CqlSession> action) {
        try {
            action.accept(cqlSession);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    Stream<EndPoint> endPoints(CqlSession session) {
        return session.getMetadata().getNodes().values().stream().map(Node::getEndPoint);
    }

    public void close() {
        cqlSession.close();
    }

    private static final Logger log = LoggerFactory.getLogger(AbstractCassandraConnected.class);

    private static final String RELEASE_VERSION = "release_version";

    private static final String VERSION_QUERY = "select " + RELEASE_VERSION + " from system.local";
}
