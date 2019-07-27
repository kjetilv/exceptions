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
import no.scienta.unearth.munch.model.CauseChain;
import no.scienta.unearth.munch.print.Rendering;
import no.scienta.unearth.munch.print.ThrowableRenderer;
import org.slf4j.Marker;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnearthlyTurboFilter extends TurboFilter {

    private final FaultHandler faultHandler;

    private final ThrowableRenderer renderer;

    public UnearthlyTurboFilter(FaultHandler faultHandler, ThrowableRenderer renderer) {
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
        if (t == null) {
            return FilterReply.NEUTRAL;
        }
        HandlingPolicy handle = faultHandler.handle(t, format, params);
        Collection<Rendering> renderings = handle.getPrintout()
            .map(causeChain -> causeChain.withStackRendering(renderer))
            .map(CauseChain::getChainRendering)
            .orElseGet(Collections::emptySet);
        Collection<String> printout = renderings.stream()
            .map(render -> render.getStrings("  "))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        Object[] pars = allPars(
            new Object[]{handle.getFaultId(), handle.getFaultEventId()},
            params,
            new Object[]{t.toString(), String.join("\n", printout)});

        String message = "{} {} " + format + "\n{}{}";
        String fqcn = logger.getName();
        int logLevel = level.toInt() / 1000;
        logger.log(marker, fqcn, logLevel, message, pars, null);
        return FilterReply.DENY;
    }

    private Object[] allPars(Object[]... params) {
        return Arrays.stream(params).flatMap(this::stream).toArray(Object[]::new);
    }

    private Stream<Object> stream(Object[] params) {
        return params == null ? Stream.empty() : Arrays.stream(params);
    }
}
