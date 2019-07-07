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

import no.scienta.unearth.core.*;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.print.ThrowableRenderer;
import no.scienta.unearth.munch.util.Util;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public class DefaultThrowablesHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultStats stats;

    private final FaultSensor sensor;

    private final ThrowableRenderer fullRenderer;

    private final ThrowableRenderer shortRenderer;

    private final Clock clock;

    public DefaultThrowablesHandler(
        FaultStorage storage,
        FaultStats stats,
        FaultSensor sensor,
        ThrowableRenderer fullRenderer,
        ThrowableRenderer shortRenderer,
        Clock clock
    ) {
        this.storage = storage;
        this.stats = stats;
        this.sensor = sensor;
        this.fullRenderer = fullRenderer;
        this.shortRenderer = shortRenderer;
        this.clock = clock;
    }

    @Override
    public HandlingPolicy handle(Throwable throwable) {
        return store(Fault.create(throwable));
    }

    private HandlingPolicy store(Fault submitted) {
        Optional<FaultEvent> lastStored = stats.getLastFaultEvent(submitted.getFaultStrand().getId());

        FaultEvent stored = storage.store(submitted);
        FaultEvent registered = sensor.registered(stored);

        return policy(lastStored.orElse(null), registered);
    }

    private HandlingPolicy policy(FaultEvent previous, FaultEvent registered) {
        Fault fault = registered.getFault();
        SimpleHandlingPolicy policy = new SimpleHandlingPolicy(registered)
            .withPrintout(HandlingPolicy.PrintoutType.MESSAGES_ONLY,
                fault.getMessages())
            .withPrintout(HandlingPolicy.PrintoutType.FULL,
                fullRenderer.render(fault))
            .withPrintout(HandlingPolicy.PrintoutType.SHORT,
                shortRenderer.render(fault));
        if (loggedInQuietWindow(previous)) {
            return policy
                .withAction(HandlingPolicy.Action.LOG_SHORT)
                .withSeverity(HandlingPolicy.Severity.WARNING);
        }
        return policy
            .withAction(HandlingPolicy.Action.LOG)
            .withSeverity(HandlingPolicy.Severity.ERROR);
    }

    private boolean loggedInQuietWindow(FaultEvent lastStored) {
        if (lastStored == null) {
            return false;
        }
        Duration timeSinceLastOccurrence = Duration.between(lastStored.getTime(), clock.instant());
        return Util.isLongerThan(QUIET_WINDOW, timeSinceLastOccurrence);
    }

    private static final Duration QUIET_WINDOW = Duration.ofMinutes(1);
}
