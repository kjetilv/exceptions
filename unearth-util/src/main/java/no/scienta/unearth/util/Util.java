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

package no.scienta.unearth.util;

import java.time.Duration;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Util {

    private Util() {
    }

    public static <T> Collection<T> orEmpty(Collection<T> t) {
        return t == null || t.isEmpty() ? Collections.emptyList() : List.copyOf(t);
    }

    public static <T> List<T> orEmptyList(Collection<T> t) {
        return t == null || t.isEmpty() ? Collections.emptyList() : List.copyOf(t);
    }

    public static <K, V> Map<K, V> orEmptyMap(Map<K, V> t) {
        return t == null || t.isEmpty() ? Collections.emptyMap() : Map.copyOf(t);
    }

    public static <T> List<T> orEmpty(List<T> t) {
        return t == null || t.isEmpty() ? Collections.emptyList() : List.copyOf(t);
    }

    public static boolean isLongerThan(Duration time, Duration comparedValue) {
        return comparedValue.minus(time).isNegative();
    }

    public static boolean isShorterThan(Duration time, Duration comparedValue) {
        return time.minus(comparedValue).isNegative();
    }

    public static <I, T> Map<I, T> byId(Collection<T> vs, Function<T, I> identifier) {
        return vs.stream()
            .collect(Collectors.groupingBy(identifier, HashMap::new, Collectors.toSet()))
            .entrySet().stream()
            .filter(Util::singleEntry)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().iterator().next())
            );
    }

    static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
    }

    private static <K, V> boolean singleEntry(Map.Entry<K, Set<V>> e) {
        if (e.getValue().isEmpty()) {
            return false;
        }
        if (e.getValue().size() == 1) {
            return true;
        }
        throw new IllegalStateException("Multiple elements for " + e.getKey() + ": " + e.getValue());
    }
}
