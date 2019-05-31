package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ThrowableSpeciesId;
import link.stuf.exceptions.core.throwables.ThrowableSpecimenId;

public interface Handling {

    ThrowableSpeciesId getSpeciesId();

    ThrowableSpecimenId getSpecimenId();

    boolean isLoggable();

    boolean isNew();
}
