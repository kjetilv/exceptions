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

package no.scienta.unearth.munch.util;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertTrue;

public class UtilTest {

    @Test
    public void longerThan() {
        assertTrue(Util.isLongerThan(Duration.ofMinutes(2), Duration.ofMillis(200)));
    }

    @Test
    public void shorterThan() {
        assertTrue(Util.isShorterThan(Duration.ofMillis(200), Duration.ofMinutes(2)));
    }
}
