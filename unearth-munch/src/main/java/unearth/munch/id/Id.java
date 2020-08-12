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
package unearth.munch.id;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import unearth.munch.base.Hashed;

public abstract class Id implements Hashed {
    
    private final UUID hash;
    
    private final String type;
    
    private final int hashCode;
    
    Id(UUID hash) {
        this.hash = Objects.requireNonNull(hash, "hash");
        this.hashCode = this.hash.hashCode();
        this.type = NAMES.computeIfAbsent(getClass(), Id::type);
    }
    
    public String getType() {
        return type;
    }
    
    @Override
    public UUID getHash() {
        return hash;
    }
    
    public String getUuid() {
        return getHash().toString();
    }
    
    public String toHashString() {
        return hash.toString();
    }
    
    private static final Map<Class<?>, String> NAMES = new HashMap<>();
    
    private static final String TAIL = "Id";
    
    private static String type(Class<?> cl) {
        String simpleName = cl.getSimpleName();
        int tail = simpleName.lastIndexOf(TAIL);
        String baseName = simpleName.substring(0, tail);
        String lowerCased = baseName.substring(0, 1).toLowerCase() + baseName.substring(1);
        int dashedLength =
            lowerCased.length() + lowerCased.chars().filter(Character::isUpperCase).map(b -> 1).sum();
        char[] source = lowerCased.substring(0, tail).toCharArray();
        char[] dashed = new char[dashedLength];
        for (int i = 0, c = 0; i < source.length; i++, c++) {
            if (Character.isUpperCase(source[i])) {
                dashed[c++] = '-';
                dashed[c] = Character.toLowerCase(source[i]);
            } else {
                dashed[c] = source[i];
            }
        }
        return new String(dashed);
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Id && Objects.equals(hash, ((Id) o).hash);
    }
    
    @Override
    public String toString() {
        return type + ":" + getHash();
    }
}
