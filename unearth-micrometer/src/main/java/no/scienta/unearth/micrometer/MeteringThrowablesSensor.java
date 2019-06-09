/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import no.scienta.unearth.core.FaultSensor;
import no.scienta.unearth.munch.data.FaultType;
import no.scienta.unearth.munch.data.FaultEvent;
import no.scienta.unearth.munch.data.Fault;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class MeteringThrowablesSensor implements FaultSensor {

    private static final String UNEARTH = "unearth-exceptions";

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
        return metrics.counter(UNEARTH, Collections.singleton(faultTypeTag(faultType)));
    }

    private Counter faultCounter(Fault fault) {
        return metrics.counter(UNEARTH, Collections.singleton(faultTag(fault)));
    }

    private Counter faultCounter(FaultType faultType, Fault fault, FaultEvent faultEvent) {
        return metrics.counter(
            UNEARTH + "-" + faultType.getHash(),
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
