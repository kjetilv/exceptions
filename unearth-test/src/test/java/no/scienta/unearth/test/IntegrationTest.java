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

import no.scienta.unearth.client.UnearthlyClient;
import no.scienta.unearth.client.dto.*;
import no.scienta.unearth.server.Unearth;
import no.scienta.unearth.server.UnearthlyConfig;
import no.scienta.unearth.util.Throwables;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IntegrationTest {

    private static Unearth.State state;

    private static UnearthlyClient client;

    @Test
    public void submitExceptionAsLogged() {
        Exception barf = new IOException("Barf");
        no.scienta.unearth.client.dto.Submission submit = client.submit("Exception in thread \"foobar\" " + Throwables.string(barf));

        Throwable throwable = client.throwable(submit.faultId);
        assertThat(throwable.getMessage(), equalTo("Barf"));

        FaultDto fault = client.fault(submit.faultId);
        assertThat(fault.id.uuid, is(submit.faultId.uuid));

        FaultStrandDto faultStrand = client.faultStrand(submit.faultStrandId);
        assertThat(faultStrand.id.uuid, is(submit.faultStrandId.uuid));

        FaultEventDto faultEvent = client.faultEvent(submit.faultEventId);
        assertThat(faultEvent.id.uuid, is(submit.faultEventId.uuid));

        assertThat(faultEvent.sequenceNo, is(0L));
        assertThat(faultEvent.faultSequenceNo, is(0L));
        assertThat(faultEvent.faultStrandSequenceNo, is(0L));

        CauseDto[] causes = fault.causes;
        CauseStrandDto[] causeStrands = faultStrand.causeStrands;

        assertThat(causes.length, is(causeStrands.length));

        assertThat(causes[0].causeStrand.id.uuid, is(causeStrands[0].id.uuid));
        CauseIdDto id = causes[0].id;

        CauseDto cause = client.cause(id);
        assertThat(cause.message, is("Barf"));

        CauseStrandDto causeStrand = client.causeStrand(causeStrands[0].id);
        assertThat(causeStrand.className, is(IOException.class.getName()));
    }

    @Test
    public void submitActualException() {
        Submission submit = client.submit(new IOException("Dang it!"));
        FaultIdDto faultId = submit.faultId;
        Throwable retrieve = client.throwable(faultId);
        assertThat(retrieve.getMessage(), equalTo("Dang it!"));
    }

    @BeforeClass
    public static void up() {
        state = new Unearth(new UnearthlyConfig(
            "/api/test",
            "localhost",
            0,
            true,
            true
        )).invoke();
        client = UnearthlyClient.connect(state.url());
    }

    @AfterClass
    public static void down() {
        state.close();
    }

    @After
    public void reset() {
        state.reset();
    }
}
