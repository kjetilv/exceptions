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

package no.scienta.unearth.munch.id;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public abstract class Id {

    private final UUID hash;

    private String name;

    private final int hashCode;

    private static final Map<Class<?>, String> NAMES = new HashMap<>();

    private static final String TAIL = "Id";

    Id(UUID hash) {
        this.hash = Objects.requireNonNull(hash, "hash");
        this.hashCode = this.hash.hashCode();
        this.name = NAMES.computeIfAbsent(getClass(), cl -> {
            String simpleName = cl.getSimpleName();
            String lowerCased = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
            int tail = lowerCased.lastIndexOf(TAIL);
            return lowerCased.substring(0, tail).intern();
        });
    }

    public UUID getHash() {
        return hash;
    }

    public String toHashString() {
        return hash.toString();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Id && Objects.equals(hash, ((Id) o).hash);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return name + ":" + getHash();
    }
}
