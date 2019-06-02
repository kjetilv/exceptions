package link.stuf.exceptions.munch;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThrowableSpecies extends AbstractHashed implements Identified<ThrowableSpeciesId> {

    private final List<ThrowableStack> stacks;

    private final ThrowableSpeciesId id;

    ThrowableSpecies(Collection<ThrowableStack> stacks) {
        this.stacks = Collections.unmodifiableList(stacks instanceof List<?>
            ? (List<ThrowableStack>) stacks
            : new ArrayList<>(stacks));
        this.id = new ThrowableSpeciesId(getHash());
    }

    public List<ThrowableStack> stacks() {
        return stacks;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + " <" +
            stacks.stream().map(Objects::toString).collect(Collectors.joining(" ")) +
            ">]";
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        hash(hash, stacks);
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
            return stacks.size() == td.stacks.size() && IntStream.range(0, stacks.size())
                .allMatch(i ->
                    Objects.equals(stacks.get(0), td.stacks.get(0)));
        }
        return false;
    }
}
