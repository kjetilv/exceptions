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

import java.util.Objects;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

public class JdbcSetup implements Runnable {

    private final DataSource dataSource;

    private final String schema;

    public JdbcSetup(DataSource dataSource, String schema) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.schema = Objects.requireNonNull(schema, "schema");
        if (schema.isBlank()) {
            throw new IllegalArgumentException("Empty schema ref: '" + schema + "'");
        }
    }

    @Override
    public void run() {
        new Flyway(Flyway.configure(Thread.currentThread().getContextClassLoader())
            .dataSource(dataSource)
            .schemas(schema)
            .baselineOnMigrate(true)
            .validateOnMigrate(true)
            .locations("classpath:db")
        ).migrate();
    }
}
