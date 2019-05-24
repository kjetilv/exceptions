package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.core.digest.ThrowableDigest;
import link.stuf.exceptions.core.digest.ThrowableOccurrence;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ThrowablesStorage {

    ThrowableDigest store(ThrowableDigest digest, ThrowableOccurrence occurrence);

    Optional<ThrowableDigest> getDigest(UUID digestId);

    Optional<ThrowableOccurrence> getOccurrence(UUID occurrenceId);

    Collection<ThrowableOccurrence> getOccurrences(UUID digestId);
}
