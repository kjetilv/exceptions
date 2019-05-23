package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.api.Handling;
import link.stuf.exceptions.core.digest.Digest;

class SimpleHandlingPolicy implements Handling {

    private final Digest digest;

    private final Digest reduced;

    private final Throwable throwable;

    private final boolean isNew;

    SimpleHandlingPolicy(
        Digest digest,
        Digest reduced,
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
