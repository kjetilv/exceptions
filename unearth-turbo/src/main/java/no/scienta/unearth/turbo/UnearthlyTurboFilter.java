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
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

public class UnearthlyTurboFilter extends TurboFilter {

    private final FaultHandler faultHandler;

    private final CausesRenderer renderer;

    public UnearthlyTurboFilter(FaultHandler faultHandler, CausesRenderer renderer) {
        this.faultHandler = faultHandler;
        this.renderer = renderer;
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
        if (params != null && params.length > 0 && params[params.length - 1] instanceof HandlingPolicy) {
            return logPolicy(
                marker, logger, level, format, params, (HandlingPolicy) params[params.length - 1], renderer);
        }
        if (t == null) {
            return FilterReply.NEUTRAL;
        }
        return logPolicy(
            marker, logger, level, format, params, faultHandler.handle(t, format, params), renderer);
    }

    private static FilterReply logPolicy(
        Marker marker,
        LocationAwareLogger logger,
        Level level,
        String format,
        Object[] params,
        HandlingPolicy policy,
        CausesRenderer renderer
    ) {
        Logging.doLog(
            (expandedFormat, expandedParams) ->
                logger.log(marker, logger.getName(), level(level), expandedFormat, expandedParams, null),
            policy,
            renderer.render(policy.getFault()),
            format,
            params
        );
        return FilterReply.DENY;
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
