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

package no.scienta.unearth.jdbc;

import java.util.Optional;
import java.util.function.Consumer;

class DefaultExistence<T> implements Session.Existence<T> {

    private DefaultSession session;

    private final String sql;

    private final Session.Set set;

    private final Session.Sel<T> sel;

    private Consumer<T> update;

    private Runnable insert;

    DefaultExistence(DefaultSession session, String sql, Session.Set set, Session.Sel<T> sel) {
        this.session = session;
        this.sql = sql;
        this.set = set;
        this.sel = sel;
    }

    @Override
    public Session.Existence<T> onUpdate(Consumer<T> update) {
        this.update = update;
        return this;
    }

    @Override
    public Session.Existence<T> onInsert(Runnable insert) {
        this.insert = insert;
        return this;
    }

    @Override
    public Session.Outcome go() {
        Optional<T> existing = session.select(sql, set, sel).stream().findFirst();
        existing.ifPresentOrElse(
            update == null ? noUpdate() : update,
            insert == null ? noInsert() : insert
        );
        return existing.isPresent() ? Session.Outcome.UPDATED : Session.Outcome.INSERTED;
    }

    private Runnable noInsert() {
        return () -> {
        };
    }

    private Consumer<T> noUpdate() {
        return t -> {
        };
    }
}
