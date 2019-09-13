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

public final class DockerStartup implements Closeable {

    private final Unearth.State state;

    private final UnearthlyClient client;

    private final GenericContainer cassandra;

    private final GenericContainer postgres;

    private final CassandraInit init;

    DockerStartup() {
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(2);
        Future<? extends GenericContainer<?>> cassandraFuture = exec.submit(DockerStartup::startCassandra);
        Future<? extends GenericContainer<?>> postgresFuture = exec.submit(DockerStartup::startPostgres);
        exec.shutdown();

        try {
            cassandra = cassandraFuture.get();
            postgres = postgresFuture.get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed", e);
        }

        UnearthlyConfig config = testConfig(cassandra, postgres);

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

    private static GenericContainer<?> startCassandra() {
        GenericContainer<?> cassandra =
            new GenericContainer<>("cassandra:3.11.4").withExposedPorts(9042);
        cassandra.start();
        return cassandra;
    }

    private static GenericContainer<?> startPostgres() {
        GenericContainer<?> postgres = new GenericContainer<>("postgres:12").withExposedPorts(5432);
        postgres.start();
        return postgres;
    }

    private static CassandraInit initCassandra(UnearthlyCassandraConfig cassandra) {
        return new CassandraInit(
            cassandra.getHost(),
            cassandra.getPort(),
            cassandra.getDc(),
            cassandra.getKeyspace()
        ).init();
    }

    private static UnearthlyConfig testConfig(
        GenericContainer<?> cassandraContainer,
        GenericContainer<?> postgresContainer
    ) {
        return new UnearthlyConfig(
            "/api/test",
            "localhost",
            0,
            true,
            true,
            cassandraConfig(cassandraContainer),
            postgresConfig(postgresContainer));
    }

    private static UnearthlyCassandraConfig cassandraConfig(GenericContainer<?> cassandraContainer) {
        return new UnearthlyCassandraConfig(
            cassandraContainer.getContainerIpAddress(),
            cassandraContainer.getFirstMappedPort(),
            "datacenter1",
            "testing");
    }

    private static UnearthlyDbConfig postgresConfig(GenericContainer<?> postgresContainer) {
        String postgresIP = postgresContainer.getContainerIpAddress();
        Integer postgresPort = postgresContainer.getFirstMappedPort();
        String postgresSchema = "postgres";
        String postgresJdbc = "jdbc:postgresql://" + postgresIP + ":" + postgresPort + "/" + postgresSchema;
        return new UnearthlyDbConfig(
            postgresIP,
            "postgres",
            "",
            postgresPort,
            postgresSchema,
            postgresJdbc);
    }
}
