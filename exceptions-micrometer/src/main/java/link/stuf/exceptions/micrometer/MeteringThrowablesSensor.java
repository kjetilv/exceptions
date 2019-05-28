package link.stuf.exceptions.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import link.stuf.exceptions.core.ThrowableSpeciesId;
import link.stuf.exceptions.core.ThrowablesSensor;
import link.stuf.exceptions.core.throwables.ThrowableSpecies;
import link.stuf.exceptions.core.throwables.ThrowableSpecimen;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class MeteringThrowablesSensor implements ThrowablesSensor {

    private static final String EXCEPTIONS = "exceptions";

    private final MeterRegistry metrics;

    public MeteringThrowablesSensor(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public void registered(ThrowableSpecies species, ThrowableSpecimen specimen) {
        count(species);
        count(species, specimen);
    }

    private void count(ThrowableSpecies digest) {
        counter(digest).count();
    }

    private void count(ThrowableSpecies digest, ThrowableSpecimen messages) {
        messageCounter(digest, messages).count();
    }

    private Counter counter(ThrowableSpecies species) {
        return metrics.counter(EXCEPTIONS, Collections.singleton(speciesTag(species)));
    }

    private Counter counter(ThrowableSpeciesId species) {
        return metrics.counter(EXCEPTIONS, Collections.singleton(speciesTag(species.getHash())));
    }

    private Counter messageCounter(ThrowableSpecies species, ThrowableSpecimen specimen) {
        return metrics.counter(
            EXCEPTIONS + "-" + species.getHash(),
            Arrays.asList(speciesTag(species), specimenTag(specimen)));
    }

    private Tag speciesTag(ThrowableSpecies exc) {
        return speciesTag(exc.getHash());
    }

    private Tag speciesTag(UUID hash) {
        return Tag.of(SPECIES, hash.toString());
    }

    private Tag specimenTag(ThrowableSpecimen specimen) {
        return Tag.of(SPECIMEN, specimen.getHash().toString());
    }

    private static final String SPECIES = "species";

    private static final String SPECIMEN = "specimen";
}
