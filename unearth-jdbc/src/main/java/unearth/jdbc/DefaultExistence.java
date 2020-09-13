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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

class DefaultExistence<T> implements Session.Existence<T> {
    
    private final Session session;
    
    private final String sql;
    
    private final Session.Set set;
    
    private final Session.Sel<T> sel;
    
    private Function<T, Integer> update;
    
    private Supplier<Integer> insert;
    
    DefaultExistence(Session session, String sql, Session.Set set, Session.Sel<T> sel) {
        this.session = session;
        this.sql = sql;
        this.set = set;
        this.sel = sel;
    }
    
    @Override
    public Session.Existence<T> onUpdate(Function<T, Integer> update) {
        this.update = update;
        return this;
    }
    
    @Override
    public Session.Existence<T> onInsert(Supplier<Integer> insert) {
        this.insert = insert;
        return this;
    }
    
    @Override
    public <R> R thenLoad(Function<T, Optional<R>> load, Supplier<R> orElse) {
        return existing().flatMap(load).orElseGet(orElse);
    }
    
    @Override
    public Session.Outcome go() {
        Optional<T> existing = existing();
        if (existing.isPresent()) {
            if (update == null) {
                return Session.Outcome.NOOP;
            }
            return existing.map(update)
                .filter(i -> i > 0)
                .map(i -> Session.Outcome.UPDATED)
                .orElse(Session.Outcome.NOOP);
        }
        if (insert == null) {
            return Session.Outcome.NOOP;
        }
        Integer inserted = insert.get();
        return inserted != null && inserted > 0 ? Session.Outcome.INSERTED : Session.Outcome.NOOP;
    }
    
    private Optional<T> existing() {
        return session.select(sql, set, sel).stream().findFirst();
    }
}
