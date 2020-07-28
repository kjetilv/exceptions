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

package unearth.test;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import unearth.client.Page;
import unearth.client.UnearthlyClient;
import unearth.client.dto.CauseDto;
import unearth.client.dto.CauseIdDto;
import unearth.client.dto.CauseStrandDto;
import unearth.client.dto.FaultDto;
import unearth.client.dto.FaultStrandDto;
import unearth.client.dto.FaultStrandEventSequenceDto;
import unearth.client.dto.FeedEntryDto;
import unearth.client.dto.SequenceType;
import unearth.client.dto.Submission;
import unearth.util.Throwables;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("StaticVariableMayNotBeInitialized")
public class IntegrationTest {

    private static DockerStartup dockerStartup;

    private static UnearthlyClient client;

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
        dockerStartup = new DockerStartup();
        client = dockerStartup.getClient();
    }

    @AfterClass
    public static void down() {
        dockerStartup.stop();
    }

    @After
    public void reset() {
        dockerStartup.reset();
    }

}
