package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ThrowableSpeciesId;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface ThrowablesStats {

    Optional<ThrowableSpecimen> lastOccurrence(ThrowableSpeciesId id);

    default long occurrenceCount(ThrowableSpeciesId id) {
        return occurrenceCount(id, null);
    }

    default long occurrenceCount(ThrowableSpeciesId id, Instant sinceTime) {
        return occurrenceCount(id, sinceTime, null);
    }

    long occurrenceCount(ThrowableSpeciesId id, Instant sinceTime, Duration during);

    default Stream<ThrowableSpecimen> occurrences(ThrowableSpeciesId id) {
        return occurrences(id, null);
    }

    default Stream<ThrowableSpecimen> occurrences(ThrowableSpeciesId id, Instant sinceTime) {
        return occurrences(id, sinceTime, null);
    }

    Stream<ThrowableSpecimen> occurrences(ThrowableSpeciesId id, Instant sinceTime, Duration period);
}
