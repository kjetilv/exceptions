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

package unearth.jdbc;

import java.time.Instant;
import java.util.UUID;

import unearth.hashable.Hashed;

@SuppressWarnings("UnusedReturnValue")
public interface Stmt extends AutoCloseable {

    default Stmt set(Hashed hashed) {
        return set(hashed.getHash());
    }

    Stmt set(String string);

    Stmt set(Boolean bool);

    Stmt set(Integer i);

    Stmt set(Long l);

    Stmt set(Instant instant);

    Stmt set(UUID uuid);

    void reset();
}
