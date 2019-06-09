package link.stuf.exceptions.munch;

import link.stuf.exceptions.munch.util.Memoizer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractHashed implements Hashed {

    private final Supplier<UUID> supplier = Hasher.uuid(this);

    private final Supplier<String> toString = Memoizer.get(() ->
        getClass().getSimpleName() + "[" + toStringIdentifier() + toStringContents() + "]");

    protected final void hashString(Consumer<byte[]> hash, String string) {
        hashStrings(hash, string);
    }

    protected final void hashStrings(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    protected final void hashStrings(Consumer<byte[]> hash, Iterable<String> strings) {
        strings.forEach(string ->
            hash.accept(string.getBytes(StandardCharsets.UTF_8)));
    }

    protected final void hashHashables(Consumer<byte[]> hash, Hashed... hasheds) {
        hashHashables(hash, Arrays.asList(hasheds));
    }

    protected final void hashHashables(Consumer<byte[]> hash, Iterable<? extends Hashed> hasheds) {
        hasheds.forEach(hashed -> hashed.hashTo(hash));
    }

    protected final void hashLongs(Consumer<byte[]> hash, long... values) {
        for (long value : values) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            hash.accept(buffer.array());
        }
    }

    protected Object toStringIdentifier() {
        return getHash();
    }

    protected String toStringBody() {
        return null;
    }

    private String toStringContents() {
        String body = toStringBody();
        return body == null || body.isBlank() ? "" : " " + body.trim();
    }

    @Override
    public final UUID getHash() {
        return supplier.get();
    }

    @Override
    public final int hashCode() {
        return getHash().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this || obj.getClass() == getClass()
            && ((AbstractHashed)obj).getHash().equals(getHash());
    }

    @Override
    public final String toString() {
        return toString.get();
    }
}
