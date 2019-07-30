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

import no.scienta.unearth.core.HandlingPolicy;
import no.scienta.unearth.munch.print.CausesRendering;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.stream.Stream;

public final class Logging {

    public interface DoLog {

        void log(String format, Object... args);
    }

    public static class WarnLog implements DoLog {

        private final Logger logger;

        public WarnLog(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void log(String format, Object... args) {
            logger.warn(format, args);
        }
    }

    private Logging() {
    }

    public static void doLog(
        DoLog log,
        HandlingPolicy policy,
        CausesRendering rendering,
        String format,
        Object... args
    ) {
        log.log(message(format), allPars(args, policy, rendering));
    }

    private static Object[] allPars(Object[] params, HandlingPolicy policy, CausesRendering rendering) {
        return allPars(
            new Object[]{
                policy.getFaultId(), policy.getFaultEventId()
            },
            params,
            new Object[]{
                String.join("\n", rendering.getStrings("  "))
            });
    }

    private static String message(String format) {
        return "{} {} " + format + "\n{}";
    }

    private static Object[] allPars(Object[]... params) {
        return Arrays.stream(params).flatMap(Logging::stream).toArray(Object[]::new);
    }

    private static Stream<Object> stream(Object[] params) {
        return params == null ? Stream.empty() : Arrays.stream(params);
    }
}
