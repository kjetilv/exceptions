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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("SameParameterValue")
final class DefaultSession implements Session {

    private final Connection connection;

    DefaultSession(DataSource dataSource, String schema) {
        try {
            connection = dataSource.getConnection();
            connection.setSchema(schema);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to init session", e);
        }
    }

    @Override
    public <T> Existence<T> exists(String sql, Set set, Sel<T> sel) {
        return new DefaultExistence<>(this, sql, set, sel);
    }

    @Override
    public <T> MultiExistence<T> exists(String sql, Collection<T> items, Set set, Sel<T> selector) {
        return new DefaultMultiExistence<>(this, items, sql, set, selector);
    }

    @Override
    public <T> List<T> select(String sql, Set set, Sel<T> sel) {
        return withStatement(sql, (ps, stmt) -> {
            if (set != null) {
                set.set(stmt);
            }
            ResultSet resultSet = ps.executeQuery();
            if (!resultSet.next()) {
                return Collections.emptyList();
            }
            Res res = new ResImpl(resultSet);
            List<T> selected = new ArrayList<>();
            do {
                selected.add(sel.select(res));
            } while (res.next());
            return selected.isEmpty() ? Collections.emptyList() : selected;
// NO GO
            //          Res res = new ResImpl(ps.executeQuery());
//            return res.get(sel).collect(Collectors.toList());
        });
    }

    @Override
    public <T> T withStatement(String sql, Action<T> action) {
        try (PreparedStatement ps = connection.prepareCall(sql)) {
            return action.on(ps, new StmtImpl(ps));
        } catch (Exception e) {
            throw new IllegalStateException("Call failed: " + sql, e);
        }

    }

    @Override
    public <T> void updateBatch(String sql, Collection<T> items, BatchSet<T> set) {
        withStatement(sql, (ps, stmt) ->
            statementWithItems(ps, stmt, set, items).executeBatch());
    }

    @Override
    public void update(String sql, Set set) {
        withStatement(sql, (ps, stmt) -> {
            set.set(stmt);
            ps.executeUpdate();
            return null;
        });
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close: " + connection, e);
        }
    }

    private <T> PreparedStatement statementWithItems(
        PreparedStatement ps,
        Stmt stmt,
        BatchSet<T> set,
        Collection<T> items
    ) {
        items.forEach(item -> {
            try {
                set.setParams(stmt, item);
            } finally {
                stmt.reset();
            }
            addBatch(ps);
        });
        return ps;
    }

    private void addBatch(PreparedStatement ps) {
        try {
            ps.addBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to batch " + ps, e);
        }
    }
}
