package link.stuf.exceptions.core.hashing;

import java.util.UUID;
import java.util.function.Consumer;

public interface Hashed {

    UUID getId();

    void hashTo(Consumer<byte[]> hash);
}
