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

package unearth.norest.traffic;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void testRate() {
        AtomicLong time = new AtomicLong();
        RateLimiter<String> limiter = new RateLimiter<>(
            new AtomicClock(time)::instant,
            10,
            100,
            Duration.ofSeconds(1),
            1);
        for (int i = 0; i < 10; i++) {
            assertThat(limiter)
                .describedAs("Call %d should pass", i)
                .accepts("session1");
            time.addAndGet(99L);
        } // 990ms passed, tokens rejected
        assertThat(limiter).rejects("session1");
        time.addAndGet(11L); // Just tipping 1 seconds
        assertThat(limiter).accepts("session1");
        assertThat(limiter).rejects("session1");
        time.addAndGet(1010L); // Another second has passed
        assertThat(limiter).accepts("session1");
    }

    private static final class AtomicClock extends Clock {

        private final AtomicLong time;

        private AtomicClock(AtomicLong time) {
            this.time = time;
        }

        @Override
        public ZoneId getZone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(time.get());
        }
    }
}
