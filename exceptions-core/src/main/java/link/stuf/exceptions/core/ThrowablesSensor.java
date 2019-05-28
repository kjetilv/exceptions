package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

public interface ThrowablesSensor {

    void registered(ThrowableSpecies species, ThrowableSpecimen specimen);
}
