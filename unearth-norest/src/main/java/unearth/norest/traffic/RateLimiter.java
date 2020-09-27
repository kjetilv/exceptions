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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class RateLimiter<T> implements Predicate<T> {

    private final Supplier<Instant> time;

    private final ConcurrentHashMap<T, Session> sessions = new ConcurrentHashMap<>();

    private final int startTokens;

    private final int maxTokens;

    private final Duration refillPeriod;

    private final int refillsPerPeriod;

    public RateLimiter(
        Supplier<Instant> time,
        int startTokens,
        int maxTokens,
        Duration refillPeriod,
        int refillsPerPeriod
    ) {
        this.time = time;
        this.startTokens = startTokens;
        this.maxTokens = maxTokens;
        this.refillPeriod = refillPeriod == null || refillPeriod.isNegative() || refillPeriod.isZero()
            ? Duration.ofSeconds(1)
            : refillPeriod;
        this.refillsPerPeriod = refillsPerPeriod;
    }

    @Override
    public boolean test(T t) {
        return sessions.computeIfAbsent(t, __ -> new Session()).allowed();
    }

    private final class TokenState {

        private final Instant time;

        private final int tokens;

        private TokenState(Instant time, int tokens) {

            this.time = time;
            this.tokens = tokens;
        }

        public TokenState decremented() {
            return tokens == 0 ? this : new TokenState(time, tokens - 1);
        }

        public TokenState incrementedAt(Instant time, int amount) {
            return new TokenState(time, tokens + amount);
        }

        private int periodsSince(Instant now) {
            return Math.toIntExact(Duration.between(time, now).dividedBy(refillPeriod));
        }

        private boolean isAllowed() {
            return tokens > 0;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[@" + time + " : " + tokens + "]";
        }
    }

    private final class Session {

        private final Object lock = new Object();

        private final AtomicReference<TokenState> state =
            new AtomicReference<>(new TokenState(time.get(), startTokens));

        public synchronized boolean allowed() {
            return state.updateAndGet(adjustment()).isAllowed();
        }

        private UnaryOperator<TokenState> adjustment() {
            return current -> adjustedAt(time.get(), current);
        }

        private TokenState adjustedAt(Instant now, TokenState current) {
            int periods = current.periodsSince(now);
            if (periods > 0) {
                return current.incrementedAt(now, 1 + refillsPerPeriod * periods);
            }
            return current.decremented();
        }

        @Override
        public String toString() {
            synchronized (lock) {
                return getClass().getSimpleName() + "[" + state + "]";
            }
        }
    }
}
