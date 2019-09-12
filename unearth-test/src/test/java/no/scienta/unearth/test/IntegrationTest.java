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

package no.scienta.unearth.test;

import no.scienta.unearth.analysis.CassandraInit;
import no.scienta.unearth.client.Page;
import no.scienta.unearth.client.UnearthlyClient;
import no.scienta.unearth.client.dto.*;
import no.scienta.unearth.server.Unearth;
import no.scienta.unearth.server.UnearthlyCassandraConfig;
import no.scienta.unearth.server.UnearthlyConfig;
import no.scienta.unearth.server.UnearthlyDbConfig;
import no.scienta.unearth.util.Throwables;
import org.junit.*;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({
    "OptionalGetWithoutIsPresent",
    "StaticVariableMayNotBeInitialized",
    "FieldCanBeLocal",
    "StaticVariableOfConcreteClass",
    "StaticVariableUsedBeforeInitialization"})
public class IntegrationTest {

    private static Unearth.State state;

    private static UnearthlyClient client;

    private static GenericContainer cassandra;

    private static GenericContainer postgres;

    private static CassandraInit init;

    private static final ScheduledExecutorService EXEC = Executors.newScheduledThreadPool(2);

    @Ignore
    @Test
    public void verifyFeedCounters() {
        Exception barf = new IOException("Barf"), barf2 = new IOException("Barf2");

        assertThat(client.globalFeedMax(), is(0L));
        Submission submit = client.submit("Exception in thread \"foobar\" " + Throwables.string(barf));
        assertThat(submit, not(nullValue()));

        assertThat(client.globalFeedMax(), is(1L));
        assertThat(client.faultFeedMax(submit.faultId), is(1L));
        assertThat(client.faultStrandFeedMax(submit.faultStrandId), is(1L));

//        assertThat(client.globalFeed().events.size(), is(1));
//        assertThat(client.faultFeed(submit.faultId).events.size(), is(1));
//        assertThat(client.faultStrandFeed(submit.faultStrandId).events.size(), is(1));

        client.submit(barf);
        client.submit(barf);
        client.submit(barf);
        client.submit(barf2);
        Submission submitBarf2 = client.submit(barf2);
        Submission submitAgain = client.submit(barf);

//        assertThat(client.globalFeed().events.size(), is(7));
//        assertThat(client.faultFeed(submit.faultId).events.size(), is(5));
//        assertThat(client.faultFeed(submitBarf2.faultId).events.size(), is(2));
//        assertThat(client.faultStrandFeed(submit.faultStrandId).events.size(), is(7));

        assertThat(submitAgain.faultId, is(submit.faultId));
        assertThat(submitBarf2.faultId, not(is(submit.faultId)));
        assertThat(submitAgain.faultStrandId, is(submit.faultStrandId));
        assertThat(submitBarf2.faultStrandId, is(submit.faultStrandId));

        FeedEntryDto faultEvent = client.feedEntry(submit.feedEntryId).get();
        assertThat(faultEvent.id, is(submit.feedEntryId));

        assertThat(faultEvent.sequenceNo, is(0L));
        assertThat(faultEvent.faultSequenceNo, is(0L));
        assertThat(faultEvent.faultStrandSequenceNo, is(0L));

        assertThat(client.faultStrandFeed(submit.faultStrandId).count, is(7L));

        FaultStrandEventSequenceDto dto = client.faultStrandFeed(submit.faultStrandId, Page.no(2).pageSize(2));
        assertThat(dto.count, is(2L));
        assertThat(dto.last, is(3L));
        assertThat(dto.offset, is(2L));
        assertThat(dto.sequenceType, is(SequenceType.FAULT_STRAND));
    }

