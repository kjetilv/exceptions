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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

class PrepImpl implements JdbcStorage.Prep {

    private final PreparedStatement statement;

    private int i;

    PrepImpl(PreparedStatement statement) {
        this.statement = statement;
    }

    @Override
    public JdbcStorage.Prep set(String string) {
        try {
            statement.setString(++i, string);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set #" + i + ": " + string, e);
        }
        return this;
    }

    @Override
    public JdbcStorage.Prep set(boolean bool) {
        try {
            statement.setBoolean(++i, bool);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set #" + i + ": " + bool, e);
        }
        return this;
    }

    @Override
    public JdbcStorage.Prep set(int i) {
        try {
            statement.setInt(++i, i);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set #" + i + ": " + i, e);
        }
        return this;
    }

    @Override
    public JdbcStorage.Prep set(UUID uuid) {
        try {
            statement.setObject(++i, uuid);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not set #" + i + ": " + uuid, e);
        }
        return this;
    }

    @Override
    public void close() throws Exception {
        statement.close();
    }
}
