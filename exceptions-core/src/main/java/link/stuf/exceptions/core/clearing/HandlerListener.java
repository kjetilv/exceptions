package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.core.digest.ThrowableDigest;
import link.stuf.exceptions.core.digest.ThrowableOccurrence;

public interface HandlerListener {

    void handled(ThrowableDigest chain, ThrowableOccurrence occurrence, Throwable source);
}
