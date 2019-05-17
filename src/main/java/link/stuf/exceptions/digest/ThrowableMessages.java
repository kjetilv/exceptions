package link.stuf.exceptions.digest;

import link.stuf.exceptions.hashing.AbstractHashed;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableMessages extends AbstractHashed implements Iterable<String> {

    private final List<String> messages;

    public static ThrowableMessages of(Throwable throwable) {
        return new ThrowableMessages(
            Throwables.stream(throwable).map(Throwable::getMessage).collect(Collectors.toList())
        );
    }

    private ThrowableMessages(List<String> messages) {
        this.messages = Collections.unmodifiableList(messages);
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
