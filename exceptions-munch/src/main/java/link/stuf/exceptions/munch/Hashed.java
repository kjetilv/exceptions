package link.stuf.exceptions.munch;

import java.util.UUID;
import java.util.function.Consumer;

public interface Hashed {

    UUID getHash();

    void hashTo(Consumer<byte[]> hash);
}
