package link.stuf.exceptions.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import link.stuf.exceptions.core.FaultSensor;
import link.stuf.exceptions.munch.data.FaultType;
import link.stuf.exceptions.munch.data.FaultEvent;
import link.stuf.exceptions.munch.data.Fault;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class MeteringThrowablesSensor implements FaultSensor {

    private static final String EXCEPTIONS = "exceptions";

    private final MeterRegistry metrics;

    public MeteringThrowablesSensor(MeterRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public FaultEvent registered(FaultEvent faultEvent) {
        Fault fault = faultEvent.getFault();
        faultCounter(fault).count();
        FaultType faultType = fault.getFaultType();
        faultTypeCounter(faultType).count();
        faultCounter(faultType, fault, faultEvent).count();
        return faultEvent;
    }

    private Counter faultTypeCounter(FaultType faultType) {
        return metrics.counter(EXCEPTIONS, Collections.singleton(faultTypeTag(faultType)));
    }

    private Counter faultCounter(Fault fault) {
        return metrics.counter(EXCEPTIONS, Collections.singleton(faultTag(fault)));
    }

    private Counter faultCounter(FaultType faultType, Fault fault, FaultEvent faultEvent) {
        return metrics.counter(
            EXCEPTIONS + "-" + faultType.getHash(),
            Arrays.asList(faultTypeTag(faultType), faultEventTag(faultEvent)));
    }

    private Tag faultTypeTag(FaultType faultType) {
        return faultTypeTag(faultType.getHash());
    }

    private Tag faultTag(Fault fault) {
        return faultTag(fault.getHash());
    }

    private Tag faultTypeTag(UUID hash) {
        return Tag.of(FAULT_TYPE, hash.toString());
    }

    private Tag faultTag(UUID hash) {
        return Tag.of(FAULT, hash.toString());
    }

    private Tag faultEventTag(FaultEvent faultEvent) {
        return Tag.of(FAULT_EVENT, faultEvent.getHash().toString());
    }

    private static final String FAULT_TYPE = "faul-type";

    private static final String FAULT = "fault";

    private static final String FAULT_EVENT = "fault-event";
}
