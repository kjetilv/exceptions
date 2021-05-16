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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

final class MeterResolvingInvocationHandler<T> extends AbstractMeterRepo implements InvocationHandler {

    private final Class<T> metrics;

    MeterResolvingInvocationHandler(
        MeterRegistry registry,
        MetricsFactory.MeterNamer namer,
        Class<T> metrics
    ) {
        super(registry, namer);
        this.metrics = metrics;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        try {
            return isMeterMethod(metrics, method)
                ? resolveMeter(method, args)
                : method.invoke(proxy, args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get meter " + method.getName(), e);
        }
    }

    private Meter resolveMeter(Method method, Object... args) {
        return resolveMeter(Meter.class, method, args, spec -> {
            Class<? extends Meter> meterType = spec.returnType();
            if (meterType.equals(COUNTER)) {
                return newCounter(spec, metrics);
            }
            if (meterType.equals(TIMER)) {
                return newTimer(spec, metrics);
            }
            if (meterType.equals(LONG_TASK_TIMER)) {
                return newLongTaskTimer(spec, metrics);
            }
            if (meterType.equals(DISTRIBUTION_SUMMARY)) {
                return newDistributionSummary(spec, metrics);
            }
            throw new IllegalStateException("Unsupported method: " + spec.method());
        });
    }

    private static final Class<? extends Meter> COUNTER = Counter.class;

    private static final Class<? extends Meter> TIMER = Timer.class;

    private static final Class<? extends Meter> LONG_TASK_TIMER = LongTaskTimer.class;

    private static final Class<? extends Meter> DISTRIBUTION_SUMMARY = DistributionSummary.class;

    private static <T> boolean isMeterMethod(Class<T> metrics, Method method) {
        return method.getDeclaringClass() == metrics &&
               Meter.class.isAssignableFrom(method.getReturnType());
    }
}
