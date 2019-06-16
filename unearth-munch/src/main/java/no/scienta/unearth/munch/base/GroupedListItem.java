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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class GroupedListItem<T> {

    public static <T> GroupedListItem<T> init() {
        return new GroupedListItem<>(
            null, 0, null, null, null, 0, 0);
    }

    private final String group;

    private final int level;

    private final T item;

    private final int depth;

    private final int groupSize;

    private final GroupedListItem<T> previous;

    private final GroupedListItem<T> next;

    private GroupedListItem(
        String group,
        int level,
        T item,
        GroupedListItem<T> previous,
        GroupedListItem<T> next,
        int depth,
        int groupSize
    ) {
        this.group = group;
        this.level = level;
        this.item = item;
        this.previous = previous;
        this.next = next;
        this.depth = depth;
        this.groupSize = groupSize;
    }

    public GroupedListItem<T> add(T item) {
        Objects.requireNonNull(item, "No item");
        return new GroupedListItem<>(
            null, level, item, this.item == null ? null : this, null, depth + 1, groupSize + 1);
    }

    public GroupedListItem<T> enterGroup(String group, T item) {
        Objects.requireNonNull(item, "No item");
        return new GroupedListItem<>(
            group, level + 1, item, this, null, depth + 1, 1);
    }

    public GroupedListItem<T> exitGroup(T item) {
        Objects.requireNonNull(item, "No item");
        return ifPrevious(() ->
            new GroupedListItem<>(
                null, level - 1, item, this, null, depth + 1, 1));
    }

    public GroupedListItem<T> unwind() {
        return ifPrevious(() ->
            unwind(null, depth, groupSize));
    }

    public List<T> toList() {
        return ifNext(() ->
            toList(new ArrayList<>(depth)));
    }

    public List<String> printList(ListItemPrinter<T> printer) {
        return ifNext(() ->
            printList(printer, level, new ArrayList<>(depth)));
    }

    private <R> R ifNext(Supplier<R> run) {
        if (next != null) {
            return run.get();
        }
        throw new IllegalStateException("Not unwound: " + this);
    }

    private <R> R ifPrevious(Supplier<R> run) {
        if (previous != null) {
            return run.get();
        }
        throw new IllegalStateException("Not wound: " + this);
    }

    private List<String> printList(ListItemPrinter<T> printer, int traversedLevel, List<String> list) {
        if (this.level == traversedLevel) {
            list.add(printer.print(this.level, item));
            return next == null ? list : next.printList(printer, traversedLevel, list);
        }
        if (this.level > traversedLevel) {
            list.add(printer.printGroup(this.level, group, groupSize));
            return printList(printer, this.level, list);
        }
        list.add(printer.print(this.level, item));
        return next == null ? list : next.printList(printer, this.level, list);
    }

    private List<T> toList(List<T> list) {
        list.add(item);
        return next == null ? list : next.toList(list);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + item + "]";
    }

    private GroupedListItem<T> unwind(GroupedListItem<T> next, int depth, int groupSize) {
        GroupedListItem<T> newMe = new GroupedListItem<>(
            group, level, item, previous, next, depth, groupSize);
        return previous == null ? newMe
            : previous.unwind(newMe, depth, previous.level == this.level ? groupSize : previous.groupSize);
    }
}
