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
import java.util.stream.Stream;

import unearth.munch.print.CauseFrame;

final class StackTraceParts {

    private final StackTraceEntry parser;

    private final String[] parts;

    StackTraceParts(StackTraceEntry parser, String[] parts) {
        this.parser = parser;
        this.parts = parts;
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
                CauseFrame.Module(parser.module(parts)),
                CauseFrame.ModuleVer(parser.moduleVersion(parts)),
                CauseFrame.ClassName(parser.className(parts)),
                CauseFrame.Method(parser.method(parts)),
                CauseFrame.File(file == null ? parser.otherSource(parts) : file),
                lineNumber == null ? -1 : lineNumber,
                parser.isNativeMethod()
            ));
        } catch (Exception e) {
            throw new IllegalStateException(this + " failed to reconstruct", e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + parser + " => " + (parts == null ? null : Arrays.asList(parts)) + "]";
    }
}
