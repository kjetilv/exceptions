package link.stuf.exceptions.core;

import io.micrometer.core.instrument.MeterRegistry;
import link.stuf.exceptions.api.ThrowablesHandler;
import link.stuf.exceptions.core.clearing.MeteringHandler;
import link.stuf.exceptions.core.digest.Packages;
import link.stuf.exceptions.core.digest.SimpleThrowableReducer;

public final class ThrowablesHandlerFactory {

    public static ThrowablesHandler forMeter(MeterRegistry meterRegistry) {
        return new MeteringHandler(
            meterRegistry,
            new SimpleThrowableReducer(
                Packages.all(),
                Packages.none(),
                Packages.none()));
    }

    private ThrowablesHandlerFactory() {
    }
}
