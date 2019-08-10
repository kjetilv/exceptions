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
import java.util.UUID;

class ResImpl implements Res {

    private final ResultSet resultSet;

    ResImpl(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @Override
    public Boolean getBoolean(String name) {
        try {
            return resultSet.getBoolean(name);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get truth '" + name + "': " + resultSet, e);
        }
    }

    @Override
    public Boolean getBoolean() {
        try {
            return resultSet.getBoolean(1);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get truth: " + resultSet, e);
        }
    }

    @Override
    public Integer getInt(String name) {
        try {
            return resultSet.getInt(name);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get int '" + name + "': " + resultSet, e);
        }
    }

    @Override
    public Integer getInt() {
        try {
            return resultSet.getInt(1);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get int: " + resultSet, e);
        }
    }

    @Override
    public Long getLong(String name) {
        try {
            return resultSet.getLong(name);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get long '" + name + "': " + resultSet, e);
        }
    }

    @Override
    public Long getLong() {
        try {
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get long: " + resultSet, e);
        }
    }

    @Override
    public String getString(String name) {
        try {
            return resultSet.getString(name);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get string '" + name + "': " + resultSet, e);
        }
    }

    @Override
    public String getString() {
        try {
            return resultSet.getString(1);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get string: " + resultSet, e);
        }
    }

    @Override
    public UUID getUUID(String name) {
        try {
            return resultSet.getObject(name, UUID.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get UUID '" + name + "': " + resultSet, e);
        }
    }

    @Override
    public UUID getUUID() {
        try {
            return resultSet.getObject(1, UUID.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get UUID: " + resultSet, e);
        }
    }
}
