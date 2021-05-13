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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import unearth.util.once.Apply;

import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

public class CodeGenMetricsFactory extends AbstractMetricsFactory {

    public static final CodeGenMetricsFactory DEFAULT = new CodeGenMetricsFactory(
        new SimpleMeterRegistry(SimpleConfig.DEFAULT, Clock.SYSTEM));

    private final MeterRegistry meterRegistry;

    private final Function<Class<?>, Object> instantiator;

    public CodeGenMetricsFactory(MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.instantiator = Apply.memoized(metrics ->
            inst(metrics, this.meterRegistry));
    }

    @Override
    public <T> T instantiate(Class<T> metrics) {
        return metrics.cast(instantiator.apply(metrics));
    }

    private static final Map<String, MethodCall> SUPER_CALLS =
        Stream.of("Timer", "Counter", "Gauge", "DistributionSummary", "FunctionCounter", "LongTaskTimer")
            .collect(Collectors.toMap(
                Function.identity(),
                name ->
                    MethodCall.invoke(getMethod("get" + name)).onSuper()
            ));

    private static Method getMethod(String method) {
        try {
            return AbstractMetricsFactory.class.getDeclaredMethod(
                method, Class.class, Method.class, Object.class.arrayType());
        } catch (Exception e) {
            throw new IllegalStateException("Could not reflect upon own method: " + method, e);
        }
    }

    private static <T> T inst(Class<T> metrics, MeterRegistry meterRegistry) {
        DynamicType.Builder<AbstractMetricsFactory> implement = new ByteBuddy()
            .subclass(AbstractMetricsFactory.class)
            .implement(metrics);
        DynamicType.Builder<AbstractMetricsFactory> withMethods = withMethods(metrics, implement);
        Class<?> generatedClass = load(withMethods);
        return createMetrics(metrics, meterRegistry, generatedClass);
    }

    private static <T> T createMetrics(Class<T> metrics, MeterRegistry meterRegistry, Class<?> generatedClass) {
        try {
            Constructor<?> constructor = generatedClass.getConstructor(MeterRegistry.class);
            return metrics.cast(constructor.newInstance(meterRegistry));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to buddy up " + metrics, e);
        }
    }

    private static <T> DynamicType.Builder<T> withMethods(Class<?> metrics, DynamicType.Builder<T> builder) {
        return Arrays.stream(metrics.getMethods()).reduce(
            builder,
            (b, m) ->
                b.method(named(m.getName())
                    .and(returns(m.getReturnType()))
                    .and(isPublic()))
                    .intercept(methodCall(m)
                        .with(metrics, m)
                        .withArgumentArray()),
            (b1, b2) -> {
                throw new IllegalStateException("No combine " + b1 + "/" + b2);
            });
    }

    private static Class<?> load(DynamicType.Builder<?> builder) {
        return builder
            .make()
            .load(Thread.currentThread().getContextClassLoader())
            .getLoaded();
    }

    private static MethodCall methodCall(Method m) {
        return SUPER_CALLS.get(m.getReturnType().getSimpleName());
    }
}
