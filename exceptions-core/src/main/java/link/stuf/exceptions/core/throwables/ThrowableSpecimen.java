package link.stuf.exceptions.core.throwables;

import link.stuf.exceptions.core.hashing.AbstractHashed;
import link.stuf.exceptions.core.id.Identified;
import link.stuf.exceptions.core.id.ThrowableSpecimenId;
import link.stuf.exceptions.core.utils.Streams;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableSpecimen
    extends AbstractHashed
    implements Identified<ThrowableSpecimenId> {

    private final List<String> messages;

    private final ThrowableSpecies species;

    private final Instant time;

    public static ThrowableSpecimen create(Throwable throwable, ThrowableSpecies digest, Instant time) {
        return new ThrowableSpecimen(messages(throwable), digest, time);
    }

    private static List<String> messages(Throwable throwable) {
        return Streams.causes(throwable).map(Throwable::getMessage).collect(Collectors.toList());
    }

    public ThrowableSpecies getSpecies() {
        return species;
    }

    public Instant getTime() {
        return time;
    }

    public Throwable toThrowable() {
        List<ShadowThrowable> chain = species.chain();
        if (chain.size() != messages.size()) {
            throw new IllegalStateException("Expected same arity: " + chain.size() + "/" + messages.size());
        }
        return Streams.reverseRange(0, chain.size()).boxed().reduce(
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

    private ThrowableSpecimen(List<String> messages, ThrowableSpecies species, Instant time) {
        this.messages = Collections.unmodifiableList(messages);
        this.species = Objects.requireNonNull(species);
        this.time = Objects.requireNonNull(time);
    }

    private static final BinaryOperator<Throwable> NO_COMBINE = (t1, t2) -> {
        throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
    };
}

