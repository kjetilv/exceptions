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

import no.scienta.unearth.jdbc.Session.Outcome;
import no.scienta.unearth.jdbc.Session.Sel;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static no.scienta.unearth.jdbc.Session.Outcome.*;
import static no.scienta.unearth.jdbc.Session.Set;

class DefaultMultiExistence<T> implements Session.MultiExistence<T> {

    private final Collection<T> items;

    private final Collection<T> existing;

    private Function<Collection<T>, Integer> inserter;

    private Function<Collection<T>, Integer> updater;

    DefaultMultiExistence(
        DefaultSession session,
        Collection<T> items,
        String sql,
        Set set,
        Sel<T> sel
    ) {
        this.items = items;
        this.existing = new HashSet<>(session.select(sql, set, sel));
    }

    @Override
    public Session.MultiExistence<T> onUpdate(Function<Collection<T>, Integer> updater) {
        this.updater = updater;
        return this;
    }

    @Override
    public Session.MultiExistence<T> onInsert(Function<Collection<T>, Integer> inserter) {
        this.inserter = inserter;
        return this;
    }

    @Override
    public Outcome go() {
        int updated = updated();
        int inserted = inserted();
        return inserted > 0 && updated > 0 ? INSERTED_AND_UPDATED :
            inserted > 0 ? INSERTED :
                updated > 0 ? UPDATED :
                    NOOP;
    }

    private int inserted() {
        if (inserter == null) {
            return -1;
        }
        Collection<T> unknown = items.stream().filter(exists().negate()).collect(Collectors.toSet());
        if (unknown.isEmpty()) {
            return -1;
        }
        return inserter.apply(unknown);
    }

    private Predicate<T> exists() {
        return existing::contains;
    }

    private int updated() {
        if (existing.isEmpty() || updater == null) {
            return -1;
        }
        return updater.apply(existing);
    }

}
