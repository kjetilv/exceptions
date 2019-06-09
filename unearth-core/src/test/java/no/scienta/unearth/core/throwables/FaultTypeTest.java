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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.core.throwables;

import no.scienta.unearth.munch.data.FaultType;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class FaultTypeTest {

    @Test
    public void hash() {
        FaultType d1 = newFaultType(); FaultType d2 = newFaultType();
        assertEquals(d1.getHash(), d2.getHash());
    }

    private FaultType newFaultType() {
        Throwable cause = new Throwable();
        Throwable cause1 = new Throwable(cause);
        return FaultType.create(new Throwable(cause1));
    }

    @Test
    public void hash2() {
        FaultType digest1 = newFaultType();
        FaultType digest2 = newFaultType();
        assertNotEquals(digest1.getHash(), digest2.getHash());
    }

    @Test
    public void fail() {
        new IOException("Foo is bar", new IOException("Zot")).printStackTrace(System.out);
    }
}
