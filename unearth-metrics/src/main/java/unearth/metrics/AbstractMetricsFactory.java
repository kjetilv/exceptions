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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

@SuppressWarnings("unused")
public abstract class AbstractMetricsFactory implements MetricsFactory {

    private final MeterRegistry meterRegistry;

    private final Map<MeterSpec, Meter> meters = new ConcurrentHashMap<>();

    private final MetricNamer metricNamer;

    public AbstractMetricsFactory(MeterRegistry meterRegistry, MetricNamer metricNamer) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.metricNamer = metricNamer == null ? AbstractMetricsFactory::defaultName : metricNamer;
    }

    @SuppressWarnings("unused")
    protected <T> Meter getMeter(Class<T> metrics, Method method, Object... args) {
        return meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newMeter(spec, metrics, getMeterRegistry()));
    }

    @SuppressWarnings("unused")
    protected <T> Counter getCounter(Class<T> metrics, Method method, Object... args) {
        return (Counter) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newCounter(spec, metrics, getMeterRegistry()));
    }

    @SuppressWarnings("unused")
    protected <T> Timer getTimer(Class<T> metrics, Method method, Object... args) {
        return (Timer) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newTimer(spec, metrics, getMeterRegistry()));
    }

    @SuppressWarnings("unused")
    protected <T> LongTaskTimer getLongTaskTimer(Class<T> metrics, Method method, Object... args) {
        return (LongTaskTimer) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newLongTaskTimer(spec, metrics, getMeterRegistry()));
    }

    @SuppressWarnings("unused")
    protected <T> DistributionSummary getDistributionSummary(Class<T> metrics, Method method, Object... args) {
        return (DistributionSummary) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newDistributionSummary(spec, metrics, getMeterRegistry()));
    }

    protected MetricNamer getMetricNamer() {
        return metricNamer;
    }

    protected final MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    private MeterSpec meterSpec(Method method, Object[] args) {
        return new MeterSpec(method, args);
    }

    private static final Class<? extends Meter> COUNTER = Counter.class;

    private static final Class<? extends Meter> TIMER = Timer.class;

    private static final Class<? extends Meter> LONG_TASK_TIMER = LongTaskTimer.class;

    private static final Class<? extends Meter> DISTRIBUTION_SUMMARY = DistributionSummary.class;

    private static final Class<? extends Meter> FUNCTION_COUNTER = FunctionCounter.class;

    private <T> Meter newMeter(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        Class<? extends Meter> meterType = spec.returnType();

        if (meterType.equals(COUNTER)) {
            return newCounter(spec, metrics, meterRegistry);
        }
        if (meterType.equals(TIMER)) {
            return newTimer(spec, metrics, meterRegistry);
        }
        if (meterType.equals(LONG_TASK_TIMER)) {
            return newLongTaskTimer(spec, metrics, meterRegistry);
        }
        if (meterType.equals(DISTRIBUTION_SUMMARY)) {
            return newDistributionSummary(spec, metrics, meterRegistry);
        }
        throw new IllegalStateException("Unsupported method: " + spec.method());
    }

    private <T> DistributionSummary newDistributionSummary(
        MeterSpec spec,
        Class<T> metrics,
        MeterRegistry meterRegistry
    ) {
        return DistributionSummary.builder(metricNamer.name(metrics, spec.method()))
            .tags(spec.tags())
            .register(meterRegistry);
    }

    private <T> LongTaskTimer newLongTaskTimer(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        return LongTaskTimer.builder(metricNamer.name(metrics, spec.method()))
            .tags(spec.tags())
            .register(meterRegistry);
    }

    private <T> Timer newTimer(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        return Timer.builder(metricNamer.name(metrics, spec.method()))
            .tags(spec.tags())
            .register(meterRegistry);
    }

    private <T> Counter newCounter(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        return Counter.builder(metricNamer.name(metrics, spec.method()))
            .tags(spec.tags())
            .register(meterRegistry);
    }

    private static <T> String defaultName(Class<T> metrics, Method method) {
        return metrics.getSimpleName() + '.' + method.getName();
    }

    private record MeterSpec(Method method, Object[] args) {

        private Collection<Tag> tags() {
            String[] parameters = parameters();
            return IntStream.range(0, parameters.length)
                .filter(i -> args[i] != null)
                .mapToObj(i -> Tag.of(parameters[i], string(args[i])))
                .collect(Collectors.toSet());
        }

        private String[] parameters() {
            return Arrays.stream(method.getParameters())
                .map(Parameter::getName)
                .toArray(String[]::new);
        }

        private Class<? extends Meter> returnType() {
            return method.getReturnType().asSubclass(Meter.class);
        }

        private static String string(Object arg) {
            if (arg instanceof Class<?>) {
                return ((Class<?>) arg).getName();
            }
            if (arg instanceof String) {
                return (String) arg;
            }
            return String.valueOf(arg);
        }
    }
}
