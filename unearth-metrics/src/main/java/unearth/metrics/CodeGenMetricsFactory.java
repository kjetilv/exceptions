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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatcher;
import unearth.util.once.Apply;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

class CodeGenMetricsFactory extends AbstractMetricsFactory {

    private final Function<Class<?>, Object> memoizedInstantiator = Apply.memoized(this::newInstance);

    public CodeGenMetricsFactory(MeterRegistry meterRegistry) {
        this(meterRegistry, null);
    }

    private CodeGenMetricsFactory(MeterRegistry meterRegistry, MetricNamer namer) {
        super(meterRegistry, namer);
    }

    @Override
    public CodeGenMetricsFactory withNamer(MetricNamer metricNamer) {
        return new CodeGenMetricsFactory(
            getMeterRegistry(),
            Objects.requireNonNull(metricNamer, "metricNamer"));
    }

    @Override
    public <T> T instantiate(Class<T> metrics) {
        return metrics.cast(memoizedInstantiator.apply(metrics));
    }

    private Object newInstance(Class<?> metricsInterface) {
        Class<?> generatedClass = generate(metricsInterface);
        try {
            return generatedClass.getConstructor(MeterRegistry.class, MetricNamer.class)
                .newInstance(getMeterRegistry(), getMetricNamer());
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to implement " + metricsInterface + ": " + generatedClass, e);
        }
    }

    private Class<?> generate(Class<?> metricsInterface) {
        return methods(metricsInterface).reduce(
            new ByteBuddy()
                .subclass(AbstractMetricsFactory.class)
                .implement(metricsInterface),
            this::addMethod,
            NO_COMBINE
        ).make()
            .load(Thread.currentThread().getContextClassLoader())
            .getLoaded();
    }

    private DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<AbstractMetricsFactory> addMethod(
        DynamicType.Builder<AbstractMetricsFactory> bld,
        Method mth
    ) {
        return bld.method(publicMethod(mth)).intercept(methodCall(mth.getDeclaringClass(), mth));
    }

    private Stream<Method> methods(Class<?> metricsInterface) {
        return Arrays.stream(metricsInterface.getMethods());
    }

    private MethodCall methodCall(Class<?> metricsInterface, Method method) {
        return methodCall(method)
            .with(metricsInterface, method)
            .withArgumentArray();
    }

    private ElementMatcher.Junction<MethodDescription> publicMethod(Method mthd) {
        return named(mthd.getName())
            .and(returns(mthd.getReturnType()))
            .and(isPublic());
    }

    private static final BinaryOperator<DynamicType.Builder<AbstractMetricsFactory>>
        NO_COMBINE =
        (b1, b2) -> {
            throw new IllegalStateException("No combine " + b1 + "/" + b2);
        };

    private static final Map<Class<? extends Meter>, MethodCall> SUPER_CALLS =
        Stream.of(Meter.class, Timer.class, Counter.class, DistributionSummary.class, LongTaskTimer.class)
            .collect(Collectors.toMap(Function.identity(), CodeGenMetricsFactory::getCall));

    private static MethodCall getCall(Class<? extends Meter> type) {
        return MethodCall.invoke(named("get" + type.getSimpleName()).and(isMethod())).onSuper();
    }

    private static MethodCall methodCall(Method m) {
        return SUPER_CALLS.get(m.getReturnType());
    }
}
