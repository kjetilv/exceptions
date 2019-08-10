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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

final class Session implements AutoCloseable {

    private final Connection connection;

    Session(DataSource dataSource) {
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to init session", e);
        }
    }

    <T> Optional<T> selectOne(
        String sql,
        Sel<T> selector
    ) {
        return selectOne(sql, stmt -> {
        }, selector);
    }

    <T> Optional<T> selectOne(
        String sql,
        Set parSet,
        Sel<T> selector
    ) {
        return select(sql, parSet, selector).stream().findFirst();
    }

    <T> List<T> select(
        String sql,
        Set parSet,
        Sel<T> selector
    ) {
        try (
            PreparedStatement ps = connection.prepareCall(sql);
            Stmt stmt = new StmtImpl(ps);
        ) {
            parSet.setParams(stmt);
            ResultSet resultSet = ps.executeQuery();
            Res resGet = new ResImpl(resultSet);
            if (resultSet.next()) {
                List<T> collection = new ArrayList<>();
                do {
                    collection.add(selector.select(resGet));
                } while (resultSet.next());
                return collection;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            throw new IllegalStateException("Call failed: " + sql, e);
        }
    }

    <T> void insert(String sql, Set set) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Stmt prep = new StmtImpl(ps);
            set.setParams(prep);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Statement failed: " + sql, e);
        }
    }

    <T> void insertBatch(String sql, Collection<T> items, BatchParSet<T> set) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Stmt prep = new StmtImpl(ps);
            items.forEach(item -> {
                set.setParams(prep, item);
                addBatch(ps);
            });
            ps.executeBatch();
        } catch (Exception e) {
            throw new IllegalStateException("Statement failed: " + sql, e);
        }
    }

    private void addBatch(PreparedStatement ps) {
        try {
            ps.addBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to batch " + ps, e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close: " + connection, e);
        }
    }

}
