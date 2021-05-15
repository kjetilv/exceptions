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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.testcontainers.containers.GenericContainer;
import unearth.analysis.CassandraInit;
import unearth.api.UnearthlyApi;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.client.UnearthlyClient;
import unearth.metrics.MetricsFactory;
import unearth.netty.UnearthlyNettyServer;
import unearth.norest.IO;
import unearth.norest.IOHandler;
import unearth.norest.Transformer;
import unearth.norest.common.JacksonIOHandler;
import unearth.norest.common.StringIOHandler;
import unearth.norest.netty.NettyApi;
import unearth.norest.netty.NettyRunner;
import unearth.norest.server.ApiInvoker;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.State;
import unearth.server.Unearth;
import unearth.server.UnearthlyCassandraConfig;
import unearth.server.UnearthlyConfig;
import unearth.server.UnearthlyDbConfig;
import unearth.server.UnearthlyRenderer;

import static unearth.norest.IO.ContentType.APPLICATION_JSON;
import static unearth.norest.IO.ContentType.TEXT_PLAIN;

@SuppressWarnings({ "FieldCanBeLocal", "WeakerAccess", "SameParameterValue" })
public final class DefaultDockerStartup implements DockerStartup {

    private final AtomicReference<State> state = new AtomicReference<>();

    private final AtomicReference<UnearthlyClient> client = new AtomicReference<>();

    private final AtomicReference<GenericContainer<?>> cassandraContainer = new AtomicReference<>();

    private final AtomicReference<GenericContainer<?>> postgresContainer = new AtomicReference<>();

    private final AtomicReference<CassandraInit> cassandraInit = new AtomicReference<>();

    public DefaultDockerStartup() {
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

        CollectorRegistry registry = new CollectorRegistry(true);
        MetricsFactory metricsFactory = MetricsFactory.DEFAULT;
        Supplier<byte[]> metricsOut = () -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8)) {
                TextFormat.write004(writer, registry.metricFamilySamples());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return byteArrayOutputStream.toByteArray();
        };

        CompletableFuture<UnearthlyClient> client = unearthFuture
            .thenApply(unearth ->
                unearth.startJavaServer(
                    metricsFactory,
                    (resources, configuration) -> {

                        Map<IO.ContentType, IOHandler> handlers = handlers();
                        Collection<Transformer<?>> transformers = transformers();
                        NettyApi apiRouter = new NettyApi(
                            configuration.getPrefix(),
                            new ApiInvoker<>(
                                UnearthlyApi.class,
                                new DefaultUnearthlyApi(
                                    resources, new
                                    UnearthlyRenderer(configuration.getPrefix())),
                                handlers,
                                transformers));

                        NettyRunner nettyServer = new NettyRunner(
                            configuration.getPort(),
                            apiRouter,
                            metricsFactory,
                            metricsOut,
                            Clock.systemDefaultZone());

                        return new UnearthlyNettyServer(configuration, nettyServer);
                    }))
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

    private static Map<IO.ContentType, IOHandler> handlers() {
        return Map.of(
            APPLICATION_JSON, JacksonIOHandler.withDefaults(new ObjectMapper()),
            TEXT_PLAIN, new StringIOHandler(StandardCharsets.UTF_8));
    }

    private static List<Transformer<?>> transformers() {
        return List.of(
            Transformer.from(FaultIdDto.class, FaultIdDto::new),
            Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
            Transformer.from(CauseIdDto.class, CauseIdDto::new),
            Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
            Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new));
    }

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
