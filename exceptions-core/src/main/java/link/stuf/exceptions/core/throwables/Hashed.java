package link.stuf.exceptions.core.throwables;

import java.util.UUID;
import java.util.function.Consumer;

public interface Hashed {

    UUID getHash();

    void hashTo(Consumer<byte[]> hash);
}
