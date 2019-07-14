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
import no.scienta.unearth.munch.model.CauseChain;
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.print.Rendering;
import no.scienta.unearth.munch.util.Memoizer;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final String summary;

    private final FaultEvent faultEvent;

    private final Action action;

    private final Map<PrintoutType, Supplier<CauseChain>> printouts;

    SimpleHandlingPolicy(FaultEvent faultEvent) {
        this(null, faultEvent, null, null);
    }

    private SimpleHandlingPolicy(
        String summary,
        FaultEvent faultEvent,
        Action action,
        Map<PrintoutType, Supplier<CauseChain>> printouts
    ) {
        this.summary = summary;
        this.faultEvent = faultEvent;
        this.action = action;
        this.printouts = printouts == null || printouts.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(printouts));
    }

    @Override
    public String getSummary() {
        return summary;
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
    public Optional<CauseChain> getPrintout(PrintoutType type) {
        return Optional.ofNullable(printouts.get(type)).map(Supplier::get);
    }

    @Override
    public Action getAction() {
        return action;
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

    @Override
    public Collection<String> getThrowableRendering(PrintoutType type) {
        Optional<CauseChain> causeChain = Optional.ofNullable(printouts.get(type)).map(Supplier::get);
        Collection<Rendering> renderings =
            causeChain.map(CauseChain::getChainRendering).orElseGet(Collections::emptyList);
        return renderings.stream()
            .map(Rendering::getStrings)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    SimpleHandlingPolicy withPrintout(PrintoutType type, Supplier<CauseChain> printout) {
        Map<PrintoutType, Supplier<CauseChain>> map = new HashMap<>(printouts);
        map.put(type, Memoizer.get(printout));
        return new SimpleHandlingPolicy(summary, faultEvent, action, map);
    }

    SimpleHandlingPolicy withSummary(String summary) {
        return new SimpleHandlingPolicy(summary, faultEvent, action, printouts);
    }

    SimpleHandlingPolicy withAction(Action action) {
        return new SimpleHandlingPolicy(summary, faultEvent, action, printouts);
    }
}
