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

package unearth.analysis;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.datastax.oss.driver.api.core.AsyncAutoCloseable;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatementBuilder;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import com.datastax.oss.driver.api.core.metadata.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unearth.util.once.Get;

public class AbstractCassandraConnected {

    private static final Logger log = LoggerFactory.getLogger(AbstractCassandraConnected.class);

    static void exec(CqlSession session, String stmt, Object... args) {
        session.execute(new SimpleStatementBuilder(stmt)
            .setExecutionProfileName(PROFILE)
            .addPositionalValues(args)
            .build());
    }

    private final Supplier<CqlSession> cqlSession;

    AbstractCassandraConnected(String host, int port, String dc, String keyspace) {
        cqlSession = Get.once(() -> {
            CqlSession cqlSession = builder(host, port, dc, keyspace).build();
            Row row = cqlSession.execute(VERSION_QUERY).one();
            if (row == null) {
                throw new IllegalStateException("Failed to obtain version information");
            }
            log.info(
                "Connected to Cassandra @ {}: {}",
                endPoints(cqlSession).map(EndPoint::asMetricPrefix).collect(Collectors.joining(", ")),
                row.getString(RELEASE_VERSION));
            return cqlSession;
        });
    }

    public void close() {
        Get.maybeOnce(cqlSession).get().ifPresent(AsyncAutoCloseable::close);
    }

    void inSession(Consumer<CqlSession> action) {
        try {
            action.accept(cqlSession.get());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to perform action", e);
        }
    }

    private static final String RELEASE_VERSION = "release_version";

    private static final String VERSION_QUERY = "select " + RELEASE_VERSION + " from system.local";

    private static final String PROFILE = "default";

    private static Stream<EndPoint> endPoints(CqlSession session) {
        return session.getMetadata().getNodes().values().stream().map(Node::getEndPoint);
    }

    private static CqlSessionBuilder builder(String host, int port, String dc, String keyspace) {
        CqlSessionBuilder cqlSessionBuilder = CqlSession.builder()
            .withConfigLoader(
                DriverConfigLoader.programmaticBuilder()
                    .startProfile(PROFILE)
                    .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
                    .endProfile()
                    .build())
            .addContactPoint(InetSocketAddress.createUnresolved(host, port))
            .withLocalDatacenter(dc);
        if (keyspace == null) {
            return cqlSessionBuilder;
        }
        return cqlSessionBuilder
            .withKeyspace(CqlIdentifier.fromCql("\"" + keyspace + "\""));
    }
}
