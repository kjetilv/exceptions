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

package no.scienta.unearth.munch.ids;

import java.util.Objects;
import java.util.UUID;

public abstract class Id {

    private final UUID hash;

    Id(UUID hash) {
        this.hash = Objects.requireNonNull(hash);
    }

    public UUID getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Id && Objects.equals(hash, ((Id) o).hash);
    }

    @Override
    public int hashCode() {
        return 3 + 31 * hash.hashCode();
    }

    @Override
    public String toString() {
        String s = getHash().toString();
        int endIndex = s.indexOf("-");
        return "{" + (endIndex < 0 ? s : s.substring(0, endIndex)) + "}";
    }
}
