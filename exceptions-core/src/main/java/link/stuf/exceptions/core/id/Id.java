package link.stuf.exceptions.core.id;

import java.util.Objects;
import java.util.UUID;

public abstract class Id {

    private final UUID hash;

    Id(UUID hash) {
        this.hash = hash;
    }

    public UUID getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Id && Objects.equals(hash, ((Id) o).hash);
    }

    @Override
    public int hashCode() {
        return 3 + 31 * hash.hashCode();
    }

    @Override
    public String toString() {
        return "{" + hash + "}";
    }
}
