package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.Handling;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.util.UUID;

class SimpleHandlingPolicy implements Handling {

    private final ThrowableSpecimen specimen;

    private final Throwable throwable;

    private final boolean isNew;

    SimpleHandlingPolicy(
        ThrowableSpecimen specimen,
        Throwable source,
        boolean isNew
    ) {
        this.specimen = specimen;
        this.throwable = source;
        this.isNew = isNew;
    }

    @Override
    public UUID getId() {
        return specimen.getHash();
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
