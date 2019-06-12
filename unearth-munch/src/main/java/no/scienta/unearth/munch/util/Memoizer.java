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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.munch.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class Memoizer {

    /**
     * Returns a supplier which runs the given supplier once only.
     *
     * @param supplier Source supplier
     * @param <O> Type
     * @return Single-run supplier
     */
    public static <O> Supplier<O> get(Supplier<O> supplier) {
        return supplier == null || supplier instanceof SuppMemoizer<?> ? supplier : new SuppMemoizer<>(supplier);
    }

    private static final class SuppMemoizer<T> implements Supplier<T> {

        private final Supplier<T> supplier;

        private final AtomicReference<T> value = new AtomicReference<>();

        private SuppMemoizer(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public T get() {
            return value.updateAndGet(v -> v == null ? supplier.get() : v);
        }
    }

    private Memoizer() {
    }
}
