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
import java.util.Objects;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class AbstractGeneratedMetrics extends AbstractMeterRepo {

    static ElementMatcher.Junction<MethodDescription> resolverMethod(Class<? extends Meter> type) {
        return named("resolve" + type.getSimpleName())
            .and(isMethod());
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

    @SuppressWarnings("unused") // Generated code helper
    protected final Gauge resolveGauge(Method method, Object[] args, Supplier<Number> gauged) {
        return resolveMeter(Gauge.class, method, args, spec ->
            newGauge(spec, this.metrics, gauged));
    }

//    @SuppressWarnings("unused") // Generated code helper
//    protected final <T> Gauge resolveGauge(
//        Method method,
//        Object[] args,
//        T obj,
//        ToDoubleFunction<T> meter
//    ) {
//        return resolveMeter(Gauge.class, method, args, spec ->
//            newGauge(spec, this.metrics, obj, meter));
//    }
}
