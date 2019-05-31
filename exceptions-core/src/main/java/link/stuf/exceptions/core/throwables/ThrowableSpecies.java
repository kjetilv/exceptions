package link.stuf.exceptions.core.throwables;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThrowableSpecies extends AbstractHashed implements Identified<ThrowableSpeciesId> {

    private final List<ShadowThrowable> chain;

    private final ThrowableSpeciesId id;

    ThrowableSpecies(List<ShadowThrowable> chain) {
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
