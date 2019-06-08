package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ThrowableSpeciesId;
import link.stuf.exceptions.munch.ThrowableSpecimenId;
import link.stuf.exceptions.munch.ThrowableSubspeciesId;

public interface HandlingPolicy {

    ThrowableSpeciesId getSpeciesId();

    ThrowableSubspeciesId getSubspeciesId();

    ThrowableSpecimenId getSpecimenId();

    boolean isLoggable();

    boolean isNew();
}
