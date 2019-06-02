package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.*;

import java.util.Collection;
import java.util.UUID;

public interface ThrowablesStorage {

    ThrowableSpecimen store(ThrowableSpecimen specimen);

    ThrowableSpeciesId resolve(UUID id);

    ThrowableSpecies getSpecies(ThrowableSpeciesId digestId);

    Collection<ThrowableSpecimen> getSpecimensOf(ThrowableSpeciesId speciesId);

    ThrowableSpecimen getSpecimen(ThrowableSpecimenId specimenId);

    ThrowableStack getStack(ThrowableStackId stackId);
}
