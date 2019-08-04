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

import no.scienta.unearth.munch.print.CauseFrame;

import java.util.Arrays;
import java.util.stream.Stream;

class StackTraceParts {

    private final StackTraceEntry parser;

    private final String[] parts;

    StackTraceParts(StackTraceEntry parser, String[] parts) {
        this.parser = parser;
        this.parts = parts;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + parser + " => " + (parts == null ? null : Arrays.asList(parts)) + "]";
    }

    Stream<CauseFrame> reconstruct() {
        if (parser == StackTraceEntry.MORE) {
            return Stream.empty();
        }
        try {
            Integer lineNumber = parser.lineNo(parts);
            String file = parser.file(parts);
            return Stream.of(new CauseFrame(
                null,
                parser.module(parts),
                parser.moduleVersion(parts),
                parser.className(parts),
                parser.method(parts),
                file == null ? parser.otherSource(parts) : file,
                lineNumber == null ? -1 : lineNumber,
                parser.isNativeMethod()
            ));
        } catch (Exception e) {
            throw new IllegalStateException(this + " failed to reconstruct", e);
        }
    }
}
