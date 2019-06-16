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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Grouper {

    public static <T> GroupedListItem<T> group(
        List<T> items,
        Function<T, String> grouper
    ) {
        String currentGroup = null;
        GroupedListItem<T> list = GroupedListItem.init();
        for (T item : items) {
            String itemGroup = grouper.apply(item);
            if (Objects.equals(itemGroup, currentGroup)) {
                list = list.add(item);
            } else if (currentGroup == null) {
                currentGroup = itemGroup;
                list = list.enterGroup(currentGroup, item);
            } else if (itemGroup == null) {
                currentGroup = null;
                list = list.exitGroup(item);
            } else {
                list = list.add(item);
            }
        }
        return list.unwind();
    }

    private static boolean inGroup(Deque<String> groups, String itemGroup) {
        return itemGroup == null && groups.isEmpty()
            || !groups.isEmpty() && Objects.equals(itemGroup, groups.getLast());
    }
}
