package link.stuf.exceptions.core.throwables;

import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractHashed implements Hashed {

    private final Supplier<UUID> supplier;

    protected AbstractHashed() {
        supplier = Hasher.uuid(this);
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
