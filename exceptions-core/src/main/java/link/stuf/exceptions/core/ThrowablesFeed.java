package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ThrowableSpeciesId;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.util.List;

public interface ThrowablesFeed {

    long limit(ThrowableSpeciesId id);

    List<ThrowableSpecimen> feed(ThrowableSpeciesId id, long offset, int count);
}
