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
import java.sql.Timestamp;
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
            statement.setString(++i, string);
        } catch (Exception e) {
            throw new IllegalStateException("Could not set string #" + i + ": " + string, e);
        }
        return this;
    }

    @Override
    public Stmt set(boolean bool) {
        try {
            statement.setBoolean(++i, bool);
        } catch (Exception e) {
            throw new IllegalStateException("Could not set truth #" + i + ": " + bool, e);
        }
        return this;
    }

    @Override
    public Stmt set(Instant instant) {
        try {
            statement.setTimestamp(++i, Timestamp.from(instant));
        } catch (Exception e) {
            throw new IllegalStateException("Could not set instant #" + i + ": " + instant, e);
        }
        return this;
    }

    @Override
    public Stmt set(long value) {
        try {
            statement.setLong(++i, value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not set long #" + i + ": " + value, e);
        }
        return this;
    }

    @Override
    public Stmt set(int value) {
        try {
            statement.setInt(++i, value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not set int #" + i + ": " + i, e);
        }
        return this;
    }

    @Override
    public Stmt set(UUID uuid) {
        try {
            statement.setObject(++i, uuid);
        } catch (Exception e) {
            throw new IllegalStateException("Could not set UUID #" + i + ": " + uuid, e);
        }
        return this;
    }

    @Override
    public void close() throws Exception {
        statement.close();
    }
}
