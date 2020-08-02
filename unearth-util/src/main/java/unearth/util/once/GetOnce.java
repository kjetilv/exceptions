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

package unearth.util.once;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class GetOnce<T> extends AbstractGet<T> {
    
    private final AtomicBoolean started = new AtomicBoolean();
    
    private final CompletableFuture<Boolean> success = new CompletableFuture<>();
    
    private final AtomicReference<T> value = new AtomicReference<>();
    
    private final AtomicReference<RuntimeException> error = new AtomicReference<>();
    
    GetOnce(Supplier<T> supplier) {
        super(supplier);
    }
    
    @Override
    protected T doGet(Supplier<T> supplier, boolean optional) {
        if (optional) {
            if (!success.isDone()) {
                return null;
            }
        } else if (isInitial()) {
            return initial(supplier);
        }
        return futureValue();
    }
    
    private T initial(Supplier<T> supplier) {
        T val;
        try {
            val = supplier.get();
        } catch (RuntimeException e) {
            return failed(e);
        }
        return succeded(val);
    }
    
    private T futureValue() {
        if (success.join()) {
            return value.get();
        }
        throw new IllegalStateException(this + ": failed (already)", error.get());
    }
    
    private T succeded(T val) {
        value.set(val);
        success.complete(true);
        return val;
    }
    
    private <X> X failed(RuntimeException e) {
        error.set(e);
        success.complete(false);
        throw new IllegalStateException(this + ": failed", e);
    }
    
    private boolean isInitial() {
        return started.compareAndSet(false, true);
    }
}