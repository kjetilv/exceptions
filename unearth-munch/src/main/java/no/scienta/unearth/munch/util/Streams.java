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

package no.scienta.unearth.munch.util;

import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@SuppressWarnings("WeakerAccess")
public final class Streams {

    public static <T> Stream<T> reverse(Stream<T> s) {
        return reverse(s.collect(Collectors.toList()));
    }

    public static <T> Stream<T> reverse(List<T> s) {
        return StreamSupport.stream(new ReverseSpliterator<>(s), false);
    }

    public static Stream<Throwable> causes(Throwable throwable) {
        return StreamSupport.stream(new CauseSpliterator(throwable), false);
    }

    public static <T, U> U quickReduce(Collection<T> stream, BiFunction<U, ? super T, U> accumulator) {
        return quickReduce(stream.stream(), accumulator);
    }

    public static <T, U> U quickReduce(Stream<T> stream, BiFunction<U, ? super T, U> accumulator) {
        return quickReduce(stream, null, accumulator);
    }

    public static <T, U> U quickReduce(Collection<T> stream, U identity, BiFunction<U, ? super T, U> accumulator) {
        return quickReduce(stream.stream(), identity, accumulator);
    }

    public static <T, U> U quickReduce(Stream<T> stream, U identity, BiFunction<U, ? super T, U> accumulator) {
        return stream.reduce(identity, accumulator, Util.noCombine());
    }

    private static class CauseSpliterator extends Spliterators.AbstractSpliterator<Throwable> {

        private Throwable throwable;

        CauseSpliterator(Throwable throwable) {
            super(Long.MAX_VALUE, IMMUTABLE | ORDERED);
            this.throwable = throwable;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Throwable> action) {
            if (throwable == null) {
                return false;
            }
            try {
                action.accept(throwable);
                return true;
            } finally {
                throwable = throwable.getCause();
            }
        }
    }

    private static class ReverseSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

        private final List<T> s;

        private int index;

        private ReverseSpliterator(List<T> s) {
            super(s.size(), Spliterator.ORDERED);
            this.s = s;
            index = s.size();
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (index == 0) {
                return false;
            }
            index--;
            action.accept(s.get(index));
            return true;
        }
    }
}
