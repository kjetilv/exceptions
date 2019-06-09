package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.data.FaultEvent;

public interface FaultSensor {

    FaultEvent registered(FaultEvent specimen);
}
