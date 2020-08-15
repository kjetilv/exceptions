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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class MetricsFactory {
    
    public static final MetricsFactory DEFAULT = new MetricsFactory(
        new SimpleMeterRegistry(SimpleConfig.DEFAULT, Clock.SYSTEM));
    
    private final MeterRegistry meterRegistry;
    
    private final Map<Spec, Meter> meters = new ConcurrentHashMap<>();
    
    public MetricsFactory(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }
    
    public <T> T instantiate(Class<T> metrics) {
        return metrics.cast(Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {
                metrics
            },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() != metrics) {
                    return method.invoke(proxy, args);
                }
                if (Meter.class.isAssignableFrom(method.getReturnType())) {
                    return newMeter(metrics, method, args);
                }
                return method.invoke(proxy, args);
            }));
    }
    
    private <T> Meter newMeter(Class<T> metrics, Method method, Object[] args) {
        return meters.computeIfAbsent(
            new Spec(method, args),
            spec ->
                newMeter(spec, metrics, meterRegistry));
    }
    
    private static <T> Meter newMeter(Spec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        Class<?> meterType = spec.getMethod().getReturnType();
        Parameter[] parameters = spec.getMethod().getParameters();
        String name = name(spec, metrics);
        Collection<Tag> tags = tags(spec, parameters);
        
        if (meterType == Counter.class) {
            return Counter.builder(name).tags(tags).register(meterRegistry);
        }
        
        if (meterType == Timer.class) {
            return Timer.builder(name).tags(tags).register(meterRegistry);
        }
        
        if (meterType == LongTaskTimer.class) {
            return LongTaskTimer.builder(name).tags(tags).register(meterRegistry);
        }
        
        if (meterType == DistributionSummary.class) {
            return DistributionSummary.builder(name).tags(tags).register(meterRegistry);
        }
        
        if (meterType == Gauge.class) {
            //noinspection ConstantConditions
            return Gauge.builder(name, null, null).tags(tags).register(meterRegistry);
        }
        
        if (meterType == FunctionCounter.class) {
            //noinspection ConstantConditions
            return FunctionCounter.builder(name, null, null).register(meterRegistry);
        }
        
        throw new IllegalStateException("Unsupported method: " + spec.method);
    }
    
    private static <T> String name(Spec spec, Class<T> metrics) {
        return metrics.getName() + '.' + spec.getMethod().getName();
    }
    
    private static Collection<Tag> tags(Spec spec, Parameter[] parameters) {
        return IntStream.range(0, spec.getMethod().getParameterCount())
            .filter(i -> spec.args[i] != null)
            .mapToObj(i ->
                Tag.of(parameters[i].getName(), String.valueOf(spec.getArg(i))))
            .collect(Collectors.toSet());
    }
    
    private static final class Spec {
        
        private final Method method;
        
        private final Object[] args;
        
        private Spec(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
        
        private Method getMethod() {
            return method;
        }
        
        private Object getArg(int i) {
            return args[i];
        }
        
        @Override
        public int hashCode() {
            return 31 * Objects.hash(method) + Arrays.hashCode(args);
        }
        
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object o) {
            return method.equals(((Spec) o).method) &&
                Arrays.equals(args, ((Spec) o).args);
        }
    }
}
