package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.api.Handling;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;

import java.util.UUID;

class SimpleHandlingPolicy implements Handling {

    private final ThrowableSpecies species;

    private final Throwable throwable;

    private final boolean isNew;

    SimpleHandlingPolicy(
        ThrowableSpecies species,
        Throwable source,
        boolean isNew
    ) {
        this.species = species;
        this.throwable = source;
        this.isNew = isNew;
    }

    @Override
    public UUID getId() {
        return species.getHash();
    }

    @Override
    public Throwable getSource() {
        return throwable;
    }

    @Override
    public boolean isLoggable() {
        return isNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
