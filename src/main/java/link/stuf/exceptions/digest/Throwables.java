package link.stuf.exceptions.digest;

import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class Throwables {

    static Stream<Throwable> stream(Throwable throwable) {
        return StreamSupport.stream(new ThrowableChainSpliterator(throwable), false);
    }

    private static class ThrowableChainSpliterator extends Spliterators.AbstractSpliterator<Throwable> {

        private Throwable throwable;

        ThrowableChainSpliterator(Throwable throwable) {
            super(Long.MAX_VALUE, IMMUTABLE | ORDERED);
            this.throwable = throwable;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Throwable> action) {
            action.accept(throwable);
            Throwable cause = throwable.getCause();
            if (cause == null || cause == throwable) {
                return false;
            }
            throwable = cause;
            return true;
        }
    }
}
