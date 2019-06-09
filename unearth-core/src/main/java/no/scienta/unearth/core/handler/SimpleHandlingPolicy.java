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
import no.scienta.unearth.munch.ids.FaultTypeId;
import no.scienta.unearth.munch.data.FaultEvent;
import no.scienta.unearth.munch.ids.FaultEventId;
import no.scienta.unearth.munch.ids.FaultId;

class SimpleHandlingPolicy implements HandlingPolicy {

    private final FaultEvent faultEvent;

    private final boolean isNew;

    SimpleHandlingPolicy(FaultEvent faultEvent, boolean isNew) {
        this.faultEvent = faultEvent;
        this.isNew = isNew;
    }

    @Override
    public FaultTypeId getFaultTypeId() {
        return faultEvent.getFault().getFaultType().getId();
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
    public boolean isNew() {
        return isNew;
    }
}
