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

package unearth.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class Once<T> implements Supplier<T> {
    
    /**
     * Returns a supplier which runs the given supplier once.
     *
     * @param supplier
     *     Source supplier
     * @param <T>
     *     Type
     *
     * @return Single-run supplier
     */
    public static <T> Supplier<T> get(Supplier<T> supplier) {
        return new Once<>(supplier);
    }
    
    private final AtomicBoolean hasStarted = new AtomicBoolean();
    
    private final AtomicBoolean hasRun = new AtomicBoolean();
    
    private final CompletableFuture<T> future = new CompletableFuture<>();
    
    private final AtomicReference<T> value = new AtomicReference<>();
    
    private final AtomicReference<RuntimeException> error = new AtomicReference<>();
    
    private final Supplier<T> supplier;
    
    private final AtomicLong counter = new AtomicLong();
    
    private Once(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    
    @Override
    public T get() {
        if (hasRun.get()) {
            return resolved();
        }
        if (hasStarted.compareAndSet(false, true)) {
            try {
                T newValue = supplier.get();
                value.set(newValue);
                future.complete(newValue);
                return newValue;
            } catch (RuntimeException e) {
                error.set(e);
                future.completeExceptionally(e);
                throw e;
            } finally {
                hasRun.set(true);
            }
        }
        future.join();
        return resolved();
    }
    
    private T resolved() {
        RuntimeException exception = error.get();
        if (exception == null) {
            return value.get();
        }
        throw exception;
    }
}
