package link.stuf.exceptions.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import link.stuf.exceptions.core.ThrowablesSensor;
import link.stuf.exceptions.munch.ThrowableSpecies;
import link.stuf.exceptions.munch.ThrowableSpecimen;
import link.stuf.exceptions.munch.ThrowableSubspecies;

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
    public ThrowableSpecimen registered(ThrowableSpecimen specimen) {
        ThrowableSubspecies subspecies = specimen.getSubspecies();
        subspeciesCounter(subspecies).count();
        ThrowableSpecies species = subspecies.getSpecies();
        speciesCounter(species).count();
        specimenCounter(species, subspecies, specimen).count();
        return specimen;
    }

    private Counter speciesCounter(ThrowableSpecies species) {
        return metrics.counter(EXCEPTIONS, Collections.singleton(speciesTag(species)));
    }

    private Counter subspeciesCounter(ThrowableSubspecies species) {
        return metrics.counter(EXCEPTIONS, Collections.singleton(subspeciesTag(species)));
    }

    private Counter specimenCounter(ThrowableSpecies species, ThrowableSubspecies subspecies, ThrowableSpecimen specimen) {
        return metrics.counter(
            EXCEPTIONS + "-" + species.getHash(),
            Arrays.asList(speciesTag(species), specimenTag(specimen)));
    }

    private Tag speciesTag(ThrowableSpecies exc) {
        return speciesTag(exc.getHash());
    }

    private Tag subspeciesTag(ThrowableSubspecies exc) {
        return subspeciesTag(exc.getHash());
    }

    private Tag speciesTag(UUID hash) {
        return Tag.of(SPECIES, hash.toString());
    }

    private Tag subspeciesTag(UUID hash) {
        return Tag.of(SUBSPECIES, hash.toString());
    }

    private Tag specimenTag(ThrowableSpecimen specimen) {
        return Tag.of(SPECIMEN, specimen.getHash().toString());
    }

    private static final String SPECIES = "species";

    private static final String SUBSPECIES = "subspecies";

    private static final String SPECIMEN = "specimen";
}
