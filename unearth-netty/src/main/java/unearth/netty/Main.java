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
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import unearth.metrics.CodeGenMetricsFactory;
import unearth.metrics.MetricsFactory;
import unearth.metrics.MicrometerClock;
import unearth.norest.IO;
import unearth.norest.IOHandler;
import unearth.norest.Transformer;
import unearth.norest.common.JacksonIOHandler;
import unearth.norest.common.StringIOHandler;
import unearth.norest.netty.NettyApi;
import unearth.norest.netty.NettyRunner;
import unearth.norest.server.ApiInvoker;
import unearth.server.DefaultUnearthlyApi;
import unearth.server.Unearth;
import unearth.server.UnearthlyRenderer;

import static unearth.norest.IO.ContentType.APPLICATION_JSON;
import static unearth.norest.IO.ContentType.TEXT_PLAIN;

public final class Main {

    public static void main(String[] args) {
        CollectorRegistry registry = new CollectorRegistry(true);
        MetricsFactory metricsFactory = getMetricsFactory(registry);
        Supplier<byte[]> metricsOut = () -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (Writer writer = new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8)) {
                TextFormat.write004(writer, registry.metricFamilySamples());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return byteArrayOutputStream.toByteArray();
        };

        new Unearth().startJavaServer(metricsFactory, (resources, configuration) -> {

            NettyApi apiRouter = new NettyApi(
                configuration.getPrefix(),
                new ApiInvoker<>(
                    UnearthlyApi.class,
                    new DefaultUnearthlyApi(
                        resources, new
                        UnearthlyRenderer(configuration.getPrefix())),
                    handlers(),
                    transformers()));

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

    private static MetricsFactory getMetricsFactory(CollectorRegistry registry) {
        MicrometerClock micrometerClock =
            new MicrometerClock(Clock.systemDefaultZone());
        return new CodeGenMetricsFactory(new CompositeMeterRegistry(
            micrometerClock,
            List.of(
                new JmxMeterRegistry(
                    JmxConfig.DEFAULT,
                    micrometerClock),
                new PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    registry,
                    micrometerClock))));
    }

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
}
