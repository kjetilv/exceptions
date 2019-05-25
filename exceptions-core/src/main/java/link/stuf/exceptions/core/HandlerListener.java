package link.stuf.exceptions.core;

import link.stuf.exceptions.core.digest.ThrowableSpecies;
import link.stuf.exceptions.core.digest.ThrowableSpecimen;

public interface HandlerListener {

    void handled(ThrowableSpecies chain, ThrowableSpecimen occurrence, Throwable source);
}
