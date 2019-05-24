package link.stuf.exceptions.core.digest;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableOccurrence extends AbstractHashed implements Iterable<String> {

    private final List<String> messages;

    private final ThrowableDigest digest;

    private final Instant time;

    public static ThrowableOccurrence create(Throwable throwable, ThrowableDigest digest, Instant time) {
        return new ThrowableOccurrence(messages(throwable), digest, time);
    }

    private static List<String> messages(Throwable throwable) {
        return Throwables.stream(throwable).map(Throwable::getMessage).collect(Collectors.toList());
    }

    public Instant getTime() {
        return time;
    }

    private ThrowableOccurrence(List<String> messages, ThrowableDigest digest, Instant time) {
        this.messages = Collections.unmodifiableList(messages);
        this.digest = digest;
        this.time = time;
    }

    @Override
    public Iterator<String> iterator() {
        return messages.iterator();
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        messages.stream().map(String::getBytes).forEach(hash);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(time.toEpochMilli());
        hash.accept(buffer.array());
    }
}
