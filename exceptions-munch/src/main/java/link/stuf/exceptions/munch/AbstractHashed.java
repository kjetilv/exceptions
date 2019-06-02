package link.stuf.exceptions.munch;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

abstract class AbstractHashed implements Hashed {

    private final Supplier<UUID> supplier;

    AbstractHashed() {
        supplier = Hasher.uuid(this);
    }

    void hashStrings(Consumer<byte[]> hash, String... strings) {
        hashStrings(hash, Arrays.asList(strings));
    }

    void hashStrings(Consumer<byte[]> hash, Iterable<String> strings) {
        strings.forEach(string ->
            hash.accept(string.getBytes(StandardCharsets.UTF_8)));
    }

    void hash(Consumer<byte[]> hash, Hashed... hasheds) {
        hash(hash, Arrays.asList(hasheds));
    }

    void hash(Consumer<byte[]> hash, Iterable<? extends Hashed> hasheds) {
        hasheds.forEach(hashed -> hashed.hashTo(hash));
    }

    @Override
    public final int hashCode() {
        return getHash().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj.getClass() == getClass() && ((AbstractHashed)obj).getHash().equals(getHash());
    }

    @Override
    public final UUID getHash() {
        return supplier.get();
    }
}
