package link.stuf.exceptions.hashing;

import java.util.UUID;
import java.util.function.Consumer;

public interface Hashed {

    UUID getId();

    void hashTo(Consumer<byte[]> hash);
}
