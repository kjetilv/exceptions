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

import no.scienta.unearth.core.*;
import no.scienta.unearth.core.HandlingPolicy.Action;
import no.scienta.unearth.munch.model.Cause;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FeedEntry;
import no.scienta.unearth.munch.model.LogEntry;

import java.time.Clock;
import java.util.stream.Collectors;

public class DefaultFaultHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultSensor sensor;

    private final FaultStats stats;

    private final Clock clock;

    public DefaultFaultHandler(
        FaultStorage storage,
        FaultSensor sensor,
        FaultStats stats,
        Clock clock
    ) {
        this.storage = storage;
        this.sensor = sensor;
        this.stats = stats;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public HandlingPolicy handle(Throwable throwable, String logMessage, Object... args) {
        HandlingPolicy store = store(
            logMessage == null ? null : LogEntry.create(logMessage, args),
            throwable,
            Fault.create(throwable));
        sensor.register(store.getFeedEntry());
        return store;
    }

    private HandlingPolicy store(
        LogEntry logEntry,
        Throwable throwable,
        Fault fault
    ) {
        FeedEntry entry = storage.store(logEntry, fault, throwable);
        return basePolicy(entry, fault).withAction(Action.LOG);
    }

    private static SimpleHandlingPolicy basePolicy(FeedEntry entry, Fault fault) {
        return new SimpleHandlingPolicy(entry, fault)
            .withSummary(fault.getCauses().stream()
                .map(Cause::getMessage)
                .collect(Collectors.joining(" <- ")));
    }
}
