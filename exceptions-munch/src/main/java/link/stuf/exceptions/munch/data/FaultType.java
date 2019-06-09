package link.stuf.exceptions.munch.data;

import link.stuf.exceptions.munch.AbstractHashedIdentified;
import link.stuf.exceptions.munch.ids.FaultTypeId;
import link.stuf.exceptions.munch.util.Streams;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A fault type consists of a list of {@link CauseType cause types}.
 */
public class FaultType extends AbstractHashedIdentified<FaultTypeId> {

    private final List<CauseType> causeTypes;

    public static FaultType create(Throwable throwable) {
        List<Cause> causes = Streams.reverse(
            Streams.causes(throwable).map(Cause::create)
        ).collect(Collectors.toList());
        List<CauseType> causeTypes = causes.stream().map(Cause::getCauseType).collect(Collectors.toList());
        return new FaultType(causeTypes);
    }

    private FaultType(Collection<CauseType> causeTypes) {
        if (Objects.requireNonNull(causeTypes).isEmpty()) {
            throw new IllegalArgumentException("Expected one or more causes");
        }
        this.causeTypes = List.copyOf(causeTypes);
    }

    public List<CauseType> getCauseTypes() {
        return causeTypes;
    }

    int getCauseCount() {
        return causeTypes.size();
    }

    @Override
    protected String toStringBody() {
        return "(" +
            causeTypes.stream().map(Objects::toString).collect(Collectors.joining(" <= ")) +
            ")";
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, causeTypes);
    }

    @Override
    protected FaultTypeId id(UUID hash) {
        return new FaultTypeId(hash);
    }
}