    @Ignore
    @Test
    public void submitExceptionAsLogged() {
        Exception barf = new IOException("Barf"), barf2 = new IOException("Barf2");

        assertThat(client.globalFeedMax(), is(0L));
        Submission submit = client.submit("Exception in thread \"foobar\" " + Throwables.string(barf));

        Submission submitBarf2 = client.submit(barf2);
        Submission submitAgain = client.submit(barf);

        assertThat(submitAgain.faultId, is(submit.faultId));
        assertThat(submitBarf2.faultId, not(is(submit.faultId)));
        assertThat(submitAgain.faultStrandId, is(submit.faultStrandId));
        assertThat(submitBarf2.faultStrandId, is(submit.faultStrandId));

        Throwable throwable = client.throwable(submit.faultId).get();
        assertThat(throwable.getMessage(), equalTo("Barf"));

        FaultDto fault = client.fault(submit.faultId).get();
        assertThat(fault.id, is(submit.faultId));

        FaultStrandDto faultStrand = client.faultStrand(submit.faultStrandId).get();
        assertThat(faultStrand.id, is(submit.faultStrandId));

        FeedEntryDto faultEvent = client.feedEntry(submit.feedEntryId).get();
        assertThat(faultEvent.id, is(submit.feedEntryId));

        CauseDto[] causes = fault.causes;
        CauseStrandDto[] causeStrands = faultStrand.causeStrands;

        assertThat(causes.length, is(causeStrands.length));

        assertThat(causes[0].causeStrand.id, is(causeStrands[0].id));
        CauseIdDto id = causes[0].id;

        CauseDto cause = client.cause(id).get();
        assertThat(cause.message, is("Barf"));

        CauseStrandDto causeStrand = client.causeStrand(causeStrands[0].id).get();
        assertThat(causeStrand.className, is(IOException.class.getName()));
    }

    @BeforeClass
    public static void up() {
        Future<? extends GenericContainer<?>> cassandraFuture = EXEC.submit(IntegrationTest::startCassandra);
        Future<? extends GenericContainer<?>> postgresFuture = EXEC.submit(IntegrationTest::startPostgres);
        EXEC.shutdown();

        try {
            cassandra = cassandraFuture.get();
            postgres = postgresFuture.get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed", e);
        }

        UnearthlyConfig config = testConfig(cassandra, postgres);

        init = initCassandra(config.getCassandra());

        state = new Unearth(config).invoke();

        client = UnearthlyClient.connect(state.url());
    }

    @AfterClass
    public static void down() {
        if (state != null) {
            state.close();
        }
        if (init != null) {
            init.close();
        }
        if (cassandra != null) {
            cassandra.stop();
            cassandra.close();
        }
    }

    @After
    public void reset() {
        if (state != null) {
            state.reset();
        }
    }

    private static GenericContainer<?> startCassandra() {
        GenericContainer<?> cassandra =
            new GenericContainer<>("cassandra:3.11.4").withExposedPorts(9042);
        cassandra.start();
        return cassandra;
    }

    private static GenericContainer<?> startPostgres() {
        GenericContainer<?> postgres = new GenericContainer<>("postgres:12").withExposedPorts(5432);
        postgres.start();
        return postgres;
    }

    private static CassandraInit initCassandra(UnearthlyCassandraConfig cassandra) {
        return new CassandraInit(
            cassandra.getHost(),
            cassandra.getPort(),
            cassandra.getDc(),
            cassandra.getKeyspace()
        ).init();
    }

    private static UnearthlyConfig testConfig(
        GenericContainer<?> cassandraContainer,
        GenericContainer<?> postgresContainer
    ) {
        return new UnearthlyConfig(
            "/api/test",
            "localhost",
            0,
            true,
            true,
            cassandraConfig(cassandraContainer),
            postgresConfig(postgresContainer));
    }

    private static UnearthlyCassandraConfig cassandraConfig(GenericContainer<?> cassandraContainer) {
        return new UnearthlyCassandraConfig(
            cassandraContainer.getContainerIpAddress(),
            cassandraContainer.getFirstMappedPort(),
            "datacenter1",
            "testing");
    }

    private static UnearthlyDbConfig postgresConfig(GenericContainer<?> postgresContainer) {
        String postgresIP = postgresContainer.getContainerIpAddress();
        Integer postgresPort = postgresContainer.getFirstMappedPort();
        String postgresSchema = "postgres";
        String postgresJdbc = "jdbc:postgresql://" + postgresIP + ":" + postgresPort + "/" + postgresSchema;
        return new UnearthlyDbConfig(
            postgresIP,
            "postgres",
            "",
            postgresPort,
            postgresSchema,
            postgresJdbc);
    }
}
