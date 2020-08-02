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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import unearth.munch.ChameleonException;
import unearth.munch.print.CauseFrame;

final class ParsedThrowable {
    
    static Throwable reconstructed(List<ParsedThrowable> parsedThrowables) {
        List<ParsedThrowable> list = new ArrayList<>(parsedThrowables);
        Collections.reverse(list);
        Throwable cause = null;
        for (ParsedThrowable parsedThrowable: list) {
            cause = parsedThrowable.reconstruct(cause);
        }
        return cause;
    }
    
    private final ExceptionHeading exceptionHeading;
    
    private final List<CauseFrame> parsedStackTrace;
    
    private final List<List<ParsedThrowable>> suppressions;
    
    ParsedThrowable(ExceptionHeading exceptionHeading, CauseFrame... parsedStackTrace) {
        this(exceptionHeading, Arrays.asList(parsedStackTrace));
    }
    
    ParsedThrowable(ExceptionHeading exceptionHeading, List<CauseFrame> parsedStackTrace) {
        this(
            exceptionHeading,
            parsedStackTrace == null || parsedStackTrace.isEmpty()
                ? Collections.emptyList()
                : parsedStackTrace,
            Collections.emptyList());
    }
    
    ParsedThrowable(
        ExceptionHeading exceptionHeading,
        List<CauseFrame> parsedStackTrace,
        List<List<ParsedThrowable>> suppressions
    ) {
        this.exceptionHeading = exceptionHeading;
        this.parsedStackTrace = parsedStackTrace;
        this.suppressions = suppressions;
    }
    
    ParsedThrowable withSuppressed(List<List<ParsedThrowable>> suppressions) {
        return suppressions == null || suppressions.isEmpty()
            ? this
            : new ParsedThrowable(exceptionHeading, parsedStackTrace, suppressions);
    }
    
    ChameleonException reconstruct(Throwable caused) {
        ChameleonException chameleonException = new ChameleonException(
            exceptionHeading.getName(), exceptionHeading.getMessage(), !suppressions.isEmpty(), caused);
        chameleonException.setStackTrace(parsedStackTrace.stream()
            .map(CauseFrame::toStackTraceElement)
            .toArray(StackTraceElement[]::new));
        suppressions.stream()
            .map(ParsedThrowable::reconstructed)
            .forEach(chameleonException::addSuppressed);
        return chameleonException;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            exceptionHeading.getMessage() + "/" +
            exceptionHeading.getName() + ": " +
            parsedStackTrace.size() +
            (suppressions.isEmpty() ? "" : " s:" + suppressions.size()) +
            "]";
    }
}
