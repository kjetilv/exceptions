package link.stuf.exceptions.core.throwables;

import link.stuf.exceptions.core.hashing.AbstractHashed;
import link.stuf.exceptions.core.id.Identified;
import link.stuf.exceptions.core.id.ThrowableSpeciesId;
import link.stuf.exceptions.core.utils.Streams;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableSpecies
    extends AbstractHashed
    implements Identified<ThrowableSpeciesId> {

    public static ThrowableSpecies create(Throwable throwable) {
        Stream<ShadowThrowable> shadows =
            Streams.causes(throwable).map(ShadowThrowable::create);
        List<ShadowThrowable> causes =
            Streams.reverse(shadows).collect(Collectors.toList());
        return new ThrowableSpecies(causes);
    }

    private final List<ShadowThrowable> chain;

    private final ThrowableSpeciesId id;

    private ThrowableSpecies(List<ShadowThrowable> chain) {
        this.chain = Collections.unmodifiableList(chain);
        this.id = new ThrowableSpeciesId(getHash());
    }

    List<ShadowThrowable> chain() {
        return chain;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + " <" +
            chain.stream().map(Objects::toString).collect(Collectors.joining(" ")) +
            ">]";
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        chain.forEach(shadow -> shadow.hashTo(hash));
    }

    @Override
    public ThrowableSpeciesId getId() {
        return id;
    }

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
}
