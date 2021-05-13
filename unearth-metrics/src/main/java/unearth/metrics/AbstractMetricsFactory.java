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
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

@SuppressWarnings("unused")
public abstract class AbstractMetricsFactory implements MetricsFactory {

    private final MeterRegistry meterRegistry;

    private final Map<MeterSpec, Meter> meters = new ConcurrentHashMap<>();

    public AbstractMetricsFactory(
        MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    protected <T> Meter getMeter(Class<T> metrics, Method method, Object... args) {
        return meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newMeter(spec, metrics, meterRegistry));
    }

    protected <T> Counter getCounter(Class<T> metrics, Method method, Object... args) {
        return (Counter) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newCounter(spec, metrics, meterRegistry));
    }

    protected <T> Timer getTimer(Class<T> metrics, Method method, Object... args) {
        return (Timer) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newTimer(spec, metrics, meterRegistry));
    }

    protected <T> LongTaskTimer getLongTaskTimer(Class<T> metrics, Method method, Object... args) {
        return (LongTaskTimer) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newLongTaskTimer(spec, metrics, meterRegistry));
    }

    protected <T> DistributionSummary getDistributionSummary(Class<T> metrics, Method method, Object... args) {
        return (DistributionSummary) meters.computeIfAbsent(
            meterSpec(method, args),
            spec ->
                newDistributionSummary(spec, metrics, meterRegistry));
    }

    private MeterSpec meterSpec(Method method, Object[] args) {
        String name = method.getName();
        String returnType = method.getReturnType().getName();
        String[] parameters = Arrays.stream(method.getParameters())
            .map(Parameter::getName)
            .toArray(String[]::new);
        return new MeterSpec(name, returnType, parameters, args);
    }

    private static final String COUNTER = Counter.class.getName();

    private static final String TIMER = Timer.class.getName();

    private static final String LONG_TASK_TIMER = LongTaskTimer.class.getName();

    private static final String DISTRIBUTION_SUMMARY = DistributionSummary.class.getName();
    private static final String FUNCTION_COUNTER = FunctionCounter.class.getName();

    protected static <T> boolean isMeterMethod(Class<T> metrics, Method method) {
        return method.getDeclaringClass() == metrics &&
               Meter.class.isAssignableFrom(method.getReturnType());
    }

    @SuppressWarnings("DuplicatedCode")
    private static <T> Meter newMeter(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        String meterType = spec.getReturnType();

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
        throw new IllegalStateException("Unsupported method: " + spec.getMethod());
    }

    private static <T> DistributionSummary newDistributionSummary(
        MeterSpec spec,
        Class<T> metrics,
        MeterRegistry meterRegistry
    ) {
        return DistributionSummary.builder(name(spec, metrics))
            .tags(tags(spec))
            .register(meterRegistry);
    }

    private static <T> LongTaskTimer newLongTaskTimer(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        return LongTaskTimer.builder(name(spec, metrics))
            .tags(tags(spec))
            .register(meterRegistry);
    }

    private static <T> Timer newTimer(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        return Timer.builder(name(spec, metrics))
            .tags(tags(spec))
            .register(meterRegistry);
    }

    private static <T> Counter newCounter(MeterSpec spec, Class<T> metrics, MeterRegistry meterRegistry) {
        return Counter.builder(name(spec, metrics))
            .tags(tags(spec))
            .register(meterRegistry);
    }

    private static <T> String name(MeterSpec spec, Class<T> metrics) {
        return name(metrics, spec.getMethod());
    }

    private static Collection<Tag> tags(MeterSpec spec) {
        String[] parameters = spec.getParameters();
        return IntStream.range(0, parameters.length)
            .filter(i ->
                spec.getArg(i) != null)
            .mapToObj(i ->
                Tag.of(parameters[i], string(spec.getArg(i))))
            .collect(Collectors.toSet());
    }

    private static <T> String name(Class<T> metrics, String name) {
        return metrics.getName() + '.' + name;
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

    private static final class MeterSpec {

        private final String method;

        private final String returnType;

        private final String[] parameters;

        private final Object[] args;

        private MeterSpec(
            String method,
            String returnType,
            String[] parameters,
            Object[] args
        ) {
            this.method = Objects.requireNonNull(method, "method");
            this.returnType = Objects.requireNonNull(returnType, "returnType");
            this.parameters = Objects.requireNonNull(parameters, "parameters").clone();
            this.args = Objects.requireNonNull(args, "args").clone();
        }

        private String[] getParameters() {
            return parameters;
        }

        private String getReturnType() {
            return returnType;
        }

        private String getMethod() {
            return method;
        }

        private Object getArg(int i) {
            return args[i];
        }

        @Override
        public int hashCode() {
            return 37 *
                   (31 * Objects.hash(method, returnType) + Arrays.hashCode(parameters)) +
                   Arrays.hashCode(args);
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object o) {
            return method.equals(((MeterSpec) o).method) &&
                   Arrays.equals(args, ((MeterSpec) o).args);
        }
    }
}
