package link.stuf.exceptions.munch;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
abstract class AbstractHashed implements Hashed {

    private final Supplier<UUID> supplier = Hasher.uuid(this);

    private final Supplier<String> toString = Memoizer.get(() -> {
        String body = toStringBody();
        String contents = body == null || body.isBlank() ? "" : " " + body.trim();
        return getClass().getSimpleName() + "[" + identifier() + contents + "]";
    });

    final void hashStrings(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    final void hashStrings(Consumer<byte[]> hash, Iterable<String> strings) {
        strings.forEach(string ->
            hash.accept(string.getBytes(StandardCharsets.UTF_8)));
    }

    final void hashHashables(Consumer<byte[]> hash, Hashed... hasheds) {
        hashHashables(hash, Arrays.asList(hasheds));
    }

    final void hashHashables(Consumer<byte[]> hash, Iterable<? extends Hashed> hasheds) {
        hasheds.forEach(hashed -> hashed.hashTo(hash));
    }

    final void hashLongs(Consumer<byte[]> hash, long... values) {
        for (long value : values) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(value);
            hash.accept(buffer.array());
        }
    }

    Object identifier() {
        return getHash();
    }

    String toStringBody() {
        return null;
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
