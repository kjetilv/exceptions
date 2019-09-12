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

package no.scienta.unearth.turbo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import no.scienta.unearth.core.FaultHandler;
import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.munch.print.CausesRenderer;
import no.scienta.unearth.munch.print.CausesRendering;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import java.util.*;
import java.util.stream.Stream;

public class UnearthlyTurboFilter extends TurboFilter {

    private final FaultHandler faultHandler;

    private final CausesRenderer renderer;

    private final Map<HandlingPolicy.Action, CausesRenderer> renderers;

    public UnearthlyTurboFilter(FaultHandler faultHandler, CausesRenderer renderer) {
        this(faultHandler, renderer, null);
    }

    private UnearthlyTurboFilter(
        FaultHandler faultHandler,
        CausesRenderer renderer,
        Map<HandlingPolicy.Action, CausesRenderer> renderers
    ) {
        this.faultHandler = faultHandler;
        this.renderer = renderer;
        this.renderers = renderers == null || renderers.isEmpty()
            ? Collections.emptyMap()
            : Map.copyOf(renderers);
    }

    @Override
    public FilterReply decide(
        Marker marker,
        Logger logger,
        Level level,
        String format,
        Object[] params,
        Throwable t
    ) {
        if (t == null) {
            return FilterReply.NEUTRAL;
        }
        HandlingPolicy policy = faultHandler.handle(t, format, params);
        ((LocationAwareLogger) logger).log(
            marker,
            ((LocationAwareLogger) logger).getName(),
            level(level),
            message(format, policy),
            allPars(params, policy, rendering(policy)),
            null);
        return FilterReply.DENY;
    }

    private CausesRendering rendering(HandlingPolicy policy) {
        if (policy.getAction() == HandlingPolicy.Action.LOG_ID) {
            return null;
        }
        return renderer(policy.getAction()).render(policy.getFault());
    }

    private CausesRenderer renderer(HandlingPolicy.Action action) {
        return renderers.getOrDefault(action, renderer);
    }

    public UnearthlyTurboFilter withRendererFor(HandlingPolicy.Action action, CausesRenderer renderer) {
        HashMap<HandlingPolicy.Action, CausesRenderer> renderers = new HashMap<>(this.renderers);
        renderers.put(action, renderer);
        return new UnearthlyTurboFilter(faultHandler, renderer, renderers);
    }

    private static Object[] allPars(Object[] params, HandlingPolicy policy, CausesRendering rendering) {
        return allPars(
            params,
            new Object[]{
                policy.getFaultId(),
                policy.getFeedEntryId(),
                rendering == null ? null : String.join("\n", rendering.getStrings("  "))
            });
    }

    private static String message(String format, HandlingPolicy policy) {
        return format + " {} {}" + (policy.getAction() == HandlingPolicy.Action.LOG_ID ? "" : "\n{}");
    }

    private static Object[] allPars(Object[]... params) {
        return Arrays.stream(params)
            .flatMap(UnearthlyTurboFilter::stream)
            .filter(Objects::nonNull)
            .toArray(Object[]::new);
    }

    private static Stream<Object> stream(Object[] params) {
        return params == null ? Stream.empty() : Arrays.stream(params);
    }

    private static int level(Level level) {
        if (Level.INFO.equals(level)) {
            return LocationAwareLogger.INFO_INT;
        }
        if (Level.WARN.equals(level)) {
            return LocationAwareLogger.WARN_INT;
        }
        if (Level.DEBUG.equals(level)) {
            return LocationAwareLogger.DEBUG_INT;
        }
        if (Level.ERROR.equals(level)) {
            return LocationAwareLogger.ERROR_INT;
        }
        return LocationAwareLogger.TRACE_INT;
    }
}
