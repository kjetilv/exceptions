package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;
import link.stuf.exceptions.core.throwables.ThrowableSpecimenId;

import java.util.Collection;

public interface ThrowablesStorage {

    ThrowableSpecimen store(ThrowableSpecimen specimen);

    ThrowableSpecies getSpecies(ThrowableSpeciesId digestId);

    ThrowableSpecimen getSpecimen(ThrowableSpecimenId specimenId);

    Collection<ThrowableSpecimen> getSpecimen(ThrowableSpeciesId speciesId);
}
