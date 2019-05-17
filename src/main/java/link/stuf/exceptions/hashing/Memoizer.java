package link.stuf.exceptions.hashing;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Memoizer {

    private Memoizer() {
    }

    static <I, O> Function<I, O> apply(Function<I, O> fun) {
        return fun == null || fun instanceof FunMemoizer<?, ?>
            ? fun
            : new FunMemoizer<>(fun);
    }

    static <O> Supplier<O> get(Supplier<O> supplier) {
        return supplier == null || supplier instanceof SuppMemoizer<?>
            ? supplier
            : new SuppMemoizer<>(supplier);
    }

    private static final class FunMemoizer<I, O> implements Function<I, O> {

        private final Function<I, O> function;

        private final Map<I, O> values = new ConcurrentHashMap<>();

        FunMemoizer(Function<I, O> function) {
            this.function = Objects.requireNonNull(function, "function");
        }

        @Override
        public O apply(I in) {
            return values.computeIfAbsent(in, function);
        }
    }

    private static final class SuppMemoizer<T> implements Supplier<T> {

        private final Supplier<T> supplier;

        private final AtomicReference<T> value = new AtomicReference<>();

        private SuppMemoizer(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public T get() {
            return value.updateAndGet(v -> v == null
                ? supplier.get()
                : v);
        }
    }
}
