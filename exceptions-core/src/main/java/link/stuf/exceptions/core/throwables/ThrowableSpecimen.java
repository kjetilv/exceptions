package link.stuf.exceptions.core.throwables;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class ThrowableSpecimen extends AbstractHashed implements Identified<ThrowableSpecimenId> {

    private final List<String> messages;

    private final ThrowableSpecies species;

    private final Instant time;

    private final Long globalSequence;

    private final Long sequence;

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

    public Long getSequence() {
        return sequence;
    }

    public ThrowableSpecimen sequenced(Instant time, Long globalSequence, Long typeSequence) {
        return new ThrowableSpecimen(messages, species,
            Objects.requireNonNull(time),
            Objects.requireNonNull(globalSequence),
            Objects.requireNonNull(typeSequence));
    }

    public Throwable toThrowable() {
        List<ShadowThrowable> chain = species.chain();
        if (chain.size() != messages.size()) {
            throw new IllegalStateException("Expected same arity: " + chain.size() + "/" + messages.size());
        }
        return IntStream.range(0, chain.size())
            .map(i1 -> chain.size() - i1 - 1)
            .boxed()
            .reduce(
                null,
                (t, i) -> chain.get(i).toException(messages.get(i), t),
                NO_COMBINE);
    }

    @Override
    public ThrowableSpecimenId getId() {
        return new ThrowableSpecimenId(getHash());
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        species.hashTo(hash);
        messages.stream().map(String::getBytes).forEach(hash);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(time.toEpochMilli());
        hash.accept(buffer.array());
    }

    private ThrowableSpecimen(List<String> messages, ThrowableSpecies species,
                              Instant time,
                              Long globalSequence,
                              Long sequence) {
        this.messages = Collections.unmodifiableList(messages);
        this.species = Objects.requireNonNull(species);
        this.time = time;
        this.globalSequence = globalSequence;
        this.sequence = sequence;
    }

    private static final BinaryOperator<Throwable> NO_COMBINE = (t1, t2) -> {
        throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
    };
}

