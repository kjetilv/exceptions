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

package unearth.munch.base;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;
import unearth.munch.print.GroupedList;

public class GrouperTest {
    
    @Test
    public void groupTest() {
        Function<String, Optional<Collection<String>>> stringOptionalFunction =
            s -> s.startsWith("zot.x") ? Optional.of(Collections.singleton("zot.x"))
                : s.startsWith("zot") ? Optional.of(Collections.singleton("zot"))
                    : Optional.empty();
        GroupedList<Collection<String>, String> group = GroupedList.group(
            Arrays.asList(
                "foo.bar",
                "foo.zot",
                "zot.a",
                "zot.b",
                "zot.x.1",
                "zot.x.2",
                "zot.c",
                "bar.zip",
                "bar.zip",
                "zot.x.1",
                "zot.x.2",
                "bar.zip",
                "bar.zip"
            ),
            stringOptionalFunction
        );
        group.forEach((strings, strings2) -> {
            System.out.println(strings);
            strings2.stream().map(s -> s + "  ").forEach(System.out::println);
        });
    }
}
