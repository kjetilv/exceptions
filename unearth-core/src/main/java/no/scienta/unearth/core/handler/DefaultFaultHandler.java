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

import no.scienta.unearth.core.FaultHandler;
import no.scienta.unearth.core.FaultStats;
import no.scienta.unearth.core.FaultStorage;
import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.core.HandlingPolicy.Action;
import no.scienta.unearth.munch.model.*;
import no.scienta.unearth.util.Util;

import java.time.Clock;
import java.time.Duration;
import java.util.stream.Collectors;

public class DefaultFaultHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultStats stats;

    private final Clock clock;

    public DefaultFaultHandler(
        FaultStorage storage,
        FaultStats stats,
        Clock clock
    ) {
        this.storage = storage;
        this.stats = stats;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public HandlingPolicy handle(Throwable throwable, String logMessage, Object... args) {
        return store(
            logMessage == null ? null : LogEntry.create(logMessage, args),
            throwable,
            Fault.create(throwable));
    }

    private HandlingPolicy store(
        LogEntry logEntry,
        Throwable throwable,
        Fault fault
    ) {
        FaultEvents events = storage.store(logEntry, fault, throwable);
        return basePolicy(events).withAction(loggingAction(events));
    }

    private Action loggingAction(FaultEvents events) {
        return events.getPrevious()
            .map(this::actionForWindow)
            .orElse(Action.LOG);
    }

    private SimpleHandlingPolicy basePolicy(FaultEvents events) {
        return new SimpleHandlingPolicy(events.getEvent())
            .withSummary(
                events.getEvent().getFault().getCauses().stream()
                    .map(Cause::getMessage)
                    .collect(Collectors.joining(" <- ")));
    }

    private Action actionForWindow(FaultEvent previousEvent) {
        return previousEvent == null ? Action.LOG
            : actionForWindow(Duration.between(previousEvent.getTime(), clock.instant()));
    }

    private static Action actionForWindow(Duration interval) {
        return isIn(interval, DELUGE_WINDOW) ? Action.LOG_ID :
            isIn(interval, SPAM_WINDOW) ? Action.LOG_MESSAGES
                : isIn(interval, QUIET_WINDOW) ? Action.LOG_SHORT
                : Action.LOG;
    }

    private static boolean isIn(Duration eventWindow, Duration window) {
        return Util.isLongerThan(window, eventWindow);
    }

    private static final Duration QUIET_WINDOW = Duration.ofMinutes(10);

    private static final Duration SPAM_WINDOW = Duration.ofMinutes(1);

    private static final Duration DELUGE_WINDOW = Duration.ofSeconds(1);
}
