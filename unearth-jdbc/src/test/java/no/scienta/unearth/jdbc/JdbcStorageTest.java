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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcStorageTest {

    private FaultStorage storage;

    private FaultFeed feed;

    private FaultStats stats;

    private AtomicLong atomicClock;

    @Before
    public void setup() {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl("jdbc:hsqldb:mem:unearth-" + UUID.randomUUID() + ";sql.syntax_pgs=true");
        configuration.setUsername("SA");
        configuration.setPassword("");
        DataSource dataSource = new HikariDataSource(configuration);

        Clock clock = newAtomicClock();
        JdbcStorage jdbcStorage = storage(dataSource, clock);
        storage = jdbcStorage;
        feed = jdbcStorage;
        stats = jdbcStorage;
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
        assertStored(fault);

        FeedEntry event2 = storage.store(null, fault, null);
        assertThat(event2.getGlobalSequenceNo()).isEqualTo(2L);
        assertThat(event2.getFaultSequenceNo()).isEqualTo(2L);
        assertThat(event2.getFaultStrandSequenceNo()).isEqualTo(2L);
        assertStored(fault);

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
        assertStored(fault1);

        FeedEntry event2 = storage.store(null, fault2, null);
        assertThat(event2.getGlobalSequenceNo()).isEqualTo(2L);
        assertThat(event2.getFaultSequenceNo()).isEqualTo(1L);
        assertThat(event2.getFaultStrandSequenceNo()).isEqualTo(2L);
        assertStored(fault2);

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

        List<FeedEntry> feedEntries = this.feed.feed(0, 10);
        assertThat(feedEntries.size()).isEqualTo(1);

        assertStored(fault);

        assertThat(this.feed.limit()).hasValue(1L);
        assertThat(this.feed.limit(fault.getId())).hasValue(1L);
        assertThat(this.feed.limit(fault.getFaultStrand().getId())).hasValue(1L);
    }

    @Test
    public void storeAndRetrieveFeed() {
        Fault fault1 = fault("testdata/exception3.txt");
        Fault fault2 = fault("testdata/exception3a.txt");

        for (int i = 0; i < 100; i++) {
            atomicClock.getAndAdd(Duration.ofDays(1).toMillis());
            storage.store(null, fault1, null);
        }
        for (int i = 0; i < 100; i++) {
            atomicClock.getAndAdd(Duration.ofDays(1).toMillis());
            storage.store(null, fault2, null);
        }

        assertThat(this.feed.feed(fault1.getId(), 10, 10)).hasSize(10);
        assertThat(this.feed.feed(fault1.getFaultStrand().getId(), 10, 10)).hasSize(10);

        assertThat(this.stats.getFeed(fault1)).hasSize(100);
        assertThat(this.stats.getFeed(fault2)).hasSize(100);

        assertThat(fault1.getFaultStrand()).isEqualTo(fault2.getFaultStrand());
        assertThat(this.stats.getFeed(fault1.getFaultStrand())).hasSize(200);

        assertThat(this.stats.getFeed()).hasSize(200);
    }

    @After
    public void teardown() {
        storage.close();
    }

    private Clock newAtomicClock() {
        atomicClock = new AtomicLong();
        return new Clock() {

            @Override
            public ZoneId getZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException(String.valueOf(zone));
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(atomicClock.getAndIncrement());
            }
        };
    }

    private void assertStored(Fault fault) {
        assertThat(storage.getFault(fault.getId())).hasValue(fault);
        assertThat(storage.getFaultStrand(fault.getFaultStrand().getId())).hasValue(fault.getFaultStrand());
        fault.getFaultStrand().getCauseStrands().forEach(causeStrand ->
            assertThat(storage.getCauseStrand(causeStrand.getId())).hasValue(causeStrand));
        fault.getCauses().forEach(cause ->
            assertThat(storage.getCause(cause.getId())).hasValue(cause));
    }

    private static Fault fault(String reference) {
        String data = IO.readPath(reference);
        Throwable parse = ThrowableParser.parse(data);
        return Fault.create(parse);
    }

    private static JdbcStorage storage(DataSource dataSource, Clock clock) {
        return new JdbcStorage(dataSource, "unearth", clock);
    }
}
