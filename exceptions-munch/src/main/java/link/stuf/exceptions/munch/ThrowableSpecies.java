package link.stuf.exceptions.munch;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
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

    public int stackCount() {
        return stacks.size();
    }

    @Override
    String toStringBody() {
        return "(" +
            stacks.stream().map(Objects::toString).collect(Collectors.joining(" <= ")) +
            ")";
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, stacks);
    }

    @Override
    protected ThrowableSpeciesId id(UUID hash) {
        return new ThrowableSpeciesId(hash);
    }
}
