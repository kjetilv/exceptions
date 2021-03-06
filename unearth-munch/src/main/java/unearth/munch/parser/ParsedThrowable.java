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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import unearth.munch.ChameleonException;
import unearth.munch.print.CauseFrame;

final class ParsedThrowable {
    
    static Throwable reconstructed(List<ParsedThrowable> parsedThrowables) {
        return reconstructed(null, parsedThrowables);
    }
    
    private final ExceptionHeading heading;
    
    private final List<CauseFrame> stackTrace;
    
    private final List<List<ParsedThrowable>> suppressions;
    
    private final int more;
    
    ParsedThrowable(ExceptionHeading heading, CauseFrame... stackTrace) {
        this(heading, Arrays.asList(stackTrace));
    }
    
    ParsedThrowable(ExceptionHeading heading, List<CauseFrame> stackTrace) {
        this(
            heading,
            stackTrace == null || stackTrace.isEmpty()
                ? Collections.emptyList()
                : stackTrace.stream()
                    .takeWhile(CauseFrame::isPrintable)
                    .collect(Collectors.toList()),
            Collections.emptyList(),
            stackTrace == null || stackTrace.isEmpty()
                ? -1
                : stackTrace.stream()
                    .dropWhile(CauseFrame::isPrintable)
                    .filter(CauseFrame::isRef)
                    .map(CauseFrame::getMore)
                    .findFirst()
                    .orElse(-1));
    }
    
    ParsedThrowable(
        ExceptionHeading heading,
        List<CauseFrame> stackTrace,
        List<List<ParsedThrowable>> suppressions,
        int more
    ) {
        this.heading = heading;
        this.stackTrace = stackTrace;
        this.suppressions = suppressions;
        this.more = more;
    }
    
    ParsedThrowable withSuppressed(List<List<ParsedThrowable>> suppressions) {
        return suppressions == null || suppressions.isEmpty()
            ? this
            : new ParsedThrowable(heading, stackTrace, suppressions, more);
    }
    
    ParsedThrowable enclosedBy(ParsedThrowable encloser) {
        if (more <= 0 || encloser == null) {
            return this;
        }
        int enclosingFrames = encloser.stackTrace.size() - more;
        if (enclosingFrames < 0) {
            throw new IllegalArgumentException(
                this + " is not enclosed by " + encloser + ", " + enclosingFrames + " frames");
        }
        return new ParsedThrowable(
            heading, Stream.concat(
            stackTrace.stream(),
            encloser.stackTrace.stream().skip(enclosingFrames))
            .collect(Collectors.toList()),
            suppressions,
            more);
    }
    
    ChameleonException reconstruct(Throwable caused) {
        ChameleonException chameleonException = new ChameleonException(
            heading.getName(), heading.getMessage(), !suppressions.isEmpty(), caused);
        chameleonException.setStackTrace(stackTrace.stream()
            .takeWhile(CauseFrame::isPrintable)
            .map(CauseFrame::toStackTraceElement)
            .toArray(StackTraceElement[]::new));
        suppressions.stream()
            .map(this::reconstructEnclosed)
            .forEach(chameleonException::addSuppressed);
        return chameleonException;
    }
    
    private Throwable reconstructEnclosed(List<ParsedThrowable> parsedThrowables) {
        return reconstructed(this, parsedThrowables);
    }
    
    private static Throwable reconstructed(
        ParsedThrowable enclosing,
        List<ParsedThrowable> parsedThrowables
    ) {
        List<ParsedThrowable> list = new ArrayList<>(parsedThrowables);
        for (int i = 0; i < list.size(); i++) {
            ParsedThrowable encloser = i == 0
                ? enclosing
                : list.get(i - 1);
            list.set(i, list.get(i).enclosedBy(encloser));
        }
        Collections.reverse(list);
        Throwable cause = null;
        for (ParsedThrowable parsedThrowable: list) {
            cause = parsedThrowable.reconstruct(cause);
        }
        return cause;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
            heading.getMessage() + "/" +
            heading.getName() + ": " +
            stackTrace.size() +
            (suppressions.isEmpty() ? "" : " s:" + suppressions.size()) +
            "]";
    }
}
