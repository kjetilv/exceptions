package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.*;

import java.util.Collection;

public interface ThrowablesStorage {

    ThrowableSpecimen store(ThrowableSpecimen specimen);

    ThrowableSpecies getSpecies(ThrowableSpeciesId digestId);

    ThrowableSpecimen getSpecimen(ThrowableSpecimenId specimenId);

    ThrowableStack getStack(ThrowableStackId stackId);

    Collection<ThrowableSpecimen> getSpecimen(ThrowableSpeciesId speciesId);
}
