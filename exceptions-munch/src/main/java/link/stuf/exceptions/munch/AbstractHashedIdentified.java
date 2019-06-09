package link.stuf.exceptions.munch;

import link.stuf.exceptions.munch.util.Memoizer;

import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractHashedIdentified<I extends Id> extends AbstractHashed implements Identified<I> {

    private final Supplier<I> id = Memoizer.get(() -> id(getHash()));

    @Override
    public final I getId() {
        return id.get();
    }

    @Override
    protected final Object toStringIdentifier() {
        return getId();
    }

    protected abstract I id(UUID hash);
}
