package link.stuf.exceptions.munch;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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

    public int count() {
        return messages.size();
    }

    public String get(Integer index) {
        if (0 <= index && index < count()) {
            return messages.get(index);
        }
        throw new IllegalStateException(this + " has no message #" + index);
    }

    @Override
    String toStringBody() {
        return messages.stream()
            .map(msg ->
                msg.length() > 10
                    ? msg.substring(0, Math.min(10, msg.length())) + "...[" + msg.length() + "]"
                    : msg)
            .collect(Collectors.joining(", "));
    }

    @Override
    public Iterator<String> iterator() {
        return messages.iterator();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashStrings(h, messages);
    }

    @Override
    protected ThrowableMessagesId id(UUID hash) {
        return new ThrowableMessagesId(hash);
    }
}
