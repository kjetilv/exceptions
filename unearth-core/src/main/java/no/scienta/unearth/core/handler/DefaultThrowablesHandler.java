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

import no.scienta.unearth.core.FaultHandler;
import no.scienta.unearth.core.FaultSensor;
import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.munch.data.Fault;
import no.scienta.unearth.munch.data.FaultEvent;

public class DefaultThrowablesHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultSensor sensor;

    public DefaultThrowablesHandler(
        FaultStorage storage,
        FaultSensor sensor
    ) {
        this.storage = storage;
        this.sensor = sensor;
    }

    @Override
    public HandlingPolicy handle(Fault fault) {
        return store(fault);
    }

    @Override
    public SimpleHandlingPolicy handle(Throwable throwable) {
        return store(Fault.create(throwable));
    }

    private SimpleHandlingPolicy store(Fault submitted) {
        FaultEvent stored = storage.store(submitted);
        FaultEvent registered = sensor.registered(stored);
        return new SimpleHandlingPolicy(registered, stored.getFaultTypeSequence() == 0);
    }
}
