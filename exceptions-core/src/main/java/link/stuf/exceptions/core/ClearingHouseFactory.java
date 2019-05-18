package link.stuf.exceptions.core;

import io.micrometer.core.instrument.MeterRegistry;
import link.stuf.exceptions.core.clearing.MeteringHandler;
import link.stuf.exceptions.api.ThrowablesHandler;

public final class ClearingHouseFactory {

    public static ThrowablesHandler forMeter(MeterRegistry meterRegistry) {
        return new MeteringHandler(meterRegistry);
    }

    private ClearingHouseFactory() {
    }
}
