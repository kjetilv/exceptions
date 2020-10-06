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

        private final long tokens;

        private TokenState(Instant time, long tokens) {
            this.time = time;
            this.tokens = tokens;
        }

        public TokenState decremented() {
            return tokens < 0 ? this : new TokenState(time, tokens - 1);
        }

        public TokenState incrementedAt(Instant time, long amount) {
            return new TokenState(
                time,
                Math.min(
                    maxTokens,
                    Math.max(tokens, 0) + amount));
        }

        private long periodsSince(Instant currentTime) {
            return Duration.between(time, currentTime).dividedBy(refillPeriod);
        }

        private boolean isAllowed() {
            return tokens >= 0;
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
            return state.updateAndGet(current -> {
                Instant now = time.get();
                long periods = current.periodsSince(now);
                return (periods > 0
                    ? current.incrementedAt(now, refillsPerPeriod * periods)
                    : current).decremented();
            }).isAllowed();
        }

        @Override
        public String toString() {
            synchronized (lock) {
                return getClass().getSimpleName() + "[" + state + "]";
            }
        }
    }
}
