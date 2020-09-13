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

import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Session extends AutoCloseable {
    
    enum Outcome {
        
        INSERTED, UPDATED, INSERTED_AND_UPDATED, NOOP
    }
    
    @FunctionalInterface
    interface Action<T> {
        
        T on(PreparedStatement ps, Stmt stmt) throws Exception;
    }
    
    @FunctionalInterface
    interface Sel<T> extends Function<Res, T> {
        
        @Override
        default T apply(Res res) {
            return select(res);
        }
        
        T select(Res res);
    }
    
    @FunctionalInterface
    interface Set {
        
        Stmt set(Stmt stmt);
    }
    
    @FunctionalInterface
    interface BatchSet<T> {
        
        void setParams(Stmt stmt, T item);
    }
    
    interface Existence<T> {
        
        Existence<T> onUpdate(Function<T, Integer> update);
        
        Existence<T> onInsert(Supplier<Integer> insert);
        
        default <R> Optional<R> thenLoad(Function<T, Optional<R>> load) {
            return Optional.ofNullable(thenLoad(load, () -> null));
        }
        
        <R> R thenLoad(Function<T, Optional<R>> load, Supplier<R> orElse);
        
        Outcome go();
    }
    
    interface MultiExistence<T> {
        
        MultiExistence<T> onUpdate(Function<Collection<T>, Integer> inserter);
        
        MultiExistence<T> onInsert(Function<Collection<T>, Integer> inserter);
        
        Outcome go();
    }
    
    interface Res {
        
        <T> Stream<T> get(Sel<T> sel);
        
        String getString();
        
        Boolean getBoolean();
        
        Integer getInt();
        
        Long getLong();
        
        UUID getUUID();
        
        Instant getInstant();
        
        boolean next();
        
        default <T> Stream<T> ifNext(Function<Res, T> apply) {
            return next() ? Optional.ofNullable(apply.apply(this)).stream() : Stream.empty();
        }
        
        default <T> Stream<T> ifNext(Supplier<T> apply) {
            return next() ? Optional.ofNullable(apply.get()).stream() : Stream.empty();
        }
        
        default <T> Optional<T> ifNextOne(Function<Res, T> apply) {
            return next() ? Optional.ofNullable(apply.apply(this)) : Optional.empty();
        }
        
        default <T> Optional<T> ifNextOne(Supplier<T> apply) {
            return next() ? Optional.ofNullable(apply.get()) : Optional.empty();
        }
    }
    
    default <T> Optional<T> selectOne(String sql, Sel<T> selector) {
        return selectOne(sql, null, selector);
    }
    
    default <T> Optional<T> selectOne(String sql, Set parSet, Sel<T> selector) {
        return select(sql, parSet, selector).stream().findFirst();
    }
    
    default <T> List<T> select(String sql, Sel<T> selector) {
        return select(sql, null, selector);
    }
    
    <T> List<T> select(String sql, Set parSet, Sel<T> sel);
    
    <T> Existence<T> exists(String sql, Set set, Sel<T> selector);
    
    <T> MultiExistence<T> exists(String sql, Collection<T> items, Set set, Sel<T> selector);
    
    int effect(String sql, Set set);
    
    <T> int[] effectBatch(String sql, Collection<T> items, BatchSet<T> set);
    
    default <T> int updateBatchTotal(String sql, Collection<T> items, BatchSet<T> set) {
        return IntStream.of(effectBatch(sql, items, set)).sum();
    }
    
    @Override
    void close();
    
    <T> T withStatement(String sql, Action<T> action);
}
