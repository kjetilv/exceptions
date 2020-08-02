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

import java.util.Objects;

public final class Tuple<T> {
    
    private final T t1;
    
    private final T t2;
    
    public Tuple(T t1, T t2) {
        this.t1 = Objects.requireNonNull(t1, "t1");
        this.t2 = Objects.requireNonNull(t2, "t2");
    }
    
    public T getT2() {
        return t2;
    }
    
    public T getT1() {
        return t1;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(t1, t2);
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Tuple && Objects.equals(t1, ((Tuple<?>) o).t1) &&
            Objects.equals(t2, ((Tuple<?>) o).t2);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + t1 + " /" + t2 + "]";
    }
}
