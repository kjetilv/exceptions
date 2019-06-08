package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ThrowableSpeciesId;
import link.stuf.exceptions.munch.ThrowableSpecimen;
import link.stuf.exceptions.munch.ThrowableSubspeciesId;

import java.util.List;

public interface ThrowablesFeed {

    long limit(ThrowableSpeciesId id);

    long limit(ThrowableSubspeciesId id);

    List<ThrowableSpecimen> feed(ThrowableSpeciesId id, long offset, int count);

    List<ThrowableSpecimen> feed(ThrowableSubspeciesId id, long offset, int count);
}
