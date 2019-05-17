package link.stuf.exceptions.core;

import io.micrometer.core.instrument.MeterRegistry;
import link.stuf.exceptions.clearing.MeteringClearingHouse;

public final class ClearingHouseFactory {

    public static ThrowablesClearingHouse forMeter(MeterRegistry meterRegistry) {
        return new MeteringClearingHouse(meterRegistry);
    }

    private ClearingHouseFactory() {
    }
}
