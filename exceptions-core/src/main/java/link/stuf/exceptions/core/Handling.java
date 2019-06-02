package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ThrowableSpeciesId;
import link.stuf.exceptions.munch.ThrowableSpecimenId;

public interface Handling {

    ThrowableSpeciesId getSpeciesId();

    ThrowableSpecimenId getSpecimenId();

    boolean isLoggable();

    boolean isNew();
}
