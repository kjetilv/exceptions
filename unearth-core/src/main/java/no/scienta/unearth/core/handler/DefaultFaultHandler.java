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
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.model.LogEntry;
import no.scienta.unearth.munch.util.Util;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultFaultHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultStats stats;

    private final FaultSensor sensor;

    private final Clock clock;

    public DefaultFaultHandler(
        FaultStorage storage,
        FaultStats stats,
        FaultSensor sensor,
        Clock clock
    ) {
        this.storage = storage;
        this.stats = stats;
        this.sensor = sensor;
        this.clock = clock;
    }

    @Override
    public HandlingPolicy handle(Throwable throwable, String logMessage, Object... args) {
        return store(
            logMessage == null ? null : LogEntry.create(logMessage, args),
            Fault.create(throwable)
        );
    }

    private HandlingPolicy store(LogEntry logEntry, Fault fault) {
        Optional<FaultEvent> lastStored = stats.getLastFaultEvent(fault.getFaultStrand().getId());
        FaultEvent event = sensor.registered(storage.store(logEntry, fault));

        return new SimpleHandlingPolicy(event)
            .withAction(
                actionForWindow(lastStored.orElse(null))
            ).withSummary(
                event.getFault().getCauses().stream()
                    .map(Cause::getMessage)
                    .collect(Collectors.joining(" <- ")));
    }

    private Action actionForWindow(FaultEvent previousEvent) {
        return previousEvent == null ? Action.LOG
            : actionForWindow(Duration.between(previousEvent.getTime(), clock.instant()));
    }

    private Action actionForWindow(Duration interval) {
        return isIn(interval, DELUGE_WINDOW) ? Action.LOG_ID :
            isIn(interval, SPAM_WINDOW) ? Action.LOG_MESSAGES
                : isIn(interval, QUIET_WINDOW) ? Action.LOG_SHORT
                : Action.LOG;
    }

    private boolean isIn(Duration eventWindow, Duration window) {
        return Util.isLongerThan(window, eventWindow);
    }

    private static final Duration QUIET_WINDOW = Duration.ofMinutes(10);

    private static final Duration SPAM_WINDOW = Duration.ofMinutes(1);

    private static final Duration DELUGE_WINDOW = Duration.ofSeconds(1);
}
