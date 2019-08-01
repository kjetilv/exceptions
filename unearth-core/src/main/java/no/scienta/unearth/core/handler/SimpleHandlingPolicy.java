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
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.core.handler;

import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.munch.model.CauseChain;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.model.FaultStrand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final String summary;

    private final FaultEvent faultEvent;

    private final Action action;

    private final Map<Action, Supplier<CauseChain>> causeChains;

    SimpleHandlingPolicy(FaultEvent faultEvent) {
        this(null, faultEvent, null, null);
    }

    private SimpleHandlingPolicy(
        String summary,
        FaultEvent faultEvent,
        Action action,
        Map<Action, Supplier<CauseChain>> causeChains
    ) {
        this.summary = summary;
        this.faultEvent = faultEvent;
        this.action = action;
        this.causeChains = causeChains == null || causeChains.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(causeChains));
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public FaultStrand getFaultStrand() {
        return faultEvent.getFault().getFaultStrand();
    }

    @Override
    public Fault getFault() {
        return faultEvent.getFault();
    }

    @Override
    public FaultEvent getFaultEvent() {
        return faultEvent;
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

    SimpleHandlingPolicy withSummary(String summary) {
        return new SimpleHandlingPolicy(summary, faultEvent, action, causeChains);
    }

    SimpleHandlingPolicy withAction(Action action) {
        return new SimpleHandlingPolicy(summary, faultEvent, action, causeChains);
    }
}
