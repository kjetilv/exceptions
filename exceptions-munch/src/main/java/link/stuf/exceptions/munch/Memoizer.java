package link.stuf.exceptions.munch;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class Memoizer {

    public static <O> Supplier<O> get(Supplier<O> supplier) {
        return supplier == null || supplier instanceof SuppMemoizer<?> ? supplier : new SuppMemoizer<>(supplier);
    }

    private static final class SuppMemoizer<T> implements Supplier<T> {

        private final Supplier<T> supplier;

        private final AtomicReference<T> value = new AtomicReference<>();

        private SuppMemoizer(Supplier<T> supplier) {
            this.supplier = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public T get() {
            return value.updateAndGet(v -> v == null ? supplier.get() : v);
        }
    }

    private Memoizer() {
    }
}
