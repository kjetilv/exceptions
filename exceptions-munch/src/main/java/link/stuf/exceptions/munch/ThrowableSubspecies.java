package link.stuf.exceptions.munch;

import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
public class ThrowableSubspecies extends AbstractHashedIdentified<ThrowableSubspeciesId> {

    private final ThrowableSpecies species;

    private final ThrowableMessages messages;

    public ThrowableSubspecies(ThrowableSpecies species, ThrowableMessages messages) {
        this.species = species;
        this.messages = messages;
        if (species.stackCount() != messages.count()) {
            throw new IllegalStateException("Expected same arity: " + species.stacks().size() + "/" + messages.count());
        }
    }

    public ThrowableSpecies getSpecies() {
        return species;
    }

    public ThrowableMessages getMessages() {
        return messages;
    }

    @Override
    protected ThrowableSubspeciesId id(UUID hash) {
        return new ThrowableSubspeciesId(hash);
    }

    @Override
    String toStringBody() {
        return "species:" + species + ": " + messages;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, species, messages);
    }
}
