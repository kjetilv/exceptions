package link.stuf.exceptions.munch;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThrowableSpecies extends AbstractHashedIdentified<ThrowableSpeciesId> {

    private final List<ThrowableStack> stacks;

    ThrowableSpecies(Collection<ThrowableStack> stacks) {
        this.stacks = Collections.unmodifiableList(stacks instanceof List<?>
            ? (List<ThrowableStack>) stacks
            : new ArrayList<>(stacks));
    }

    public List<ThrowableStack> stacks() {
        return stacks;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getId() + " <" +
            stacks.stream().map(Objects::toString).collect(Collectors.joining(" ")) +
            ">]";
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        hash(hash, stacks);
    }

    @Override
    protected ThrowableSpeciesId id(UUID hash) {
        return new ThrowableSpeciesId(hash);
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
