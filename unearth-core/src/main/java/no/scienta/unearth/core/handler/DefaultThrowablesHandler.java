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
import no.scienta.unearth.munch.print.CauseChainRenderer;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

public class DefaultThrowablesHandler implements FaultHandler {

    private final FaultStorage storage;

    private final FaultStats stats;

    private final FaultSensor sensor;

    private final CauseChainRenderer causeChainRenderer;

    private final Clock clock;

    public DefaultThrowablesHandler(
        FaultStorage storage,
        FaultStats stats,
        FaultSensor sensor,
        CauseChainRenderer causeChainRenderer,
        Clock clock
    ) {
        this.storage = storage;
        this.stats = stats;
        this.sensor = sensor;
        this.causeChainRenderer = causeChainRenderer;
        this.clock = clock;
    }

    @Override
    public HandlingPolicy handle(Throwable throwable) {
        return store(Fault.create(throwable));
    }

    private HandlingPolicy store(Fault submitted) {
        FaultEvent stored = storage.store(submitted);
        FaultEvent registered = sensor.registered(stored);
        SimpleHandlingPolicy policy = new SimpleHandlingPolicy(registered);
        Optional<FaultEvent> faultEvent = stats.getLastFaultEvent(stored.getFault().getFaultStrand().getId());
        if (stored.getFaultSequenceNo() == 0 || faultEvent.map(FaultEvent::getTime).filter(time ->
            QUIET_WINDOW.minus(Duration.between(time, clock.instant())).isNegative()).isPresent()) {
            return policy
                .withAction(HandlingPolicy.Action.LOG)
                .withSeverity(HandlingPolicy.Severity.ERROR);
        }
        return policy
            .withAction(HandlingPolicy.Action.LOG_SHORT)
            .withPrintout(HandlingPolicy.PrintoutType.SHORT, toString())
            .withSeverity(HandlingPolicy.Severity.WARNING);
    }

    private static final Duration QUIET_WINDOW = Duration.ofMinutes(1);
}
