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

package no.scienta.unearth.core.handler;

import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.munch.id.FaultEventId;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.model.FaultEvent;

import java.util.*;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final FaultEvent faultEvent;

    private final Action action;

    private final Severity severity;

    private final Map<PrintoutType, List<String>> printouts;

    SimpleHandlingPolicy(FaultEvent faultEvent) {
        this(faultEvent, null, null, null);
    }

    private SimpleHandlingPolicy(
        FaultEvent faultEvent,
        Action action,
        Severity severity,
        Map<PrintoutType, List<String>> printouts
    ) {
        this.faultEvent = faultEvent;
        this.action = action;
        this.severity = severity == null ? Severity.DEBUG : severity;
        this.printouts = printouts == null || printouts.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(printouts));
    }

    @Override
    public FaultStrandId getFaultStrandId() {
        return faultEvent.getFault().getFaultStrand().getId();
    }

    public FaultId getFaultId() {
        return faultEvent.getFault().getId();
    }

    @Override
    public FaultEventId getFaultEventId() {
        return faultEvent.getId();
    }

    @Override
    public List<String> getPrintout(PrintoutType type) {
        return Optional.ofNullable(printouts.get(type)).orElseGet(Collections::emptyList);
    }

    @Override
    public Action getAction() {
        return action;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public long getGlobalSequence() {
        return faultEvent.getGlobalSequenceNo();
    }

    @Override
    public long getFaultSequence() {
        return faultEvent.getFaultSequenceNo();
    }

    @Override
    public long getFaultStrandSequence() {
        return faultEvent.getFaultStrandSequenceNo();
    }

    SimpleHandlingPolicy
    withPrintout(PrintoutType type, List<String> printout) {
        Map<PrintoutType, List<String>> map = new HashMap<>(printouts);
        map.put(type, printout);
        return new SimpleHandlingPolicy(faultEvent, action, severity, map);
    }

    SimpleHandlingPolicy withAction(Action action) {
        return new SimpleHandlingPolicy(faultEvent, action, severity, printouts);
    }

    SimpleHandlingPolicy withSeverity(Severity severity) {
        return new SimpleHandlingPolicy(faultEvent, action, severity, printouts);
    }
}
