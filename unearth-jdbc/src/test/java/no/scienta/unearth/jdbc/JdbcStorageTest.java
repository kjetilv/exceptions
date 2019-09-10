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
import no.scienta.unearth.core.FaultFeed;
import no.scienta.unearth.core.FaultStats;
import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FeedEntry;
import no.scienta.unearth.munch.parser.ThrowableParser;
import no.scienta.unearth.util.IO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class JdbcStorageTest {

    private FaultStorage storage;

    private FaultFeed feed;

    private FaultStats stats;

    @Before
    public void setup() {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl("jdbc:hsqldb:mem:unearth-" + UUID.randomUUID() + ";sql.syntax_pgs=true");
        configuration.setUsername("SA");
        configuration.setPassword("");
        DataSource dataSource = new HikariDataSource(configuration);
        storage = storage(dataSource);
        feed = (FaultFeed) storage;
        stats = (FaultStats) storage;
        storage.initStorage().run();
    }

    @Test
    public void smoke() {
        assertTrue(feed.limit().isEmpty());
    }

    @Test
    public void emptyLimits() {
        assertTrue(feed.limit().isEmpty());
        assertTrue(feed.limit(new FaultId(UUID.randomUUID())).isEmpty());
        assertTrue(feed.limit(new FaultStrandId(UUID.randomUUID())).isEmpty());
    }

    @Test
    public void storeAndRetrieve() {
        String data = IO.readPath("testdata/exception3.txt");
        assertNotNull(data);
        Throwable parse = ThrowableParser.parse(data);
        FeedEntry event = storage.store(
            null,
            Fault.create(parse),
            null);
        Optional<FeedEntry> faultEvent = storage.getFeedEntry(event.getId());

        assertThat(faultEvent.isPresent(), is(true));
        assertThat(faultEvent.get().getId(), is(event.getId()));

        assertThat(storage.getFault(event.getFaultEvent().getFaultId()).isPresent(), is(true));
        assertThat(storage.getFaultStrand(event.getFaultEvent().getFaultStrandId()).isPresent(), is(true));

        assertThat(feed.limit().isEmpty(), is(false));
        assertThat(feed.limit().getAsLong(), is(1L));

        List<FeedEntry> feed = this.feed.feed(0, 10);
        assertThat(feed.size(), is(1));
    }

    @After
    public void teardown() {
        storage.close();
    }

    private FaultStorage storage(DataSource dataSource) {
        return new JdbcStorage(dataSource, "unearth");
    }
}
