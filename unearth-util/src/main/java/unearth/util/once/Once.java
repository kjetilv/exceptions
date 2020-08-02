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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class Once {
    
    /**
     * Returns a supplier which runs the given supplier once.
     *
     * @param supplier Source supplier
     * @param <T> Type
     *
     * @return Single-run supplier
     */
    public static <T> Supplier<T> get(Supplier<T> supplier) {
        return supplier instanceof GetOnce<?> ? supplier : new GetOnce<>(vetted(supplier));
    }
    
    public static <T> Supplier<T> mostly(Supplier<T> supplier) {
        return supplier instanceof GetMostlyOnce<?> ? supplier : new GetMostlyOnce<>(vetted(supplier));
    }
    
    public static <T> Supplier<Optional<T>> maybe(Supplier<T> supplier) {
        return ((AbstractGet<T>) (supplier instanceof GetOnce<?>
            ? supplier
            : mostly(supplier)))
            .maybe();
    }
    
    private Once() {
    }
    
    private static <T> T vetted(T supplier) {
        return Objects.requireNonNull(supplier, "supplier");
    }
}
