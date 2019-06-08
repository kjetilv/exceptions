package link.stuf.exceptions.munch;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class ThrowableSubspecies extends AbstractHashedIdentified<ThrowableSubpeciesId> {

    private final ThrowableSpecies species;

    private final ThrowableMessages messages;

    public ThrowableSubspecies(ThrowableSpecies species, ThrowableMessages messages) {
        this.species = species;
        this.messages = messages;
    }

    @Override
    protected ThrowableSubpeciesId id(UUID hash) {
        return new ThrowableSubpeciesId(hash);
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        species.hashTo(hash);
        messages.hashTo(hash);
    }
}
