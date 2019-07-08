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
import no.scienta.unearth.core.HandlingPolicy.Action;
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
            .withPrintout(HandlingPolicy.PrintoutType.MESSAGES_ONLY, fault::getMessages)
            .withPrintout(HandlingPolicy.PrintoutType.FULL, () ->
                fullRenderer.render(fault))
            .withPrintout(HandlingPolicy.PrintoutType.SHORT, () ->
                shortRenderer.render(fault));
        return policy.withAction(actionForWindow(previous));
    }

    private Action actionForWindow(FaultEvent previous) {
        if (previous == null) {
            return Action.LOG;
        }
        Duration eventInterval = Duration.between(previous.getTime(), clock.instant());
        return isIn(eventInterval, DELUGE_WINDOW) ? Action.LOG_ID :
            isIn(eventInterval, SPAM_WINDOW) ? Action.LOG_MESSAGES
                : isIn(eventInterval, QUIET_WINDOW) ? Action.LOG_SHORT
                : Action.LOG;
    }

    private boolean isIn(Duration eventWindow, Duration window) {
        return Util.isLongerThan(window, eventWindow);
    }

    private static final Duration QUIET_WINDOW = Duration.ofMinutes(5);

    private static final Duration SPAM_WINDOW = Duration.ofMinutes(1);

    private static final Duration DELUGE_WINDOW = Duration.ofSeconds(5);
}
