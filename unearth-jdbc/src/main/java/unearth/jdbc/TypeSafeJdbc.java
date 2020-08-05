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
import java.util.function.Supplier;

import unearth.munch.base.Hashable;
import unearth.munch.base.Hashed;
import unearth.munch.id.Id;
import unearth.util.StringlyTyped;

@SuppressWarnings({"unused", "WeakerAccess"})
class TypeSafeJdbc<T extends Hashed> {

    private final Stmt stmt;

    TypeSafeJdbc(Stmt stmt, T hashable) {
        this.stmt = stmt;
        if (hashable != null) {
            this.stmt.set(hashable);
        }
    }

    protected Stmt s(Hashable hashable) {
        return stmt.set(hashable);
    }

    protected Stmt s(Id id) {
        return stmt.set(id);
    }

    protected Stmt s(Instant instant) {
        return stmt.set(instant);
    }

    protected Stmt s(UUID uuid) {
        return stmt.set(uuid);
    }

    protected Stmt s(String string) {
        return stmt.set(string);
    }

    protected Stmt s(StringlyTyped typed) {
        return s(typed.string());
    }

    protected Stmt s(Integer val) {
        return stmt.set(val);
    }

    protected Stmt s(Long val) {
        return stmt.set(val);
    }

    protected Stmt s(Boolean bool) {
        return stmt.set(bool);
    }

    protected Stmt noop() {
        return stmt;
    }

    protected static <M> M set(Supplier<Stmt> action, Supplier<M> returner) {
        action.get();
        return returner.get();
    }
}
