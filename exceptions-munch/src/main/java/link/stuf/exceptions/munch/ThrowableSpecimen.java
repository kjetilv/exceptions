package link.stuf.exceptions.munch;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableSpecimen extends AbstractHashed implements Identified<ThrowableSpecimenId> {

    private final List<String> messages;

    private final ThrowableSpecies species;

    private final Instant time;

    private final Long globalSequence;

    private final Long typeSequence;

    ThrowableSpecimen(List<String> messages, ThrowableSpecies species) {
        this(messages, species, null, null, null);
    }

    public ThrowableSpecies getSpecies() {
        return species;
    }

    public Instant getTime() {
        return time;
    }

    public Long getGlobalSequence() {
        return globalSequence;
    }

    public Long getTypeSequence() {
        return typeSequence;
    }

    public ThrowableSpecimen sequenced(Instant time, Long globalSequence, Long typeSequence) {
        return new ThrowableSpecimen(messages, species,
            Objects.requireNonNull(time),
            Objects.requireNonNull(globalSequence),
            Objects.requireNonNull(typeSequence));
    }

    public ThrowableDto toThrowableDto() {
        return reversedRange(species.stacks())
            .reduce(
                null,
                (cause, i) ->
                    species.stacks().get(i).toExceptionDto(messages.get(i), species.stacks().get(i), cause),
                noCombine());
    }

    public Throwable toThrowable() {
        return reversedRange(species.stacks())
            .reduce(
                null,
                (cause, i) ->
                    species.stacks().get(i).toException(messages.get(i), cause),
                noCombine());
    }

    private Stream<Integer> reversedRange(List<ThrowableStack> stacks) {
        return IntStream.range(0, stacks.size())
            .map(i -> stacks.size() - i - 1)
            .boxed();
    }

    @Override
    public ThrowableSpecimenId getId() {
        return new ThrowableSpecimenId(getHash());
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        species.hashTo(hash);
        hashStrings(hash, messages);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(time.toEpochMilli());
        hash.accept(buffer.array());
    }

    private ThrowableSpecimen(List<String> messages, ThrowableSpecies species,
                              Instant time,
                              Long globalSequence,
                              Long typeSequence) {
        this.messages = Collections.unmodifiableList(messages);
        this.species = Objects.requireNonNull(species);
        this.time = time;
        this.globalSequence = globalSequence;
        this.typeSequence = typeSequence;
        if (species.stacks().size() != messages.size()) {
            throw new IllegalStateException("Expected same arity: " + species.stacks().size() + "/" + messages.size());
        }
    }

    private static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
    }
}

