package link.stuf.exceptions.munch.data;

import link.stuf.exceptions.munch.AbstractHashedIdentified;
import link.stuf.exceptions.munch.dto.ThrowableDto;
import link.stuf.exceptions.munch.ids.FaultId;
import link.stuf.exceptions.munch.util.Streams;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Fault extends AbstractHashedIdentified<FaultId> {

    public static Fault create(Throwable throwable) {
        return new Fault(FaultType.create(throwable), causes(throwable));
    }

    private final FaultType faultType;

    private final List<Cause> causes;

    private Fault(FaultType faultType, Collection<Cause> causes) {
        this.faultType = Objects.requireNonNull(faultType);
        this.causes = causes == null || causes.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(causes);
        if (this.faultType.getCauseCount() != this.causes.size()) {
            throw new IllegalStateException(
                "Expected same arity: " + this.faultType.getCauseTypes().size() + "/" + this.causes.size());
        }
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public List<Cause> getCauses() {
        return causes;
    }

    public ThrowableDto toThrowableDto() {
        return Streams.reverse(causes)
            .reduce(null, (dto, cause) -> cause.toThrowableDto(dto), noCombine());
    }

    public Throwable toThrowable() {
        return Streams.reverse(causes)
            .reduce(null,
                (t, cause) ->
                    cause.toThrowable(t),
                noCombine());
    }

    private Stream<Integer> reversedRange(List<?> elements) {
        return IntStream.range(0, elements.size())
            .map(i -> elements.size() - i - 1)
            .boxed();
    }

    private static List<Cause> causes(Throwable throwable) {
        return Streams.causes(throwable).map(Cause::create).collect(Collectors.toList());
    }

    private static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
    }

    @Override
    protected FaultId id(UUID hash) {
        return new FaultId(hash);
    }

    @Override
    protected String toStringBody() {
        return "faultType:" + faultType + ": " + causes.size();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, faultType);
        hashHashables(h, causes);
    }
}
