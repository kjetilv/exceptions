package link.stuf.exceptions.core;

import java.util.UUID;

public abstract class Id {

    private final UUID hash;

    protected Id(UUID hash) {
        this.hash = hash;
    }

    public UUID getHash() {
        return hash;
    }
}
