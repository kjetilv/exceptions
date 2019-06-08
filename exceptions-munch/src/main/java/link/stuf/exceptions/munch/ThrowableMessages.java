package link.stuf.exceptions.munch;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

public class ThrowableMessages extends AbstractHashedIdentified<ThrowableMessagesId> implements Iterable<String> {
    private final List<String> messages;

    public ThrowableMessages(String... messages) {
        this(Arrays.asList(messages));
    }

    public ThrowableMessages(Collection<String> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Expected >0 messages");
        }
        this.messages = List.copyOf(messages);
    }

    @Override
    public Iterator<String> iterator() {
        return messages.iterator();
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        messages.stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).forEach(hash::accept);
    }

    @Override
    protected ThrowableMessagesId id(UUID hash) {
        return new ThrowableMessagesId(hash);
    }
}
