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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.scienta.unearth.jdbc.Session.Outcome.*;
import static no.scienta.unearth.jdbc.Session.Set;

class DefaultMultiExistence<T> implements Session.MultiExistence<T> {

    private final Collection<T> items;

    private final Collection<T> existing;

    private Consumer<Collection<T>> inserter;

    private Consumer<Collection<T>> updater;

    DefaultMultiExistence(
        DefaultSession session,
        Collection<T> items,
        String sql,
        Set set,
        Sel<T> selector
    ) {
        this.items = items;
        this.existing = new HashSet<>(session.select(sql, set, selector));
    }

    @Override
    public Session.MultiExistence<T> onUpdate(Consumer<Collection<T>> updater) {
        this.updater = updater;
        return this;
    }

    @Override
    public Session.MultiExistence<T> onInsert(Consumer<Collection<T>> inserter) {
        this.inserter = inserter;
        return this;
    }

    @Override
    public Outcome go() {
        boolean updated = updated();
        boolean inserted = inserted();
        return inserted && updated ? INSERTED_AND_UPDATED :
            inserted ? INSERTED :
            updated ? UPDATED :
            NOOP;
    }

    private boolean inserted() {
        if (inserter == null) {
            return false;
        }
        Collection<T> unknown = items.stream().filter(existing::add).collect(Collectors.toSet());
        if (unknown.isEmpty()) {
            return false;
        }
        inserter.accept(unknown);
        return true;
    }

    private boolean updated() {
        if (existing.isEmpty() || updater == null) {
            return false;
        }
        updater.accept(existing);
        return true;
    }

}
