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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.persistence;

import no.scienta.unearth.munch.base.Hashable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class Session implements AutoCloseable {

    private final Connection connection;

    Session(DataSource dataSource) {
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to init session", e);
        }
    }

    final <T extends Hashable> void insert(String sql, JdbcStorage.Inserter set) {
        try (
            PreparedStatement ps = connection.prepareStatement(sql)
        ) {
            JdbcStorage.Prep prep = new PrepImpl(ps);
            set.setParams(prep);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Statement failed: " + sql, e);
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
