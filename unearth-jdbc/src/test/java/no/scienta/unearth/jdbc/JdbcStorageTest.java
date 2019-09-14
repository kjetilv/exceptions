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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(feed.limit()).isEmpty();
    }

    @Test
    public void emptyLimits() {
        assertThat(feed.limit()).isEmpty();
        assertThat(feed.limit(new FaultId(UUID.randomUUID()))).isEmpty();
        assertThat(feed.limit(new FaultStrandId(UUID.randomUUID()))).isEmpty();
    }

    @Test
    public void storeTwiceAndRetrieve() {
        Fault fault = fault("testdata/exception3.txt");
        FeedEntry event1 = storage.store(null, fault, null);
        assertThat(event1.getGlobalSequenceNo()).isEqualTo(1L);
        assertThat(event1.getFaultSequenceNo()).isEqualTo(1L);
        assertThat(event1.getFaultSequenceNo()).isEqualTo(1L);

        FeedEntry event2 = storage.store(null, fault, null);
        assertThat(event2.getGlobalSequenceNo()).isEqualTo(2L);
        assertThat(event2.getFaultSequenceNo()).isEqualTo(2L);
        assertThat(event2.getFaultStrandSequenceNo()).isEqualTo(2L);

        List<FeedEntry> feed = this.feed.feed(0, 10);
        assertThat(feed.size()).isEqualTo(2);

        assertThat(storage.getFault(fault.getId())).hasValue(fault);
    }

    @Test
    public void storeVariantAndRetrieve() {
        Fault fault1 = fault("testdata/exception3.txt");
        Fault fault2 = fault("testdata/exception3a.txt");

        FeedEntry event1 = storage.store(null, fault1, null);
        assertThat(event1.getGlobalSequenceNo()).isEqualTo(1L);
        assertThat(event1.getFaultSequenceNo()).isEqualTo(1L);
        assertThat(event1.getFaultSequenceNo()).isEqualTo(1L);

        FeedEntry event2 = storage.store(null, fault2, null);
        assertThat(event2.getGlobalSequenceNo()).isEqualTo(2L);
        assertThat(event2.getFaultSequenceNo()).isEqualTo(1L);
        assertThat(event2.getFaultStrandSequenceNo()).isEqualTo(2L);

        List<FeedEntry> feed = this.feed.feed(0, 10);
        assertThat(feed.size()).isEqualTo(2);

        assertThat(storage.getFault(fault1.getId())).hasValue(fault1);
        assertThat(storage.getFault(fault2.getId())).hasValue(fault2);
    }

    @Test
    public void storeAndRetrieve() {
        Fault fault = fault("testdata/exception3.txt");
        FeedEntry event = storage.store(null, fault, null);

        assertThat(storage.getFeedEntry(event.getId())).hasValueSatisfying(feedEntry ->
            assertThat(feedEntry.getId()).isEqualTo(event.getId()));

        assertThat(storage.getFault(event.getFaultEvent().getFaultId())).isPresent();
        assertThat(storage.getFaultStrand(event.getFaultEvent().getFaultStrandId())).isPresent();

        assertThat(feed.limit()).hasValue(1L);

        List<FeedEntry> feed = this.feed.feed(0, 10);
        assertThat(feed.size()).isEqualTo(1);

        assertThat(storage.getFault(fault.getId())).hasValue(fault);
    }

    @After
    public void teardown() {
        storage.close();
    }

    private static Fault fault(String reference) {
        String data = IO.readPath(reference);
        Throwable parse = ThrowableParser.parse(data);
        return Fault.create(parse);
    }

    private static FaultStorage storage(DataSource dataSource) {
        return new JdbcStorage(dataSource, "unearth");
    }
}
