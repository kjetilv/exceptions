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
import no.scienta.unearth.munch.model.Cause;
import no.scienta.unearth.munch.model.CauseChain;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.model.FaultEvent;
import no.scienta.unearth.munch.print.ThrowableRenderer;
import no.scienta.unearth.munch.util.Util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.scienta.unearth.core.HandlingPolicy.PrintoutType;

public class DefaultThrowablesHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultStats stats;

    private final FaultSensor sensor;

    private final Clock clock;

    private final Map<PrintoutType, ThrowableRenderer> renderers;

    public DefaultThrowablesHandler(
        FaultStorage storage,
        FaultStats stats,
        FaultSensor sensor,
        ThrowableRenderer fullRenderer,
        ThrowableRenderer shortRenderer,
        ThrowableRenderer messagesRenderer, Clock clock
    ) {
        this.storage = storage;
        this.stats = stats;
        this.sensor = sensor;
        this.clock = clock;

        Map<PrintoutType, ThrowableRenderer> renderers = new HashMap<>();
        put(renderers, PrintoutType.FULL, fullRenderer);
        put(renderers, PrintoutType.MESSAGES_ONLY, messagesRenderer);
        put(renderers, PrintoutType.SHORT, shortRenderer);
        this.renderers = Collections.unmodifiableMap(renderers);
    }

    @Override
    public HandlingPolicy handle(Throwable throwable) {
        return store(Fault.create(throwable));
    }

    private HandlingPolicy store(Fault submitted) {
        Optional<FaultEvent> lastStored = stats.getLastFaultEvent(submitted.getFaultStrand().getId());
        FaultEvent stored = storage.store(submitted);
        FaultEvent registered = sensor.registered(stored);
        return policy(registered, lastStored.orElse(null));
    }

    private HandlingPolicy policy(FaultEvent event, FaultEvent previous) {
        CauseChain causeChain = CauseChain.build(event.getFault());
        return renderers.entrySet().stream()
            .reduce(
                new SimpleHandlingPolicy(event),
                (handlingPolicy, e) ->
                    printout(handlingPolicy, e, causeChain),
                Util.noCombine()
            ).withAction(
                actionForWindow(previous)
            ).withSummary(
                event.getFault().getCauses().stream()
                    .map(Cause::getMessage)
                    .collect(Collectors.joining(" <- "))
            );
    }

    private SimpleHandlingPolicy printout(
        SimpleHandlingPolicy hp,
        Entry<PrintoutType, ThrowableRenderer> e, CauseChain causeChain
    ) {
        return hp.withPrintout(e.getKey(), () ->
            causeChain.withStackRendering(e.getValue()));
    }

    private Action actionForWindow(FaultEvent previousEvent) {
        if (previousEvent == null) {
            return Action.LOG;
        }
        Instant eventTime = clock.instant();
        Duration interval = Duration.between(previousEvent.getTime(), eventTime);
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

    private static void put(
        Map<PrintoutType, ThrowableRenderer> renderers,
        PrintoutType type,
        ThrowableRenderer renderer
    ) {
        if (renderer != null) {
            renderers.put(type, renderer);
        }
    }
}
