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

package unearth.norest;

import java.util.Optional;
import java.util.function.Function;

public interface Transformer<T> {
    
    static <T> Transformer<T> to(Class<T> type, Function<T, String> fun) {
        return new Transformer<>() {
    
            @Override
            public Class<T> getType() {
                return type;
            }
    
            @Override
            public Optional<T> from(String string) {
                throw new IllegalStateException(this + " does not read: " + string);
            }
    
            @Override
            public Optional<String> to(T t) {
                return Optional.ofNullable(t).map(fun);
            }
    
            @Override
            public String toString() {
                return "Transformer[to " + type + "]";
            }
        };
    }
    
    static <T> Transformer<T> from(Class<T> type, Function<String, T> fun) {
        return from(type, fun, null);
    }
    
    static <T> Transformer<T> from(Class<T> type, Function<String, T> fun, T defaultValue) {
        Optional<T> defaultOptional = Optional.ofNullable(defaultValue);
        return new Transformer<>() {
    
            @Override
            public Class<T> getType() {
                return type;
            }
    
            @Override
            public Optional<T> from(String string) {
                return Optional.ofNullable(string).map(fun)
                    .or(() -> defaultOptional);
            }
    
            @Override
            public Optional<String> to(T t) {
                throw new IllegalStateException(this + " does not write: " + t);
            }
    
            @Override
            public String toString() {
                return "Transformer[from " + type + "]";
            }
        };
    }
    
    Class<T> getType();
    
    Optional<T> from(String string);
    
    Optional<String> to(T t);
}
