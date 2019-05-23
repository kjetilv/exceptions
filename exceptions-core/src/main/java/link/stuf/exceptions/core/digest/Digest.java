package link.stuf.exceptions.core.digest;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Digest extends AbstractHashed
    implements Iterable<ThrowableDigest> {

    public static Digest create(Throwable mainThrowable) {
        LinkedList<ThrowableDigest> ts = new LinkedList<>();
        List<ThrowableDigest> rs = new LinkedList<>();
        List<Throwable> reversed = new LinkedList<>();

        Throwables.stream(mainThrowable).forEach(reversed::add);
        reversed.forEach(throwable -> {
            ThrowableDigest cause = ts.isEmpty() ? null : ts.getFirst();
            ThrowableDigest digest = new ThrowableDigest(throwable, cause);
            ts.add(0, digest);
            rs.add(digest);
        });

        return new Digest(ts, rs);
    }

    private final List<ThrowableDigest> digests;

    private final List<ThrowableDigest> reversed;

    private Digest(List<ThrowableDigest> digests, List<ThrowableDigest> reversed) {
        this.digests = Collections.unmodifiableList(digests);
        this.reversed = Collections.unmodifiableList(reversed);
    }

    public Digest map(UnaryOperator<ThrowableDigest> mapper) {
        return new Digest(
            digests.stream().map(mapper).collect(Collectors.toList()),
            reversed.stream().map(mapper).collect(Collectors.toList())
        );
    }

    public Throwable toThrowable() {
        return reversed.stream().reduce(null, (exception, digest) -> digest.toException(exception), NO_COMBINE);
    }

    private static final BinaryOperator<Throwable> NO_COMBINE = (throwable, throwable2) -> {
        throw new IllegalStateException("No combine");
    };

    @Override
    public Iterator<ThrowableDigest> iterator() {
        return digests.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Digest) {
            Digest td = (Digest) o;
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
