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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CodeGenMootricsFactoryTest {

    @SuppressWarnings("UnusedReturnValue")
    public interface Mootrics {

        Counter writes();

        Counter reads(String type);
    }

    @Test
    public void assertSimpleCodeGen() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        CodeGenMetricsFactory metricsFactory = new CodeGenMetricsFactory(meterRegistry)
            .withNamer((metrics, method) -> metrics.getSimpleName() + "." + method.getName());

        Mootrics trix = metricsFactory.instantiate(Mootrics.class);
        for (int i = 0; i < 10; i++) {
            trix.reads("foo").increment();
            trix.writes().increment();
        }
        assertEquals(2, meterRegistry.getMeters().size());

        trix.reads("bar").increment();

        assertEquals(3, meterRegistry.getMeters().size());

        Counter reads = meterRegistry.counter("Mootrics.writes");
        assertNotNull(reads);
        assertEquals(10.0d, reads.count(), 0.1d);
    }
}