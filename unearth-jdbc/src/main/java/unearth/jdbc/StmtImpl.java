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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

class StmtImpl implements Stmt {

    private final PreparedStatement statement;

    private int i;

    StmtImpl(PreparedStatement statement) {
        this.statement = statement;
    }

    @Override
    public Stmt set(String string) {
        try {
            if (string == null) {
                statement.setNull(++i, Types.VARCHAR);
            } else {
                statement.setString(++i, string);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set string #" + i + ": " + string, e);
        }
        return this;
    }

    @Override
    public Stmt set(Boolean bool) {
        try {
            if (bool == null) {
                statement.setNull(++i, Types.BOOLEAN);
            } else {
                statement.setBoolean(++i, bool);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set truth #" + i + ": " + bool, e);
        }
        return this;
    }

    @Override
    public Stmt set(Integer value) {
        try {
            if (value == null) {
                statement.setNull(++i, Types.INTEGER);
            } else {
                statement.setInt(++i, value);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set int #" + i + ": " + i, e);
        }
        return this;
    }

    @Override
    public Stmt set(Long value) {
        try {
            if (value == null) {
                statement.setNull(++i, Types.BIGINT);
            } else {
                statement.setLong(++i, value);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set long #" + i + ": " + value, e);
        }
        return this;
    }

    @Override
    public Stmt set(Instant instant) {
        try {
            if (instant == null) {
                statement.setNull(++i, Types.TIMESTAMP);
            } else {
                statement.setTimestamp(++i, Timestamp.from(instant));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set instant #" + i + ": " + instant, e);
        }
        return this;
    }

    @Override
    public Stmt set(UUID uuid) {
        try {
            if (uuid == null) {
                statement.setNull(++i, Types.OTHER);
            } else {
                statement.setObject(++i, uuid);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not set UUID #" + i + ": " + uuid, e);
        }
        return this;
    }

    @Override
    public void reset() {
        i = 0;
    }

    @Override
    public void close() throws Exception {
        statement.close();
    }
}
