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
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.core.parser;

import no.scienta.unearth.munch.data.ChameleonException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableParser {

    private static final String CAUSED_BY = "Caused by: ";

    public static Throwable parse(ByteBuffer buffer) {
        return parse(new String(buffer.array(), StandardCharsets.UTF_8));
    }

    public static Throwable parse(String in) {
        try {
            List<String> lines = Arrays.stream(in.split("\n"))
                .filter(Objects::nonNull)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
            List<Integer> causeIndices =
                IntStream.range(0, lines.size())
                    .filter(index ->
                        !whitespaceAtStart(lines.get(index)))
                    .boxed().collect(Collectors.toList());

            List<ParsedThrowable> parsedThrowables = IntStream.range(0, causeIndices.size()).mapToObj(causeIndex -> {
                int endIndex = causeIndex >= causeIndices.size() - 1
                    ? lines.size()
                    : causeIndices.get(causeIndex + 1);
                StackTraceElement[] parsed = parsed(
                    lines,
                    causeIndices.get(causeIndex) + 1,
                    endIndex);
                String s = exceptionHeading(lines, causeIndices, causeIndex);
                return new ParsedThrowable(s, parsed);
            }).collect(Collectors.toCollection(ArrayList::new));
            Collections.reverse(parsedThrowables);
            Throwable cause = null;
            for (ParsedThrowable parsedThrowable : parsedThrowables) {
                cause = parsedThrowable.reconstruct(cause);
            }
            return cause;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Failed to parse as exception: " + in.substring(0, Math.max(30, in.length())) + "...", e);
        }
    }

    private static String exceptionHeading(List<String> lines, List<Integer> causeIndices, int causeIndex) {
        String line = lines.get(causeIndices.get(causeIndex));
        return line.startsWith(CAUSED_BY) ? line.substring(CAUSED_BY.length()) : line;
    }

    private static StackTraceElement[] parsed(List<String> lines, int startIndex, int endIndex) {
        return lines.subList(startIndex, endIndex).stream().flatMap(line ->
            Stream.of(StackTraceEntry.values()).flatMap(type ->
                reconstructed(type, line)))
            .toArray(StackTraceElement[]::new);
    }

    private static Stream<StackTraceElement> reconstructed(StackTraceEntry pattern, String line) {
        String[] matches = pattern.parts(line);
        return matches.length == 0
            ? Stream.empty()
            : new StackTraceParts(pattern, matches).reconstruct();
    }

    private static boolean whitespaceAtStart(String line) {
        return Pattern.compile("^\\s+.*").matcher(line).matches();
    }

    private static class ParsedThrowable {

        private final String exceptionHeading;

        private final StackTraceElement[] parsedStackTrace;

        private ParsedThrowable(String exceptionHeading, StackTraceElement[] parsedStackTrace) {
            this.exceptionHeading = exceptionHeading;
            this.parsedStackTrace = parsedStackTrace;
        }

        private ChameleonException reconstruct(Throwable caused) {
            String[] split = exceptionHeading.split(": ", 2);
            String exceptionName = split[0];
            String message = split[1];
            ChameleonException chameleonException = new ChameleonException(exceptionName, message, caused);
            chameleonException.setStackTrace(parsedStackTrace);
            return chameleonException;
        }
    }

    static class StackTraceParts {

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

        Stream<StackTraceElement> reconstruct() {
            if (parser == StackTraceEntry.MORE) {
                return Stream.empty();
            }
            try {
                Integer lineNumber = parser.lineNo(parts);
                String file = parser.file(parts);
                return Stream.of(new StackTraceElement(
                    null,
                    parser.module(parts),
                    parser.moduleVersion(parts),
                    parser.className(parts),
                    parser.method(parts),
                    file == null ? parser.otherSource(parts) : file,
                    lineNumber == null ? -1 : lineNumber
                ));
            } catch (Exception e) {
                throw new IllegalStateException(this + " failed to reconstruct", e);
            }
        }
    }
}
