package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ThrowableSpeciesId;
import link.stuf.exceptions.munch.ThrowableSpecimen;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface ThrowablesStats {

    Optional<ThrowableSpecimen> lastSpecimen(ThrowableSpeciesId id);

    default long specimentCount(ThrowableSpeciesId id) {
        return specimentCount(id, null);
    }

    default long specimentCount(ThrowableSpeciesId id, Instant sinceTime) {
        return specimenCount(id, sinceTime, null);
    }

    long specimenCount(ThrowableSpeciesId id, Instant sinceTime, Duration during);

    default Stream<ThrowableSpecimen> specimens(ThrowableSpeciesId id) {
        return specimens(id, null);
    }

    default Stream<ThrowableSpecimen> specimens(ThrowableSpeciesId id, Instant sinceTime) {
        return specimens(id, sinceTime, null);
    }

    Stream<ThrowableSpecimen> specimens(ThrowableSpeciesId id, Instant sinceTime, Duration period);
}
