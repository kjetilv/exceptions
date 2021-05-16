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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public abstract class AbstractMeterRepo {

    protected final Map<MeterSpec, Meter> meters = new ConcurrentHashMap<>();

    private final MeterRegistry registry;

    private final MetricsFactory.MeterNamer meterNamer;

    public AbstractMeterRepo(MeterRegistry registry, MetricsFactory.MeterNamer meterNamer) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.meterNamer = meterNamer;
    }

    protected <T extends Meter> T resolveMeter(
        Class<T> type, Method method,
        Object[] args,
        Function<MeterSpec, T> create
    ) {
        return type.cast(meters.computeIfAbsent(
            new MeterSpec(method, args),
            create));
    }

    protected DistributionSummary newDistributionSummary(MeterSpec spec, Class<?> metrics) {
        return DistributionSummary.builder(meterName(spec, metrics))
            .tags(spec.tags())
            .register(registry);
    }

    protected LongTaskTimer newLongTaskTimer(MeterSpec spec, Class<?> metrics) {
        return LongTaskTimer.builder(meterName(spec, metrics))
            .tags(spec.tags())
            .register(registry);
    }

    protected Timer newTimer(MeterSpec spec, Class<?> metrics) {
        return Timer.builder(meterName(spec, metrics))
            .tags(spec.tags())
            .register(registry);
    }

    protected Counter newCounter(MeterSpec spec, Class<?> metrics) {
        return Counter.builder(meterName(spec, metrics))
            .tags(spec.tags())
            .register(registry);
    }

    @SuppressWarnings("unchecked")
    protected Gauge newGauge(
        MeterSpec spec,
        Class<?> metrics,
        Supplier<?> gauged
    ) {
        return Gauge.builder(
            meterName(spec, metrics),
            (Supplier<Number>) Objects.requireNonNull(gauged, "gauged")
        ).tags(spec.tags(1))
            .register(registry);
    }

    @SuppressWarnings("unchecked")
    protected <T> Gauge newGauge(
        MeterSpec spec,
        Class<?> metrics,
        T obj,
        ToDoubleFunction<?> meter
    ) {
        return Gauge.builder(
            meterName(spec, metrics),
            Objects.requireNonNull(obj, "obj"),
            (ToDoubleFunction<T>) Objects.requireNonNull(meter, "meter")
        ).tags(spec.tags(2))
            .register(registry);
    }

    @SuppressWarnings("unchecked")
    protected <T> FunctionTimer newFunctionTimer(
        MeterSpec spec,
        Class<?> metrics,
        T obj,
        ToLongFunction<?> countFun,
        ToDoubleFunction<?> totalTimeFun
    ) {
        return FunctionTimer.builder(
            meterName(spec, metrics),
            Objects.requireNonNull(obj, "obj"),
            (ToLongFunction<T>) Objects.requireNonNull(countFun, "countFun"),
            (ToDoubleFunction<T>) Objects.requireNonNull(totalTimeFun, "totalTimeFun"),
            TimeUnit.MILLISECONDS
        ).tags(spec.tags(3))
            .register(registry);
    }

    @SuppressWarnings("unchecked")
    protected <T> FunctionTimer newFunctionTimer(
        MeterSpec spec,
        Class<?> metrics,
        T obj,
        ToLongFunction<?> countFun,
        ToDoubleFunction<?> totalTimeFun,
        TimeUnit timeUnit
    ) {
        return FunctionTimer.builder(
            meterName(spec, metrics),
            Objects.requireNonNull(obj, "obj"),
            (ToLongFunction<T>) Objects.requireNonNull(countFun, "countFun"),
            (ToDoubleFunction<T>) Objects.requireNonNull(totalTimeFun, "totalTimeFun"),
            Objects.requireNonNull(timeUnit, "timeUnit")
        ).tags(spec.tags(4))
            .register(registry);
    }

    @SuppressWarnings("unchecked")
    protected <T> FunctionCounter newFunctionCounter(
        MeterSpec spec,
        Class<?> metrics,
        T obj,
        ToDoubleFunction<?> fun
    ) {
        return FunctionCounter.builder(
            meterName(spec, metrics),
            Objects.requireNonNull(obj, "obj"),
            (ToDoubleFunction<T>) Objects.requireNonNull(fun, "fun")
        ).tags(spec.tags(2))
            .register(registry);
    }

    private String meterName(MeterSpec spec, Class<?> metrics) {
        return meterNamer == null
            ? metrics.getSimpleName() + '.' + spec.method().getName()
            : meterNamer.name(metrics, spec.method());
    }
}
