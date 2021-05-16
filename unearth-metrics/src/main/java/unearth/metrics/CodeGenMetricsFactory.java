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
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import unearth.util.once.Apply;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

public final class CodeGenMetricsFactory implements MetricsFactory {

    private final Function<Class<?>, Object> memoizedInstantiator = Apply.memoized(this::newInstance);

    private final MeterRegistry registry;

    private final MeterNamer namer;

    public CodeGenMetricsFactory(MeterRegistry registry) {
        this(registry, null);
    }

    protected CodeGenMetricsFactory(MeterRegistry registry, MeterNamer namer) {
        this.registry = registry;
        this.namer = namer;
    }

    @Override
    public CodeGenMetricsFactory withNamer(MeterNamer meterNamer) {
        return new CodeGenMetricsFactory(
            registry,
            Objects.requireNonNull(meterNamer, "metricNamer"));
    }

    @Override
    public <T> T instantiate(Class<T> metrics) {
        validate(metrics);
        return metrics.cast(memoizedInstantiator.apply(metrics));
    }

    private <T> void validate(Class<T> metrics) {
        Method[] meters = metrics.getMethods();
        if (Arrays.stream(meters).map(Method::getName).distinct().count() < meters.length) {
            throw new IllegalArgumentException("Metrics interface should not have overloaded method names");
        }
    }

    private Object newInstance(Class<?> metricsInterface) {
        try {
            Class<?> generatedClass = generate(metricsInterface);
            Constructor<?> constructor =
                getConstructor(generatedClass, MeterRegistry.class, MeterNamer.class, Class.class);
            return construct(metricsInterface, generatedClass, constructor);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to implement " + metricsInterface, e);
        }
    }

    private Constructor<?> getConstructor(Class<?> generatedClass, Class<?>... parameters) {
        try {
            return generatedClass.getConstructor(parameters);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to retrieve constructor of " + generatedClass + ": " + Arrays.toString(parameters), e);
        }
    }

    private Object construct(Class<?> metricsInterface, Class<?> generatedClass, Constructor<?> constructor) {
        try {
            return constructor.newInstance(registry, namer, metricsInterface);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instaniate " + generatedClass, e);
        }
    }

    private Class<?> generate(Class<?> metricsInterface) {
        try {
            DynamicType.Builder<AbstractGeneratedMetrics> baseBuilder =
                new ByteBuddy()
                    .subclass(AbstractGeneratedMetrics.class)
                    .implement(metricsInterface);
            DynamicType.Builder<AbstractGeneratedMetrics> methodEnrichedBuilder =
                Arrays.stream(metricsInterface.getMethods())
                    .reduce(baseBuilder, this::withMethod, noCombine());
            return methodEnrichedBuilder.make()
                .load(Thread.currentThread().getContextClassLoader())
                .getLoaded();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate class for " + metricsInterface, e);
        }
    }

    private DynamicType.Builder<AbstractGeneratedMetrics> withMethod(
        DynamicType.Builder<AbstractGeneratedMetrics> builder,
        Method method
    ) {
        return builder
            .method(named(method.getName())
                .and(returns(method.getReturnType()))
                .and(isPublic()))
            .intercept(helperMethodCall(method)
                .with(method)
                .withArgumentArray());
    }

    private static final Map<Class<? extends Meter>, MethodCall> HELPERS =
        Stream.of(
            Meter.class,
            Timer.class,
            Counter.class,
            DistributionSummary.class,
            FunctionCounter.class,
            FunctionTimer.class,
            LongTaskTimer.class,
            Gauge.class
        ).collect(Collectors.toMap(
            Function.identity(),
            type ->
                MethodCall.invoke(
                    named(AbstractGeneratedMetrics.resolverMethodName(type)).and(isMethod()))));

    private static <B> BinaryOperator<B> noCombine() {
        return (b1, b2) -> {
            throw new IllegalStateException("No combine " + b1 + "/" + b2);
        };
    }

    private static MethodCall helperMethodCall(Method m) {
        return Optional.ofNullable(HELPERS.get(m.getReturnType()))
            .orElseThrow(() ->
                new IllegalStateException("Unmapped return type: " + m.getReturnType()));
    }

    private static boolean matches(ParameterDescription target) {
        return target.getType().getTypeName().equals(
            Supplier.class.getName() + "<" + Number.class.getName() + ">");
    }
}
