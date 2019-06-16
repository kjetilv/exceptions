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
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GroupedListItemTest {

    @Test
    public void simpleChain() {
        GroupedListItem<String> unwind = GroupedListItem.<String>init()
            .add("foo.bar")
            .add("foo.baz")
            .unwind();

        List<String> strings = unwind.toList();
        assertEquals(strings, Arrays.asList("foo.bar", "foo.baz"));
    }

    @Test
    public void groupedChain() {
        GroupedListItem<String> list = GroupedListItem.<String>init()
            .add("foo.bar")
            .add("foo.baz")
            .enterGroup("zot", "zot.biz")
            .add("zot.zap")
            .exitGroup("foo.zot");
        GroupedListItem<String> unwind = list
            .unwind();

        List<String> strings = unwind.toList();
        assertEquals(strings, Arrays.asList(
            "foo.bar",
            "foo.baz",
            "zot.biz",
            "zot.zap",
            "foo.zot"));

        unwind.printList(new SimpleStringListItemPrinter()).forEach(System.out::println);
    }
}
