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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

public class DynamicProxyMetricsFactory extends AbstractMetricsFactory {

    public DynamicProxyMetricsFactory(MeterRegistry meterRegistry) {
        this(meterRegistry, null);
    }

    private DynamicProxyMetricsFactory(MeterRegistry meterRegistry, MetricNamer namer) {
        super(meterRegistry, namer);
    }

    @Override
    public MetricsFactory withNamer(MetricNamer metricNamer) {
        return new DynamicProxyMetricsFactory(
            getMeterRegistry(),
            Objects.requireNonNull(metricNamer, "metricNamer"));
    }

    @Override
    public <T> T instantiate(Class<T> metrics) {
        return metrics.cast(Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] { metrics },
            new MeterResolvingInvocationHandler<>(metrics)));
    }

    private class MeterResolvingInvocationHandler<T> implements InvocationHandler {

        private final Class<T> metrics;

        protected MeterResolvingInvocationHandler(Class<T> metrics) {
            this.metrics = metrics;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            try {
                return isMeterMethod(metrics, method)
                    ? DynamicProxyMetricsFactory.this.getMeter(metrics, method, args)
                    : method.invoke(proxy, args);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to get meter " + method.getName(), e);
            }
        }

        protected static <T> boolean isMeterMethod(Class<T> metrics, Method method) {
            return method.getDeclaringClass() == metrics &&
                   Meter.class.isAssignableFrom(method.getReturnType());
        }
    }
}
