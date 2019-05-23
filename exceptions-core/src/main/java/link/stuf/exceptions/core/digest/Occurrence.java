package link.stuf.exceptions.core.digest;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Occurrence extends AbstractHashed implements Iterable<String> {

    private final List<String> messages;

    private final Digest digest;

    private final Instant time;

    public static Occurrence create(Throwable throwable, Digest digest, Instant time) {
        return new Occurrence(
            Throwables.stream(throwable).map(Throwable::getMessage).collect(Collectors.toList()),
            digest,
            time);
    }

    public Instant getTime() {
        return time;
    }

    private Occurrence(List<String> messages, Digest digest, Instant time) {
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
    }
}
