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

package no.scienta.unearth.munch.base;

import org.junit.Test;

import java.util.Arrays;

public class GrouperTest {

    @Test
    public void groupTest() {
        GroupedListItem<String> group = Grouper.group(
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
            s -> s.startsWith("zot.x") ? "zot.x"
                : s.startsWith("zot") ? "zot"
                : null
        );
        group.printList(new SimpleStringListItemPrinter()).forEach(System.out::println);
    }
}
