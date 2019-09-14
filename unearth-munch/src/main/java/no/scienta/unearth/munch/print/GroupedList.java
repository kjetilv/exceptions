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

package no.scienta.unearth.munch.print;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class GroupedList<G, T> {

    private final Deque<Group<G, T>> groups = new LinkedList<>();

    public static <G, T> GroupedList<G, T> group(List<T> items, Function<T, Optional<G>> grouper) {
        GroupedList<G, T> groupedList = new GroupedList<>();
        for (T item : items) {
            Optional<G> group = grouper.apply(item);
            if (group.isPresent()) {
                groupedList.add(group.get(), item);
            } else {
                groupedList.add(item);
            }
        }
        return groupedList;
    }

    public void forEach(BiConsumer<G, List<T>> groupAction) {
        groups().forEach(group ->
            groupAction.accept(group.ref(), group.items()));
    }

    private void add(T item) {
        add(null, item);
    }

    private void add(G groupRef, T item) {
        if (groups.isEmpty() || !currentGroup().hasRef(groupRef)) {
            addGroup(group(groupRef));
        }
        currentGroup().add(item);
    }

    private List<Group<G, T>> groups() {
        return List.copyOf(groups);
    }

    private Group<G, T> group(G g) {
        return new Group<>(g);
    }

    private void addGroup(Group<G, T> base) {
        groups.add(base);
    }

    private Group<G, T> currentGroup() {
        return groups.getLast();
    }

    private static final class Group<G, T> {

        private final G ref;

        private final List<T> items = new ArrayList<>();

        Group(G ref) {
            this.ref = ref;
        }

        G ref() {
            return ref;
        }

        List<T> items() {
            return Collections.unmodifiableList(items);
        }

        boolean hasRef(G ref) {
            return Objects.equals(this.ref, ref);
        }

        void add(T item) {
            items.add(item);
        }
    }
}
