package link.stuf.exceptions.core.digest;

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

    private final List<ShadowThrowable> shadows;

    private ThrowableSpecies(List<ShadowThrowable> shadows) {
        this.shadows = Collections.unmodifiableList(shadows);
    }

    public ThrowableSpecies map(UnaryOperator<ShadowThrowable> mapper) {
        return new ThrowableSpecies(shadows.stream().map(mapper).collect(Collectors.toList()));
    }

    public Throwable toThrowable() {
        return Streams.reverse(shadows).reduce(
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
            return shadows.size() == td.shadows.size() && IntStream.range(0, shadows.size())
                .allMatch(i ->
                    Objects.equals(shadows.get(0), td.shadows.get(0)));
        }
        return false;
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        shadows.forEach(shadow -> shadow.hashTo(hash));
    }
}
