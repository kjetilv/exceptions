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
import no.scienta.unearth.munch.data.FaultStrand;
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
        FaultStrand faultStrand = fault.getFaultStrand();
        faultStrandCounter(faultStrand).count();
        faultCounter(faultStrand, fault, faultEvent).count();
        return faultEvent;
    }

    private Counter faultStrandCounter(FaultStrand faultStrand) {
        return metrics.counter(UNEARTH, Collections.singleton(faultStrandTag(faultStrand)));
    }

    private Counter faultCounter(Fault fault) {
        return metrics.counter(UNEARTH, Collections.singleton(faultTag(fault)));
    }

    private Counter faultCounter(FaultStrand faultStrand, Fault fault, FaultEvent faultEvent) {
        return metrics.counter(
            UNEARTH + "-" + faultStrand.getHash(),
            Arrays.asList(faultStrandTag(faultStrand), faultEventTag(faultEvent)));
    }

    private Tag faultStrandTag(FaultStrand faultStrand) {
        return faultStrandTag(faultStrand.getHash());
    }

    private Tag faultTag(Fault fault) {
        return faultTag(fault.getHash());
    }

    private Tag faultStrandTag(UUID hash) {
        return Tag.of(FAULT_STRAND, hash.toString());
    }

    private Tag faultTag(UUID hash) {
        return Tag.of(FAULT, hash.toString());
    }

    private Tag faultEventTag(FaultEvent faultEvent) {
        return Tag.of(FAULT_EVENT, faultEvent.getHash().toString());
    }

    private static final String FAULT_STRAND = "faul-type";

    private static final String FAULT = "fault";

    private static final String FAULT_EVENT = "fault-event";
}
