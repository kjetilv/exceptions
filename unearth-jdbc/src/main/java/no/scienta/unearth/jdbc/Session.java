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

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface Session extends AutoCloseable {

    default <T> Optional<T> selectOne(String sql, Sel<T> selector) {
        return selectOne(sql, null, selector);
    }

    <T> Optional<T> selectOne(String sql, Set parSet, Sel<T> selector);

    default <T> Optional<T> selectMaybeOne(String sql, Sel<Optional<T>> selector) {
        return selectMaybeOne(sql, null, selector);
    }

    <T> Optional<T> selectMaybeOne(String sql, Set parSet, Sel<Optional<T>> selector);

    default <T> List<T> select(String sql, Sel<T> selector) {
        return select(sql, null, selector);
    }

    <T> List<T> select(String sql, Set parSet, Sel<T> sel);

    default<T> List<T> selectOpt(String sql, Sel<Optional<T>> sel) {
        return selectOpt(sql, null, sel);
    }

    <T> List<T> selectOpt(String sql, Set set, Sel<Optional<T>> sel);

    default void setParams(Stmt stmt, Set set) {
        if (set != null) {
            set.set(stmt);
        }
    }

    <T> Existence<T> exists(String sql, Set set, Sel<T> selector);

    <T> MultiExistence<T> exists(String sql, Collection<T> items, Set set, Sel<T> selector);

    void update(String sql, Set set);

    <T> void updateBatch(String sql, Collection<T> items, BatchSet<T> set);

    @Override
    void close();

    <T> T withStatement(String sql, Action<T> action);

    enum Outcome {
        INSERTED, UPDATED, INSERTED_AND_UPDATED, NOOP
    }

    @FunctionalInterface
    interface Action<T> {

        T act(PreparedStatement ps, Stmt stmt) throws Exception;
    }

    @FunctionalInterface
    interface Sel<T> {

        T select(Res res);
    }

    @FunctionalInterface
    interface Set {

        Stmt set(Stmt stmt);
    }

    @FunctionalInterface
    interface BatchSet<T> {

        Stmt setParams(Stmt stmt, T item);
    }

    interface Existence<T> {

        Existence<T> onUpdate(Consumer<T> update);

        Existence<T> onInsert(Runnable insert);

        Outcome go();
    }

    interface MultiExistence<T> {

        MultiExistence<T> onUpdate(Consumer<Collection<T>> inserter);

        MultiExistence<T> onInsert(Consumer<Collection<T>> inserter);

        Outcome go();
    }
}
