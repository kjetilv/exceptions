package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.api.Handling;
import link.stuf.exceptions.core.digest.ThrowablesDigest;

class SimpleHandlingPolicy implements Handling {

    private MeteringHandler meteringClearingHouse;

    private final ThrowablesDigest digest;

    private final ThrowablesDigest reduced;

    private final Throwable throwable;

    private final boolean isNew;

    SimpleHandlingPolicy(
        ThrowablesDigest digest,
        ThrowablesDigest reduced,
        Throwable source,
        boolean isNew
    ) {
        this.digest = digest;
        this.reduced = reduced;
        this.throwable = source;
        this.isNew = isNew;
    }

    @Override
    public java.util.UUID getId() {
        return digest.getId();
    }

    @Override
    public Throwable getSource() {
        return throwable;
    }

    @Override
    public Throwable getReduced() {
        return reduced.toThrowable();
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
