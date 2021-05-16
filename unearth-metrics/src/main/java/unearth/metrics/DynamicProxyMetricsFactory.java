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

package unearth.metrics;

import java.lang.reflect.Proxy;
import java.util.Objects;

import io.micrometer.core.instrument.MeterRegistry;

public final class DynamicProxyMetricsFactory implements MetricsFactory {

    private final MeterRegistry registry;

    private final MeterNamer namer;

    public DynamicProxyMetricsFactory(MeterRegistry registry) {
        this(registry, null);
    }

    private DynamicProxyMetricsFactory(MeterRegistry registry, MeterNamer namer) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.namer = namer;
    }

    @Override
    public DynamicProxyMetricsFactory withNamer(MeterNamer meterNamer) {
        return new DynamicProxyMetricsFactory(registry, Objects.requireNonNull(meterNamer, "metricNamer"));
    }

    @Override
    public <T> T instantiate(Class<T> metrics) {
        return metrics.cast(Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] { metrics },
            new MeterResolvingInvocationHandler<>(registry, namer, metrics)));
    }
}

