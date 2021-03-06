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

package unearth.munch;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import unearth.munch.model.CauseStrand;

public class ThrowableStackTest {
    
    @Test
    public void can_hash() {
        CauseStrand digest = CauseStrand.create(new Exception());
        
        UUID hash = digest.getHash();
        
        Assert.assertEquals(hash, digest.getHash());
    }
}
