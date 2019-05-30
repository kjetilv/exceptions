package link.stuf.exceptions.core;

import link.stuf.exceptions.core.id.ThrowableSpeciesId;
import link.stuf.exceptions.core.id.ThrowableSpecimenId;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.util.Collection;
import java.util.Optional;

public interface ThrowablesStorage {

    ThrowableSpecies store(ThrowableSpecimen specimen);

    Optional<ThrowableSpecies> getSpecies(ThrowableSpeciesId digestId);

    Optional<ThrowableSpecimen> getSpecimen(ThrowableSpecimenId specimenId);

    Collection<ThrowableSpecimen> getSpecimen(ThrowableSpeciesId speciesId);
}
