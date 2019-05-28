package link.stuf.exceptions.core.throwables;

import link.stuf.exceptions.core.hashing.AbstractHashed;
import link.stuf.exceptions.core.utils.Streams;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableSpecies extends AbstractHashed {

    public static ThrowableSpecies create(Throwable throwable) {
        Stream<ShadowThrowable> shadows =
            Streams.causes(throwable).map(ShadowThrowable::create);
        List<ShadowThrowable> causes =
            Streams.reverse(shadows).collect(Collectors.toList());
        return new ThrowableSpecies(causes);
    }

    private final List<ShadowThrowable> chain;

    private ThrowableSpecies(List<ShadowThrowable> chain) {
        this.chain = Collections.unmodifiableList(chain);
    }

    public ThrowableSpecies map(UnaryOperator<ShadowThrowable> mapper) {
        return new ThrowableSpecies(chain.stream().map(mapper).collect(Collectors.toList()));
    }

    public Throwable toThrowable() {
        return Streams.reverse(chain).reduce(
            null,
            (exception, digest) ->
                digest.toException(exception),
            NO_COMBINE);
    }

    private static final BinaryOperator<Throwable> NO_COMBINE = (t1, t2) -> {
        throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
    };

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof ThrowableSpecies) {
            ThrowableSpecies td = (ThrowableSpecies) o;
            return chain.size() == td.chain.size() && IntStream.range(0, chain.size())
                .allMatch(i ->
                    Objects.equals(chain.get(0), td.chain.get(0)));
        }
        return false;
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        chain.forEach(shadow -> shadow.hashTo(hash));
    }
}
