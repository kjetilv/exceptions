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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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

public class AbstractGeneratedMetrics extends AbstractMeterRepo {

    static String resolverMethodName(Class<? extends Meter> type) {
        return "resolve" + type.getSimpleName();
    }

    private final Class<?> metrics;

    protected AbstractGeneratedMetrics(
        MeterRegistry meterRegistry,
        MetricsFactory.MeterNamer namer,
        Class<?> metrics
    ) {
        super(meterRegistry, namer);
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @SuppressWarnings("unused") // Generated code helper
    protected final Counter resolveCounter(Method method, Object[] args) {
        return resolveMeter(Counter.class, method, args, spec ->
            newCounter(spec, this.metrics));
    }

    @SuppressWarnings("unused") // Generated code helper
    protected final Timer resolveTimer(Method method, Object[] args) {
        return resolveMeter(Timer.class, method, args, spec ->
            newTimer(spec, this.metrics));
    }

    @SuppressWarnings("unused") // Generated code helper
    protected final LongTaskTimer resolveLongTaskTimer(Method method, Object[] args) {
        return resolveMeter(LongTaskTimer.class, method, args, spec ->
            newLongTaskTimer(spec, this.metrics));
    }

    @SuppressWarnings("unused") // Generated code helper
    protected final DistributionSummary resolveDistributionSummary(Method method, Object[] args) {
        return resolveMeter(DistributionSummary.class, method, args, spec ->
            newDistributionSummary(spec, this.metrics));
    }

    @SuppressWarnings({ "unused" }) // Generated code helper
    protected final Gauge resolveGauge(Method method, Object[] args) {
        if (args.length > 0 && args[0] instanceof Supplier<?>) {
            return resolveMeter(Gauge.class, method, skip(1, args), spec ->
                newGauge(spec, this.metrics, (Supplier<?>) args[0]));
        }
        if (args.length > 1 && args[1] instanceof ToDoubleFunction<?>) {
            return resolveMeter(Gauge.class, method, skip(2, args), spec ->
                newGauge(spec, this.metrics, args[0], (ToDoubleFunction<?>) args[1]));
        }
        throw new IllegalStateException(
            "Invalid " + Gauge.class.getSimpleName() + " specification in " + method +
            ", must comply with " + Gauge.Builder.class);
    }

    @SuppressWarnings({ "unused" }) // Generated code helper
    protected final FunctionCounter resolveFunctionCounter(Method method, Object[] args) {
        if (args.length > 1 && args[1] instanceof ToDoubleFunction<?>) {
            return resolveMeter(FunctionCounter.class, method, skip(2, args), spec ->
                newFunctionCounter(spec, this.metrics, args[0], (ToDoubleFunction<?>) args[1]));
        }
        throw new IllegalStateException(
            "Invalid " + FunctionCounter.class.getSimpleName() + " specification: " + method +
            ", must comply with " + FunctionCounter.Builder.class);
    }

    @SuppressWarnings({ "unused" }) // Generated code helper
    protected final FunctionTimer resolveFunctionTimer(Method method, Object[] args) {
        if (args.length > 2) {
            if (args.length > 3 && args[3] instanceof TimeUnit) {
                return resolveMeter(FunctionTimer.class, method, skip(4, args), spec ->
                    newFunctionTimer(
                        spec,
                        metrics,
                        args[0],
                        (ToLongFunction<?>) args[1],
                        (ToDoubleFunction<?>) args[2],
                        (TimeUnit) args[3]));
            }
            return resolveMeter(FunctionTimer.class, method, skip(3, args), spec ->
                newFunctionTimer(
                    spec,
                    metrics,
                    args[0],
                    (ToLongFunction<?>) args[1],
                    (ToDoubleFunction<?>) args[2]));
        }
        throw new IllegalStateException(
            "Invalid " + FunctionTimer.class.getSimpleName() + " specification: " + method +
            ", must comply with " + FunctionTimer.Builder.class);
    }

    private Object[] skip(int offset, Object[] args) {
        return Arrays.stream(args).skip(offset).toArray(Object[]::new);
    }
}
