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

package no.scienta.unearth.test;

import no.scienta.unearth.analysis.CassandraInit;
import no.scienta.unearth.client.UnearthlyClient;
import no.scienta.unearth.server.Unearth;
import no.scienta.unearth.server.UnearthlyCassandraConfig;
import no.scienta.unearth.server.UnearthlyConfig;
import no.scienta.unearth.server.UnearthlyDbConfig;
import org.testcontainers.containers.GenericContainer;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess", "SameParameterValue"})
public final class DockerStartup implements Closeable {

    private final Unearth.State state;

    private final UnearthlyClient client;

    private final GenericContainer cassandra;

    private final GenericContainer postgres;

    private final CassandraInit init;

    private static final String CASSANDRA_IMAGE = "cassandra:3.11.4";

    private static final String POSTGRES_IMAGE = "postgres:12";

    private static final String DATACENTER = "datacenter1";

    DockerStartup() {
        AtomicInteger threadCounter = new AtomicInteger();
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(
            2,
            r ->
                new Thread(r, "t" + threadCounter.incrementAndGet()));

        Future<? extends GenericContainer<?>> cassandraFuture = exec.submit(DockerStartup::startCassandra);
        Future<? extends GenericContainer<?>> postgresFuture = exec.submit(DockerStartup::startPostgres);

        cassandra = waitFor(cassandraFuture, "Failed: Cassandra");
        postgres = waitFor(postgresFuture, "Failed: Postgres");

        UnearthlyConfig config = new UnearthlyConfig(
            "/api/test",
            "localhost",
            0,
            true,
            true,
            cassandraConfig(cassandra, "testing"),
            postgresConfig(postgres, "postgres"));

        init = initCassandra(config.getCassandra());
        state = new Unearth(config).invoke();
        client = UnearthlyClient.connect(state.url());
    }

    public UnearthlyClient getClient() {
        return client;
    }

    public void stop() {
        if (state != null) {
            state.close();
        }
        if (init != null) {
            init.close();
        }
        if (cassandra != null) {
            cassandra.stop();
            cassandra.close();
        }
    }

    public void reset() {
        if (state != null) {
            state.reset();
        }
    }

    @Override
    public void close() {
        stop();
    }

    private static GenericContainer<?> waitFor(Future<? extends GenericContainer<?>> future, String msg) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException(msg, e);
        }
    }

    private static GenericContainer<?> startCassandra() {
        GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE).withExposedPorts(9042);
        cassandra.start();
        return cassandra;
    }

    private static GenericContainer<?> startPostgres() {
        GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE).withExposedPorts(5432);
        postgres.start();
        return postgres;
    }

    private static CassandraInit initCassandra(UnearthlyCassandraConfig cassandraConfig) {
        return new CassandraInit(
            cassandraConfig.getHost(),
            cassandraConfig.getPort(),
            cassandraConfig.getDc(),
            cassandraConfig.getKeyspace()
        ).init();
    }

    private static UnearthlyCassandraConfig cassandraConfig(GenericContainer<?> container, String keyspace) {
        return new UnearthlyCassandraConfig(
            container.getContainerIpAddress(),
            container.getFirstMappedPort(),
            DATACENTER,
            keyspace);
    }

    private static UnearthlyDbConfig postgresConfig(GenericContainer<?> container, String schema) {
        String postgresIP = container.getContainerIpAddress();
        Integer postgresPort = container.getFirstMappedPort();
        String postgresJdbc = "jdbc:postgresql://" + postgresIP + ":" + postgresPort + "/" + schema;
        return new UnearthlyDbConfig(
            postgresIP,
            "postgres",
            "",
            postgresPort,
            schema,
            postgresJdbc);
    }
}
