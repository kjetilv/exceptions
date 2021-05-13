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

package unearth.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StreamsTest {

    @Test
    public void slice() {
        Integer[] ints = { 0, 1, 2, 3, 4, 5 };

        assertEquals(
            Arrays.asList(2, 3),
            Streams.slice(ints, 2, 4).collect(Collectors.toList()));
        assertEquals(
            Arrays.asList(3, 4, 5),
            Streams.slice(ints, 3, 6).collect(Collectors.toList()));
    }

    @Test
    public void tuplify() {
        List<Integer> ints = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> triples =
            Streams.tuplify(ints, 3).collect(Collectors.toList());
        assertEquals(6, triples.size());
        assertEquals(Arrays.asList(0, 1, 2), triples.get(0));
        assertEquals(Arrays.asList(1, 2, 3), triples.get(1));
        assertEquals(Arrays.asList(5, 6, 7), triples.get(5));
    }
}
