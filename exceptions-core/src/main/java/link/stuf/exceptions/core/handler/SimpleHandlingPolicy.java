package link.stuf.exceptions.core.handler;

import link.stuf.exceptions.core.Handling;
import link.stuf.exceptions.core.throwables.ThrowableSpeciesId;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;
import link.stuf.exceptions.core.throwables.ThrowableSpecimenId;

class SimpleHandlingPolicy implements Handling {

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
