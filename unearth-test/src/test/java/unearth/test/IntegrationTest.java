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
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import unearth.api.dto.CauseDto;
import unearth.api.dto.CauseIdDto;
import unearth.api.dto.CauseStrandDto;
import unearth.api.dto.FaultDto;
import unearth.api.dto.FaultStrandDto;
import unearth.api.dto.FaultStrandEventSequenceDto;
import unearth.api.dto.FeedEntryDto;
import unearth.api.dto.SequenceType;
import unearth.api.dto.Submission;
import unearth.client.Page;
import unearth.client.UnearthlyClient;
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
        assertThat(client.faultFeedMax(submit.getFaultId()), is(1L));
        assertThat(client.faultStrandFeedMax(submit.getFaultStrandId()), is(1L));

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

        assertThat(submitAgain.getFaultId(), is(submit.getFaultId()));
        assertThat(submitBarf2.getFaultId(), not(is(submit.getFaultId())));
        assertThat(submitAgain.getFaultStrandId(), is(submit.getFaultStrandId()));
        assertThat(submitBarf2.getFaultStrandId(), is(submit.getFaultStrandId()));

        FeedEntryDto feedEntry = client.feedEntry(submit.getFeedEntryId()).get();
        assertThat(feedEntry.getFaultEvent().getId(), is(submit.getFeedEntryId()));

        assertThat(feedEntry.getSequenceNo(), is(0L));
        assertThat(feedEntry.getFaultSequenceNo(), is(0L));
        assertThat(feedEntry.getFaultStrandSequenceNo(), is(0L));

        assertThat(client.faultStrandFeed(submit.getFaultStrandId()).getCount(), is(7L));

        FaultStrandEventSequenceDto dto = client.faultStrandFeed(submit.getFaultStrandId(), Page.no(2).pageSize(2));
        assertThat(dto.getCount(), is(2L));
        assertThat(dto.getLast(), is(3L));
        assertThat(dto.getOffset(), is(2L));
        assertThat(dto.getSequenceType(), is(SequenceType.FAULT_STRAND));
    }

    @Ignore
    @Test
    public void submitExceptionAsLogged() {
        Exception barf = new IOException("Barf"), barf2 = new IOException("Barf2");

        assertThat(client.globalFeedMax(), is(0L));
        Submission submit = client.submit("Exception in thread \"foobar\" " + Throwables.string(barf));

        Submission submitBarf2 = client.submit(barf2);
        Submission submitAgain = client.submit(barf);

        assertThat(submitAgain.getFaultId(), is(submit.getFaultId()));
        assertThat(submitBarf2.getFaultId(), not(is(submit.getFaultId())));
        assertThat(submitAgain.getFaultStrandId(), is(submit.getFaultStrandId()));
        assertThat(submitBarf2.getFaultStrandId(), is(submit.getFaultStrandId()));

        Throwable throwable = client.throwable(submit.getFaultId()).get();
        assertThat(throwable.getMessage(), equalTo("Barf"));

        FaultDto fault = client.fault(submit.getFaultId()).get();
        assertThat(fault.getId(), is(submit.getFaultId()));

        FaultStrandDto faultStrand = client.faultStrand(submit.getFaultStrandId()).get();
        assertThat(faultStrand.getId(), is(submit.getFaultStrandId()));

        FeedEntryDto faultEvent = client.feedEntry(submit.getFeedEntryId()).get();
        assertThat(faultEvent.getFaultEvent().getId(), is(submit.getFeedEntryId()));

        List<CauseDto> causes = fault.getCauses();
        List<CauseStrandDto> causeStrands = faultStrand.getCauseStrands();

        assertThat(causes.size(), is(causeStrands.size()));

        assertThat(causes.get(0).getCauseStrand().getId(), is(causeStrands.get(0).getId()));
        CauseIdDto id = causes.get(0).getId();

        CauseDto cause = client.cause(id).get();
        assertThat(cause.getMessage(), is("Barf"));

        CauseStrandDto causeStrand = client.causeStrand(causeStrands.get(0).getId()).get();
        assertThat(causeStrand.getClassName(), is(IOException.class.getName()));
    }

    @BeforeClass
    public static void up() {
        dockerStartup = new DefaultDockerStartup();
        client = dockerStartup.getClient();
    }

    @AfterClass
    public static void down() {
        dockerStartup.close();
    }

    @After
    public void reset() {
        dockerStartup.reset();
    }
}
