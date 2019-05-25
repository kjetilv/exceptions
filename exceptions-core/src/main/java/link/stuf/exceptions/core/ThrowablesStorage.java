package link.stuf.exceptions.core;

import link.stuf.exceptions.core.digest.ThrowableSpecies;
import link.stuf.exceptions.core.digest.ThrowableSpecimen;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ThrowablesStorage {

    ThrowableSpecies store(ThrowableSpecies digest, ThrowableSpecimen occurrence);

    Optional<ThrowableSpecies> getDigest(UUID digestId);

    Optional<ThrowableSpecimen> getOccurrence(UUID occurrenceId);

    Collection<ThrowableSpecimen> getOccurrences(UUID digestId);
}
