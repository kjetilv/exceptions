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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class DefaultMultiExistence<T> implements Session.MultiExistence<T> {

    private final DefaultSession session;

    private final Collection<T> items;

    private final String sql;

    private final Session.Set set;

    private final Session.Sel<T> selector;

    @Override
    public Session.Outcome insert(Consumer<Collection<T>> inserter) {
        Collection<T> known = new HashSet<>(session.select(sql, set, selector));
        Collection<T> unknown = items.stream().filter(known::add).collect(Collectors.toSet());
        if (unknown.isEmpty()){
            return Session.Outcome.NOOP;
        }
        inserter.accept(unknown);
        return Session.Outcome.INSERTED;
    }

    DefaultMultiExistence(
        DefaultSession session,
        Collection<T> items,
        String sql,
        Session.Set set,
        Session.Sel<T> selector
    ) {
        this.session = session;
        this.items = items;
        this.sql = sql;
        this.set = set;
        this.selector = selector;
    }
}
