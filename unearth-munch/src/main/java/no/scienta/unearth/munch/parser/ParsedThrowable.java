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

package no.scienta.unearth.munch.parser;

import no.scienta.unearth.munch.ChameleonException;
import no.scienta.unearth.munch.print.CauseFrame;

import java.util.Arrays;

final class ParsedThrowable {

    private final ExceptionHeading exceptionHeading;

    private final CauseFrame[] parsedStackTrace;

    ParsedThrowable(ExceptionHeading exceptionHeading, CauseFrame[] parsedStackTrace) {
        this.exceptionHeading = exceptionHeading;
        this.parsedStackTrace = parsedStackTrace;
    }

    ChameleonException reconstruct(Throwable caused) {
        ChameleonException chameleonException = new ChameleonException(
            exceptionHeading.getName(), exceptionHeading.getMessage(), caused);
        chameleonException.setStackTrace(Arrays.stream(parsedStackTrace)
            .map(CauseFrame::toStackTraceElement)
            .toArray(StackTraceElement[]::new));
        return chameleonException;
    }
}
