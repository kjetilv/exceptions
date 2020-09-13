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

package unearth.test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.testcontainers.containers.GenericContainer;
import unearth.analysis.CassandraInit;
import unearth.client.UnearthlyClient;
import unearth.http4k.Http4kServer;
import unearth.metrics.ByteBuddyMetricsFactory;
import unearth.metrics.MetricsFactory;
import unearth.server.State;
import unearth.server.Unearth;
import unearth.server.UnearthlyCassandraConfig;
import unearth.server.UnearthlyConfig;
import unearth.server.UnearthlyDbConfig;
import unearth.server.UnearthlyRenderer;

@SuppressWarnings({ "FieldCanBeLocal", "WeakerAccess", "SameParameterValue" })
public final class DefaultDockerStartup implements DockerStartup {

    private final AtomicReference<State> state = new AtomicReference<>();

    private final AtomicReference<UnearthlyClient> client = new AtomicReference<>();

    private final AtomicReference<GenericContainer<?>> cassandraContainer = new AtomicReference<>();

    private final AtomicReference<GenericContainer<?>> postgresContainer = new AtomicReference<>();

    private final AtomicReference<CassandraInit> cassandraInit = new AtomicReference<>();

    DefaultDockerStartup() {
        CompletableFuture<UnearthlyCassandraConfig> cassandraFuture = CompletableFuture
            .supplyAsync(DefaultDockerStartup::startCassandra)
            .whenComplete((genericContainer, e) -> cassandraContainer.set(genericContainer))
            .thenApply(container -> cassandraConfig(container, "testing"));

        CompletableFuture<UnearthlyDbConfig> postgresFuture = CompletableFuture
            .supplyAsync(DefaultDockerStartup::startPostgres)
            .whenComplete((genericContainer, e) -> postgresContainer.set(genericContainer))
            .thenApply(container -> postgresConfig(container, "postgres"));

        cassandraFuture.thenApply(cassandraConfig ->
            new CassandraInit(
                cassandraConfig.getHost(),
                cassandraConfig.getPort(),
                cassandraConfig.getDc(),
                cassandraConfig.getKeyspace()))
            .whenComplete((init, throwable) ->
                cassandraInit.set(init))
            .thenApply(CassandraInit::init);

        CompletableFuture<Unearth> unearthFuture = cassandraFuture
            .thenCombineAsync(postgresFuture, (cassandraConfig, dbConfig) ->
                new UnearthlyConfig(
                    "/api/test",
                    "localhost",
                    0,
                    Duration.ofSeconds(30),
                    true,
                    true,
                    true,
                    cassandraConfig,
                    dbConfig))
            .thenApply(Unearth::new);

        MetricsFactory metricsFactory = ByteBuddyMetricsFactory.DEFAULT;

        CompletableFuture<UnearthlyClient> client = unearthFuture
            .thenApply(unearth ->
                unearth.startJavaServer(
                    metricsFactory,
                    (unearthlyController, unearthlyConfig) ->
                        new Http4kServer(
                            unearthlyController,
                            unearthlyConfig,
                            new UnearthlyRenderer(unearthlyConfig.getPrefix()))))
            .whenComplete((state, throwable) ->
                this.state.set(state))
            .thenApply(State::url)
            .thenApply(UnearthlyClient::connect)
            .whenComplete((unearthlyClient, throwable) ->
                this.client.set(unearthlyClient));

        client.join();
    }

    @Override
    public UnearthlyClient getClient() {
        return client.get();
    }

    @Override
    public void stop() {
        State state = this.state.get();
        if (state != null) {
            state.close();
        }
        CassandraInit init = this.cassandraInit.get();
        if (init != null) {
            init.close();
        }
        GenericContainer<?> container = cassandraContainer.get();
        if (container != null) {
            container.stop();
            container.close();
        }
    }

    @Override
    public void reset() {
        State state = this.state.get();
        if (state != null) {
            state.reset();
        }
    }

    private static final String CASSANDRA_IMAGE = "cassandra:3.11.4";

    private static final String POSTGRES_IMAGE = "postgres:12";

    private static final String DATACENTER = "datacenter1";

    private static GenericContainer<?> startCassandra() {

        GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE).withExposedPorts(9042);
        cassandra.start();
        return cassandra;
    }

    private static GenericContainer<?> startPostgres() {

        GenericContainer<?> postgres = new GenericContainer<>(POSTGRES_IMAGE)
            .withEnv("POSTGRES_PASSWORD", "password")
            .withExposedPorts(5432);
        postgres.start();
        return postgres;
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
            "password",
            postgresPort,
            schema,
            postgresJdbc);
    }
}
