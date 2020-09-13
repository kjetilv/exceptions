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

package unearth.netty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import unearth.api.UnearthlyApi;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandIdDto;
import unearth.api.dto.FaultIdDto;
import unearth.api.dto.FaultStrandIdDto;
import unearth.api.dto.FeedEntryIdDto;
import unearth.metrics.ByteBuddyMetricsFactory;
import unearth.metrics.MetricsFactory;
import unearth.metrics.MicrometerClock;
import unearth.norest.Transformer;
import unearth.norest.common.JacksonIOHandler;
import unearth.norest.netty.NettyApi;
import unearth.norest.netty.NettyRunner;
import unearth.norest.server.ApiInvoker;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.Unearth;
import unearth.server.UnearthlyRenderer;

public final class Main {

    public static void main(String[] args) {

        MicrometerClock micrometerClock = new MicrometerClock(Clock.systemDefaultZone());
        CollectorRegistry registry = new CollectorRegistry(true);
        MetricsFactory metricsFactory =
            new ByteBuddyMetricsFactory(new CompositeMeterRegistry(
                micrometerClock,
                List.of(
                    new JmxMeterRegistry(
                        JmxConfig.DEFAULT,
                        micrometerClock),
                    new PrometheusMeterRegistry(
                        PrometheusConfig.DEFAULT,
                        registry,
                        micrometerClock))));

        Supplier<byte[]> metricsOut = () -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8)) {
                TextFormat.write004(writer, registry.metricFamilySamples());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return byteArrayOutputStream.toByteArray();
        };

        new Unearth().startJavaServer(
            metricsFactory,
            (resources, configuration) -> {

                UnearthlyRenderer renderer = new UnearthlyRenderer(configuration.getPrefix());
                UnearthlyApi api = new DefaultUnearthlyApi(resources, renderer);

                LongAdder threadCount = new LongAdder();
                Executor executor = new ThreadPoolExecutor(
                    16, 128,
                    10, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(10),
                    r -> {
                        try {
                            return new Thread(r, "api-" + threadCount.longValue());
                        } finally {
                            threadCount.increment();
                        }
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy());

                ApiInvoker<UnearthlyApi> invoker =
                    new ApiInvoker<>(UnearthlyApi.class, api, List.of(
                        Transformer.from(FaultIdDto.class, FaultIdDto::new),
                        Transformer.from(FaultStrandIdDto.class, FaultStrandIdDto::new),
                        Transformer.from(CauseIdDto.class, CauseIdDto::new),
                        Transformer.from(CauseStrandIdDto.class, CauseStrandIdDto::new),
                        Transformer.from(FeedEntryIdDto.class, FeedEntryIdDto::new)));

                NettyApi apiRouter = new NettyApi(
                    configuration.getPrefix(),
                    JacksonIOHandler.DEFAULT,
                    executor,
                    invoker::response);
                NettyRunner nettyServer = new NettyRunner(
                    configuration.getPort(),
                    apiRouter,
                    metricsFactory,
                    metricsOut,
                    Clock.systemDefaultZone());

                return new UnearthlyNettyServer(configuration, nettyServer);
            });
    }

    private Main() {
    }
}
