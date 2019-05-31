package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;
import link.stuf.exceptions.core.throwables.ThrowableSpecimenId;

public interface ThrowablesHandler {

    Handling handle(Throwable throwable);

}
