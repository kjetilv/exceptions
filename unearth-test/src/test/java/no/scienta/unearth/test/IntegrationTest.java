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

import no.scienta.unearth.client.Print;
import no.scienta.unearth.client.UnearthlyClient;
import no.scienta.unearth.dto.*;
import no.scienta.unearth.server.Unearth;
import no.scienta.unearth.server.UnearthlyConfig;
import no.scienta.unearth.util.Throwables;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IntegrationTest {

    private static Unearth.State state;

    private static UnearthlyClient client;

    @Test
    public void submitExceptionAsLogged() {
        Exception barf = new IOException("Barf");
        Submission submit = client.submit("Exception in thread \"foobar\" " + Throwables.string(barf));

        System.out.println(Print.toString(submit));

        Throwable throwable = client.throwable(submit.getFaultId());
        assertThat(throwable.getMessage(), equalTo("Barf"));

        FaultDto fault = client.fault(submit.getFaultId());
        assertThat(fault.getId(), is(submit.getFaultId()));

        FaultStrandDto faultStrand = client.faultStrand(submit.getFaultStrandId());
        assertThat(faultStrand.getId(), is(submit.getFaultStrandId()));

        FaultEventDto faultEvent = client.faultEvent(submit.getFaultEventId());
        assertThat(faultEvent.getId(), is(submit.getFaultEventId()));

        assertThat(faultEvent.getSequenceNo(), is(0L));
        assertThat(faultEvent.getFaultSequenceNo(), is(0L));
        assertThat(faultEvent.getFaultStrandSequenceNo(), is(0L));

        List<CauseDto> causes = fault.getCauses();
        List<CauseStrandDto> causeStrands = faultStrand.getCauseStrands();

        assertThat(causes.size(), is(1));
        assertThat(causeStrands.size(), is(1));

        assertThat(causes.get(0).getCauseStrand().getId(), is(causeStrands.get(0).getId()));

        CauseIdDto id = causes.get(0).getId();

        CauseDto cause = client.cause(id);
        assertThat(cause.getMessage(), is("Barf"));

        CauseStrandDto causeStrand = client.causeStrand(causeStrands.get(0).getId());
        assertThat(causeStrand.getClassName(), is(IOException.class.getName()));
    }

    @Test
    public void submitActualException() {
        Submission submit = client.submit(new IOException("Dang it!"));
        FaultIdDto faultId = submit.getFaultId();
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
