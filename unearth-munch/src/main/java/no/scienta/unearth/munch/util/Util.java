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

package no.scienta.unearth.munch.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;

public final class Util {

    public static <T> Collection<T> orEmpty(Collection<T> t) {
        return t == null || t.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(t));
    }

    public static <T> List<T> orEmptyList(Collection<T> t) {
        return t == null || t.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(t));
    }

    public static <T> List<T> orEmpty(List<T> t) {
        return t == null || t.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(t));
    }

    public static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
    }

    public static boolean isLongerThan(Duration time, Duration comparedValue) {
        return comparedValue.minus(time).isNegative();
    }

    public static boolean isShorterThan(Duration time, Duration comparedValue) {
        return time.minus(comparedValue).isNegative();
    }

    private Util() {
    }
}
