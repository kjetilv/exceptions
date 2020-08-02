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

package unearth.munch.parser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import unearth.munch.ChameleonException;
import unearth.munch.print.CauseFrame;

final class ParsedThrowable {

    private final ExceptionHeading exceptionHeading;

    private final Collection<CauseFrame> parsedStackTrace;

    ParsedThrowable(ExceptionHeading exceptionHeading, CauseFrame... parsedStackTrace) {
        this(exceptionHeading, Arrays.asList(parsedStackTrace));
    }
    
    ParsedThrowable(ExceptionHeading exceptionHeading, Collection<CauseFrame> parsedStackTrace) {
        this.exceptionHeading = exceptionHeading;
        this.parsedStackTrace = parsedStackTrace == null || parsedStackTrace.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(parsedStackTrace);
    }

    ChameleonException reconstruct(Throwable caused) {
        ChameleonException chameleonException = new ChameleonException(
            exceptionHeading.getName(), exceptionHeading.getMessage(), caused);
        chameleonException.setStackTrace(parsedStackTrace.stream()
            .map(CauseFrame::toStackTraceElement)
            .toArray(StackTraceElement[]::new));
        return chameleonException;
    }
}
