package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.HandlingPolicy;
import link.stuf.exceptions.munch.ThrowableSpeciesId;
import link.stuf.exceptions.munch.ThrowableSpecimen;
import link.stuf.exceptions.munch.ThrowableSpecimenId;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final ThrowableSpecimen specimen;

    private final boolean isNew;

    SimpleHandlingPolicy(ThrowableSpecimen specimen, boolean isNew) {
        this.specimen = specimen;
        this.isNew = isNew;
    }

    @Override
    public ThrowableSpeciesId getSpeciesId() {
        return specimen.getSpecies().getId();
    }

    @Override
    public ThrowableSpecimenId getSpecimenId() {
        return specimen.getId();
    }

    @Override
    public boolean isLoggable() {
        return isNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
