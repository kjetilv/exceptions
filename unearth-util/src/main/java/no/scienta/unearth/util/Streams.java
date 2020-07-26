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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streams {

    private Streams() {
    }

    public static <T> Stream<T> reverse(Stream<T> s) {
        return reverse(s.collect(Collectors.toList()));
    }

    public static <T> Stream<T> reverse(Collection<T> s) {
        return StreamSupport.stream(new ReverseSpliterator<>(s), false);
    }

    public static Stream<Throwable> causes(Throwable throwable) {
        return chain(throwable, Throwable::getCause);
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

    public static <T> Stream<T> chain(T head, Function<T, T> next) {
        return StreamSupport.stream(
            new NextSpliterator<>(head, next),
            false);
    }

    public static <T> String args(Collection<T> ts) {
        return ts.stream().map(t -> "?").collect(Collectors.joining(", "));
    }

    private static final class ReverseSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

        private final List<T> s;

        private int index;

        private ReverseSpliterator(Collection<T> s) {
            super(s.size(), ORDERED);
            this.s = s instanceof List<?> ? (List<T>) s : new ArrayList<>(s);
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

    private static final class NextSpliterator<T> extends Spliterators.AbstractSpliterator<T> {

        private final Function<T, T> next;

        private T current;

        private NextSpliterator(T head, Function<T, T> next) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.ORDERED);
            this.next = next;
            current = head;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (current == null) {
                return false;
            }
            action.accept(current);
            current = next.apply(current);
            return true;
        }
    }
}
