package link.stuf.exceptions.core.utils;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
