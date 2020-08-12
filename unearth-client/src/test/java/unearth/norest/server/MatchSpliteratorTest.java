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
package unearth.norest.server;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MatchSpliteratorTest {
    
    static final Pattern LETTERS = Pattern.compile(".*\\d(\\w)\\d(\\w)(\\d)(\\w)\\d(\\w).*");
    
    @Test
    public void test_it() {
        Matcher matcher = LETTERS.matcher("xx1a2b3c4xx");
        assertTrue(matcher.matches());
        assertEquals(
            List.of("a", "b", "3", "c", "x"),
            MatchSpliterator.stream(matcher).collect(Collectors.toList()));
    }
}
