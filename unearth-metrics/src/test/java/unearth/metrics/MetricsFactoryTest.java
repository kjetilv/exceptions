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

import io.micrometer.core.instrument.Counter;
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

public class MetricsFactoryTest {

    @SuppressWarnings("UnusedReturnValue")
    public interface MehTriX {

        Counter writes();

        Counter reads(String type);

        Timer testTimer();

//        Gauge gauge(Supplier<Number> supplier);
    }

    private MeterRegistry meterRegistry;

    @Before
    public void setup() {
        meterRegistry = new SimpleMeterRegistry();
    }

    @After
    public void teardow(){
        meterRegistry = null;
    }

    @Test
    public void assertSimpleCodeGen() throws InterruptedException {
        assertSimpleMetrics(new CodeGenMetricsFactory(meterRegistry));
    }

    @Test
    public void assertSimpleDynamic() throws InterruptedException {
        assertSimpleMetrics(new CodeGenMetricsFactory(meterRegistry));
    }

    private void assertSimpleMetrics(MetricsFactory base) throws InterruptedException {
        MetricsFactory metricsFactory = base
            .withNamer((metrics, method) -> "test-" + method.getName());

        MehTriX triX = metricsFactory.instantiate(MehTriX.class);

        Supplier<Number> numberSupplier = new AtomicInteger()::getAndIncrement;
//        Gauge gauge = triX.gauge(numberSupplier);

        Instant time = Instant.now();
        for (int i = 0; i < 10; i++) {
            triX.reads("foo").increment();
            triX.writes().increment();
        }
        assertEquals(2, meterRegistry.getMeters().size());

        triX.reads("bar").increment();

        assertEquals(3, meterRegistry.getMeters().size());

        Counter writes = meterRegistry.counter("test-writes");
        assertNotNull(writes);
        assertEquals(10.0d, writes.count(), 0.1d);

        Counter fooReads = meterRegistry.find("test-reads").tag("type", "foo").counter();
        assertNotNull(fooReads);
        assertEquals(10.0d, fooReads.count(), 0.1d);
        assertSame(triX.reads("foo"), fooReads);

        Counter barReads = meterRegistry.find("test-reads").tag("type", "bar").counter();
        assertNotNull(barReads);
        assertEquals(1.0d, barReads.count(), 0.1d);
        assertSame(triX.reads("foo"), fooReads);

        Thread.sleep(1);
        triX.testTimer().record(Duration.between(time, Instant.now()));

        Timer timer = meterRegistry.find("test-testTimer").timer();
        assertNotNull(timer);
        assertTrue(timer.totalTime(TimeUnit.MICROSECONDS) > 0);

//        double value = gauge.value();
//        assertEquals(value, 0.0d, 0.01d);
    }
}