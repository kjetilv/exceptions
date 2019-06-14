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
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.data.FaultEvent;
import no.scienta.unearth.munch.id.FaultEventId;
import no.scienta.unearth.munch.id.FaultId;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final FaultEvent faultEvent;

    private final boolean isNew;

    SimpleHandlingPolicy(FaultEvent faultEvent, boolean isNew) {
        this.faultEvent = faultEvent;
        this.isNew = isNew;
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
    public boolean isLoggable() {
        return isNew;
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
}
