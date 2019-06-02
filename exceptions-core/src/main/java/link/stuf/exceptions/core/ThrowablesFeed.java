package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ThrowableSpeciesId;
import link.stuf.exceptions.munch.ThrowableSpecimen;

import java.util.List;

public interface ThrowablesFeed {

    long limit(ThrowableSpeciesId id);

    List<ThrowableSpecimen> feed(ThrowableSpeciesId id, long offset, int count);
}
