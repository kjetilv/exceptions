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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MetricsFactoryTest {

    @SuppressWarnings("UnusedReturnValue")
    public interface NopeTriX {

        Counter writer();

        Counter writer(String tag);
    }

    @SuppressWarnings("UnusedReturnValue")
    public interface MehTriX {

        Counter writes();

        Counter reads(String type);

        Timer testTimer();

        Gauge gauge(Supplier<Number> supplier, String type);

        <T> Gauge gaugeFun(T t, ToDoubleFunction<T> fun);

        <T> FunctionTimer funTimer(T t, ToLongFunction<T> count, ToDoubleFunction<T> time);

        <T> FunctionTimer funTimerX(T t, ToLongFunction<T> count, ToDoubleFunction<T> time, TimeUnit timeUnit);

        <T> FunctionCounter funCounter(T t, ToDoubleFunction<T> count);
    }

    private MeterRegistry meterRegistry;

    private MetricsFactory metricsFactory;

    private MehTriX triX;

    @Before
    public void setup() {
        meterRegistry = new SimpleMeterRegistry();
        metricsFactory = new CodeGenMetricsFactory(meterRegistry)
            .withNamer((metrics, method) -> "test-" + method.getName());

        triX = metricsFactory.instantiate(MehTriX.class);
    }

    @After
    public void teardow() {
        meterRegistry = null;
        metricsFactory = null;

        triX = null;
    }

    @Test
    public void naming() {
        try {
            fail(metricsFactory.instantiate(NopeTriX.class) + " cannot be!");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("overload"));
        }
    }

    @Test
    public void gauges() {
        AtomicInteger atomicInteger = new AtomicInteger();
        Supplier<Number> numberSupplier = atomicInteger::getAndIncrement;
        Gauge gauge = triX.gauge(numberSupplier, "foo");
        Gauge gauge2 = triX.gaugeFun(atomicInteger, AtomicInteger::incrementAndGet);

        assertEquals(0.0d, gauge.value(), 0.01d);
        assertEquals(1.0d, gauge.value(), 0.01d);
        assertEquals(2.0d, gauge.value(), 0.01d);
        assertEquals(4.0d, gauge2.value(), 0.01d);
        assertEquals(4.0d, gauge.value(), 0.01d);
        assertEquals(5.0d, gauge.value(), 0.01d);
        assertEquals(7.0d, gauge2.value(), 0.01d);

        Gauge foo = meterRegistry.find("test-gauge").tag("type", "foo").gauge();
        assertNotNull(foo);
        assertEquals(7.0d, foo.value(), 0.01d);
        assertEquals(8.0d, foo.value(), 0.01d);
    }

    @Test
    public void counters() {

        for (int i = 0; i < 10; i++) {
            triX.writes().increment();
        }
        assertEquals(1, meterRegistry.getMeters().size());

        triX.reads("bar").increment();

        assertEquals(2, meterRegistry.getMeters().size());

        Counter writes = meterRegistry.counter("test-writes");
        assertNotNull(writes);
        assertEquals(10.0d, writes.count(), 0.1d);

        Counter barReads = meterRegistry.find("test-reads").tag("type", "bar").counter();
        assertNotNull(barReads);
        assertEquals(1.0d, barReads.count(), 0.1d);
    }

    @Test
    public void tags() {
        for (int i = 0; i < 10; i++) {
            triX.reads("foo").increment();
        }
        assertEquals(1, meterRegistry.getMeters().size());

        Counter fooReads = meterRegistry.find("test-reads").tag("type", "foo").counter();
        assertNotNull(fooReads);
        assertEquals(10.0d, fooReads.count(), 0.1d);
        assertSame(triX.reads("foo"), fooReads);
    }

    @Test
    public void timers() throws InterruptedException {
        Instant time = Instant.now();
        Thread.sleep(1);
        triX.testTimer().record(Duration.between(time, Instant.now()));

        Timer timer = meterRegistry.find("test-testTimer").timer();
        assertNotNull(timer);
        assertTrue(timer.totalTime(TimeUnit.MICROSECONDS) > 0);
    }

    @Test
    public void functionCounters() {
        AtomicInteger i = new AtomicInteger();
        FunctionCounter counter = triX.funCounter(i, AtomicInteger::incrementAndGet);
        assertEquals(1.0d, counter.count(), 0.01d);
    }

    @Test
    public void functionTimers() {
        AtomicInteger i = new AtomicInteger();
        FunctionTimer counter =
            triX.funTimer(i, AtomicInteger::incrementAndGet, AtomicInteger::doubleValue);

        assertEquals(1.0d, counter.count(), 0.01d);
        assertEquals(1.0d, counter.totalTime(TimeUnit.MILLISECONDS), 0.01d);
    }

    @Test
    public void timeUnitFunctionTimers() {
        AtomicInteger i = new AtomicInteger();
        FunctionTimer counter =
            triX.funTimerX(i, AtomicInteger::incrementAndGet, AtomicInteger::doubleValue, TimeUnit.SECONDS);

        assertEquals(1.0d, counter.count(), 0.01d);
        assertEquals(1000.0d, counter.totalTime(TimeUnit.MILLISECONDS), 0.01d);
    }
}