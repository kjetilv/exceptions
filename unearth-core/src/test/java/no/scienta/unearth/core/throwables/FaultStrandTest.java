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

package no.scienta.unearth.core.throwables;

import no.scienta.unearth.munch.model.FaultStrand;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FaultStrandTest {

    @Test
    public void hash() {
        FaultStrand d1 = newFaultStrand();
        FaultStrand d2 = newFaultStrand();
        assertEquals(d1.getHash(), d2.getHash());
    }

    private FaultStrand newFaultStrand() {
        Throwable cause = new Throwable();
        Throwable cause1 = new Throwable(cause);
        return FaultStrand.create(new Throwable(cause1));
    }

    @Test
    public void hash2() {
        FaultStrand[] strands = new FaultStrand[2];
        new Object() {
            {
                strands[0] = newFaultStrand();
            }
        };
        strands[1] = newFaultStrand();
        assertNotEquals(strands[0].getHash(), strands[1].getHash());
    }

    @Test
    public void fail() {
        new IOException("Foo is bar", new IOException("Zot")).printStackTrace(System.out);
    }
}
