package link.stuf.exceptions.core.hashing;

import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractHashed implements Hashed {

    private final Supplier<UUID> supplier;

    public AbstractHashed() {
        supplier = Hasher.uuid(this);
    }

    @Override
    public final UUID getId() {
        return supplier.get();
    }
}
