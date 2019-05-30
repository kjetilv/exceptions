package link.stuf.exceptions.core.utils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class Streams {

    public static <T> Stream<T> reverse(Stream<T> s) {
        return reverse(s.collect(Collectors.toList()));
    }

    public static <T> Stream<T> reverse(List<T> s) {
        return StreamSupport.stream(new ReverseSpliterator<T>(s), false);
    }

    public static Stream<Throwable> causes(Throwable throwable) {
        return StreamSupport.stream(new CauseSpliterator(throwable), false);
    }

    public static IntStream reverseRange(int start, int end) {
        return IntStream.range(start, end).map(i -> end - i - 1);
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
