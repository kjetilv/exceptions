package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ids.FaultTypeId;
import link.stuf.exceptions.munch.data.FaultEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

public interface FaultStats {

    Optional<FaultEvent> lastFaultEvent(FaultTypeId id);

    default long faultEventCount(FaultTypeId id) {
        return faultEventCount(id, null);
    }

    default long faultEventCount(FaultTypeId id, Instant sinceTime) {
        return faultEventCount(id, sinceTime, null);
    }

    long faultEventCount(FaultTypeId id, Instant sinceTime, Duration during);

    default Stream<FaultEvent> faultEvents(FaultTypeId id) {
        return faultEvents(id, null);
    }

    default Stream<FaultEvent> faultEvents(FaultTypeId id, Instant sinceTime) {
        return faultEvents(id, sinceTime, null);
    }

    Stream<FaultEvent> faultEvents(FaultTypeId id, Instant sinceTime, Duration period);
}
