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

package unearth.norest.common;

import java.util.Optional;

public class DefaultTransformer<T> implements Transformer<T> {
    
    private final Class<T> type;
    
    public DefaultTransformer(Class<T> type) {
        this.type = type;
    }
    
    @Override
    public Class<T> getType() {
        return type;
    }
    
    @Override
    public Optional<T> from(String string) {
        throw new IllegalStateException(this + " does not read " + string);
    }
    
    @Override
    public Optional<String> to(T o) {
        return Optional.of(o).map(String::valueOf);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[type=" + type + "]";
    }
}
