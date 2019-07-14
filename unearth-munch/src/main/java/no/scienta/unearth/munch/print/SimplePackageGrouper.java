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

package no.scienta.unearth.munch.print;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class SimplePackageGrouper implements ConfigurableThrowableRenderer.PackageGrouper {

    private final Collection<Collection<String>> groups;

    public SimplePackageGrouper(String... groups) {
        this(Arrays.asList(groups));
    }

    public SimplePackageGrouper(List<String> groups) {
        this(Collections.singleton(groups));
    }

    public SimplePackageGrouper(Collection<Collection<String>> groups) {
        this.groups = groups == null || groups.isEmpty() || groups.stream().allMatch(g -> g == null || g.isEmpty())
            ? Collections.emptyList()
            : Collections.unmodifiableCollection(new ArrayList<>(groups));
    }

    @Override
    public Optional<Collection<String>> apply(CauseFrame causeFrame) {
        Stream<Collection<String>> collectionStream = groups.stream()
            .filter(group ->
                group.stream().anyMatch(name ->
                    causeFrame.className().startsWith(name)));
        return collectionStream
            .max(Comparator.comparing(maxMatch(causeFrame)));
    }

    private static Function<Collection<String>, Integer> maxMatch(CauseFrame causeFrame) {
        return group -> group.stream()
            .filter(name ->
                causeFrame.className().startsWith(name))
            .mapToInt(String::length)
            .max()
            .orElse(0);
    }
}
