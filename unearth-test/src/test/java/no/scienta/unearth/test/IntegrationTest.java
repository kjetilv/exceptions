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
import no.scienta.unearth.dto.FaultIdDto;
import no.scienta.unearth.dto.Submission;
import no.scienta.unearth.server.Unearth;
import no.scienta.unearth.server.UnearthlyConfig;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class IntegrationTest {

    @Test
    public void testSubmit() {
        Unearth.State state = new Unearth(new UnearthlyConfig(
            "/api/test",
            "localhost",
            0,
            true,
            true
        )).invoke();

        UnearthlyClient client = UnearthlyClient.connect(state.url());

        Submission submit = client.submit(new IOException("Dang it!"));

        FaultIdDto faultId = submit.getFaultId();

        Throwable retrieve = client.retrieve(faultId);

        assertThat(retrieve.getMessage(), CoreMatchers.equalTo("Dang it!"));

        state.close();
    }
}
