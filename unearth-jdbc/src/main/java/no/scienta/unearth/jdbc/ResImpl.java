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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

class ResImpl implements Res {

    private final ResultSet resultSet;

    private int i;

    ResImpl(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public boolean next() {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to start iteration: " + resultSet, e);
        } finally {
            i = 0;
        }
    }

    @Override
    public Boolean getBoolean() {
        try {
            return resultSet.getBoolean(++i);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get truth #" + i + ": " + resultSet, e);
        }
    }

    @Override
    public Integer getInt() {
        try {
            return resultSet.getInt(++i);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get int #" + i + ": " + resultSet, e);
        }
    }

    @Override
    public Long getLong() {
        try {
            return resultSet.getLong(++i);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get long #" + i + ": " + resultSet, e);
        }
    }

    @Override
    public String getString() {
        try {
            return resultSet.getString(++i);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get string #" + i + ": " + resultSet, e);
        }
    }

    @Override
    public UUID getUUID() {
        try {
            return resultSet.getObject(++i, UUID.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get UUID #" + i + ": " + resultSet, e);
        }
    }

    @Override
    public Instant getInstant() {
        try {
            return resultSet.getTimestamp(++i).toInstant();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get timestamp #" + i + ": " + resultSet, e);
        }
    }
}
