package link.stuf.exceptions.core.digest;

import link.stuf.exceptions.core.hashing.Hashed;
import link.stuf.exceptions.core.hashing.Hasher;

import java.util.UUID;
import java.util.function.Supplier;

abstract class AbstractHashed implements Hashed {

    private final Supplier<UUID> supplier;

    AbstractHashed() {
        supplier = Hasher.uuid(this);
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj.getClass() == getClass() && ((AbstractHashed)obj).getId().equals(getId());
    }

    @Override
    public final UUID getId() {
        return supplier.get();
    }
}
