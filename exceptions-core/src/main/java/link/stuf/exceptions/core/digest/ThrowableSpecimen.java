package link.stuf.exceptions.core.digest;

import link.stuf.exceptions.core.hashing.AbstractHashed;
import link.stuf.exceptions.core.utils.Streams;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableSpecimen extends AbstractHashed implements Iterable<String> {

    private final List<String> messages;

    private final ThrowableSpecies species;

    private final Instant time;

    public static ThrowableSpecimen create(Throwable throwable, ThrowableSpecies digest, Instant time) {
        return new ThrowableSpecimen(messages(throwable), digest, time);
    }

    private static List<String> messages(Throwable throwable) {
        return Streams.causes(throwable).map(Throwable::getMessage).collect(Collectors.toList());
    }

    public Instant getTime() {
        return time;
    }

    private ThrowableSpecimen(List<String> messages, ThrowableSpecies species, Instant time) {
        this.messages = Collections.unmodifiableList(messages);
        this.species = species;
        this.time = time;
    }

    @Override
    public Iterator<String> iterator() {
        return messages.iterator();
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        species.hashTo(hash);
        messages.stream().map(String::getBytes).forEach(hash);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(time.toEpochMilli());
        hash.accept(buffer.array());
    }
}
