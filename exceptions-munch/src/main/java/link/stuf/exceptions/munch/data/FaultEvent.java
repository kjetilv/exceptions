package link.stuf.exceptions.munch.data;

import link.stuf.exceptions.munch.AbstractHashedIdentified;
import link.stuf.exceptions.munch.ids.FaultEventId;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class FaultEvent extends AbstractHashedIdentified<FaultEventId> {

    private final Fault fault;

    private final long time;

    private final long globalSequence;

    private final long faultTypeSequence;

    private final long faultSequence;

    FaultEvent(Fault fault) {
        this(fault, null, null, null, null);
    }

    public FaultEvent(
        Fault fault,
        Instant time,
        Long globalSequence,
        Long faultTypeSequence,
        Long faultSequence
    ) {
        this.fault = Objects.requireNonNull(fault);
        this.time = Objects.requireNonNull(time).toEpochMilli();
        this.globalSequence = Objects.requireNonNull(globalSequence);
        this.faultTypeSequence = Objects.requireNonNull(faultTypeSequence);
        this.faultSequence = Objects.requireNonNull(faultSequence);
    }

    public Fault getFault() {
        return fault;
    }

    public Instant getTime() {
        return Instant.ofEpochMilli(time);
    }

    public Long getGlobalSequence() {
        return globalSequence;
    }

    public Long getFaultTypeSequence() {
        return faultTypeSequence;
    }

    public Long getFaultSequence() {
        return faultSequence;
    }

    @Override
    protected FaultEventId id(UUID hash) {
        return new FaultEventId(hash);
    }

    @Override
    protected String toStringBody() {
        String time = getTime().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        return fault.getId() + "@" + time +
            " g#" + globalSequence + " ft#" + faultTypeSequence + " f#" + faultSequence;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, fault);
        hashLongs(h, time, faultSequence, faultTypeSequence, globalSequence);
    }
}

