package link.stuf.exceptions.core.digest;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThrowableDigest extends AbstractHashed{

    public static ThrowableDigest create(Throwable mainThrowable) {
        LinkedList<ShadowThrowable> ts = new LinkedList<>();
        List<Throwable> reversed = new LinkedList<>();

        Throwables.stream(mainThrowable).forEach(reversed::add);
        reversed.forEach(throwable ->
            ts.add(0, ShadowThrowable.create(throwable)));

        return new ThrowableDigest(ts);
    }

    private final List<ShadowThrowable> digests;

    private ThrowableDigest(List<ShadowThrowable> digests) {
        this.digests = Collections.unmodifiableList(digests);
    }

    public ThrowableDigest map(UnaryOperator<ShadowThrowable> mapper) {
        return new ThrowableDigest(digests.stream().map(mapper).collect(Collectors.toList()));
    }

    public Throwable toThrowable() {
        List<ShadowThrowable> copy = new ArrayList<>(digests);
        Collections.reverse(copy);
        return copy.stream().reduce(null, (exception, digest) -> digest.toException(exception), NO_COMBINE);
    }

    private static final BinaryOperator<Throwable> NO_COMBINE = (throwable, throwable2) -> {
        throw new IllegalStateException("No combine");
    };

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ThrowableDigest) {
            ThrowableDigest td = (ThrowableDigest) o;
            return digests.size() == td.digests.size() && IntStream.range(0, digests.size())
                .allMatch(i ->
                    Objects.equals(digests.get(0), td.digests.get(0)));
        }
        return false;
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        digests.forEach(digest ->
            digest.hashTo(hash));
    }
}
