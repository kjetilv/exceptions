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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.model.FaultEvents;
import no.scienta.unearth.munch.parser.ThrowableParser;
import no.scienta.unearth.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JdbcStorageTest {

    private JdbcStorage unearth;

    @Before
    public void setup() {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl("jdbc:hsqldb:mem:unearth-" + UUID.randomUUID() + ";sql.syntax_pgs=true");
        configuration.setUsername("SA");
        configuration.setPassword("");
        DataSource dataSource = new HikariDataSource(configuration);
        unearth = new JdbcStorage(dataSource, "unearth");
        unearth.initStorage().run();
    }

    @Test
    public void smoke() {
        assertTrue(unearth.limit().isEmpty());
    }

    @Test
    public void emptyLimits() {
        assertTrue(unearth.limit().isEmpty());
        assertTrue(unearth.limit(new FaultId(UUID.randomUUID())).isEmpty());
        assertTrue(unearth.limit(new FaultStrandId(UUID.randomUUID())).isEmpty());
    }

    @Test
    public void storeAndRetrieve() {
        String data = IO.readPath("testdata/exception3.txt");
        assertNotNull(data);
        Throwable parse = ThrowableParser.parse(data);
        FaultEvents store = unearth.store(
            null,
            Fault.create(parse),
            null);
        FaultEvent event = store.getEvent();
        Optional<FaultEvent> faultEvent = unearth.getFaultEvent(event.getId());

        assertThat(faultEvent.isPresent(), is(true));
        assertThat(faultEvent.get().getId(), is(event.getId()));

        assertThat(unearth.getFault(event.getFaultId()).isPresent(), is(true));
        assertThat(unearth.getFaultStrand(event.getFaultStrandId()).isPresent(), is(true));
    }

    @After
    public void teardown() {
        unearth.close();
    }
}
