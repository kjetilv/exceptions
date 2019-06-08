package link.stuf.exceptions.munch;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableSpecimen extends AbstractHashedIdentified<ThrowableSpecimenId> {

    private final ThrowableSubspecies subspecies;

    private final Instant time;

    private final Long globalSequence;

    private final Long speciesSequence;

    private final Long subspeciesSequence;

    ThrowableSpecimen(ThrowableSubspecies subspecies) {
        this(subspecies, null, null, null, null);
    }

    private ThrowableSpecimen(
        ThrowableSubspecies species,
        Instant time,
        Long globalSequence,
        Long speciesSequence,
        Long subspeciesSequence
    ) {
        this.subspecies = Objects.requireNonNull(species);
        this.time = time;
        this.globalSequence = globalSequence;
        this.speciesSequence = speciesSequence;
        this.subspeciesSequence = subspeciesSequence;
    }

    public ThrowableSubspecies getSubspecies() {
        return subspecies;
    }

    public Instant getTime() {
        return time;
    }

    public Long getGlobalSequence() {
        return globalSequence;
    }

    public Long getSpeciesSequence() {
        return speciesSequence;
    }

    public Long getSubspeciesSequence() {
        return subspeciesSequence;
    }

    public ThrowableSpecimen sequenced(
        Instant time,
        Long globalSequence,
        Long speciesSequence,
        Long subspeciesSequence
    ) {
        if (this.time != null) {
            throw new IllegalStateException(this + " already sequenced");
        }
        return new ThrowableSpecimen(subspecies,
            Objects.requireNonNull(time),
            Objects.requireNonNull(globalSequence),
            Objects.requireNonNull(speciesSequence),
            Objects.requireNonNull(subspeciesSequence));
    }

    @Override
    String toStringBody() {
        String time = this.time == null
            ? "<>"
            : this.time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return "@" + time + " g:" + globalSequence + " s:" + speciesSequence + " ss:" + subspeciesSequence;
    }

    public ThrowableDto toThrowableDto() {
        return reversedRange(subspecies.getSpecies().stacks())
            .reduce(
                null,
                (cause, i) ->
                    subspecies.getSpecies().stacks().get(i).toExceptionDto(
                        subspecies.getMessages().get(i),
                        subspecies.getSpecies().stacks().get(i),
                        cause),
                noCombine());
    }

    public Throwable toThrowable() {
        return reversedRange(subspecies.getSpecies().stacks())
            .reduce(
                null,
                (cause, i) ->
                    subspecies.getSpecies().stacks().get(i).toException(
                        subspecies.getMessages().get(i), cause),
                noCombine());
    }

    private Stream<Integer> reversedRange(List<ThrowableStack> stacks) {
        return IntStream.range(0, stacks.size())
            .map(i -> stacks.size() - i - 1)
            .boxed();
    }

    @Override
    protected ThrowableSpecimenId id(UUID hash) {
        return new ThrowableSpecimenId(hash);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, subspecies);
        hashLongs(h, time.toEpochMilli());
    }

    private static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
    }
}

