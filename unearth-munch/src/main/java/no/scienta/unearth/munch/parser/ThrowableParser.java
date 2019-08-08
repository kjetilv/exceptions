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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableParser {

    private static final String CAUSED_BY = "Caused by: ";

    private static final String EXCEPTION = Exception.class.getSimpleName();

    private static final String ERROR = Error.class.getSimpleName();

    public static Throwable parse(ByteBuffer buffer) {
        return parse(new String(buffer.array(), StandardCharsets.UTF_8));
    }

    public static Throwable parse(String in) {
        try {
            List<String> lines = trimmed(in);
            List<Integer> causeIndices = causeIndices(lines);
            List<ParsedThrowable> parsedThrowables = parsed(lines, causeIndices);
            return reconstructed(parsedThrowables);
        } catch (Exception e) {
            return failedParse(in, e);
        }
    }

    private static Throwable reconstructed(List<ParsedThrowable> parsedThrowables) {
        List<ParsedThrowable> list = new ArrayList<>(parsedThrowables);
        Collections.reverse(list);
        Throwable cause = null;
        for (ParsedThrowable parsedThrowable : list) {
            cause = parsedThrowable.reconstruct(cause);
        }
        return cause;
    }

    private static List<Integer> causeIndices(List<String> lines) {
        return IntStream.range(0, lines.size())
            .filter(index -> {
                String line = lines.get(index);
                return !line.startsWith("at ");
            })
            .filter(index -> {
                String line = lines.get(index);
                return types(EXCEPTION, ERROR).anyMatch(line::contains) || line.startsWith(CAUSED_BY);
            })
            .boxed()
            .collect(Collectors.toList());
    }

    private static Stream<String> types(String... types) {
        return Arrays.stream(types).map(type -> type + ": ");
    }

    private static List<String> trimmed(String in) {
        return Arrays.stream(in.trim().split("\n"))
            .filter(Objects::nonNull)
            .filter(line -> !line.isBlank())
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private static List<ParsedThrowable> parsed(List<String> lines, List<Integer> causeIndices) {
        return IntStream.range(0, causeIndices.size())
            .map(causeIndices::get)
            .mapToObj(causeIndex -> {
                    CauseFrame[] causeFrames =
                        stackTrace(lines, causeIndices, causeIndex);
                    ExceptionHeading exceptionHeading =
                        getExceptionHeading(lines, causeIndex);
                    return new ParsedThrowable(
                        exceptionHeading, causeFrames);
                }
            ).collect(Collectors.toList());
    }

    private static ExceptionHeading getExceptionHeading(List<String> lines, int causeIndex) {
        String line = lines.get(causeIndex);
        Optional<String> simple = Stream.of(EXCEPTION, ERROR)
            .filter(type -> line.contains(type + ": "))
            .findFirst();
        Optional<ExceptionHeading> simpleParsed = simple.map(type ->
            getExceptionHeading(type, line));
        if (simpleParsed.isPresent()) {
            return simpleParsed.get();
        }
        if (line.contains(CAUSED_BY)) {
            return getCauseExceptionHeading(line);
        }
        throw new IllegalStateException("Not cause index: " + causeIndex + ": " + lines.get(causeIndex));
    }

    private static ExceptionHeading getExceptionHeading(String type, String line) {
        int hit = line.indexOf(type + ": ");
        int i = hit;
        while (true) {
            if (i == 0 || Character.isWhitespace(line.charAt(i - 1))) {
                String exceptionName = line.substring(i, hit) + type;
                String message = line.substring(hit + type.length() + 2);
                return new ExceptionHeading(exceptionName, message);
            }
            i--;
        }
    }

    private static ExceptionHeading getCauseExceptionHeading(String line) {
        int startIndex = line.indexOf(CAUSED_BY);
        int nextIndex = line.indexOf(":", startIndex);
        String exceptionName = line.substring(startIndex + CAUSED_BY.length(), nextIndex);
        String message = line.substring(startIndex + CAUSED_BY.length() + 2);
        return new ExceptionHeading(exceptionName, message);
    }

    private static CauseFrame[] stackTrace(List<String> lines, List<Integer> causeIndices, int causeIndex) {
        int endIndex = causeIndex >= causeIndices.size() - 1
            ? lines.size() - 1
            : causeIndices.get(causeIndex + 1);
        return parsed(
            lines,
            causeIndex + 1,
            endIndex);
    }

    private static <T> T failedParse(String in, Exception e) {
        throw new IllegalArgumentException(
            "Failed to parse as exception: " + in.substring(0, Math.min(30, in.length())) + "...", e);
    }

    private static CauseFrame[] parsed(List<String> lines, int startIndex, int endIndex) {
        return lines.subList(startIndex, endIndex).stream()
            .flatMap(line ->
                Stream.of(StackTraceEntry.values()).flatMap(type -> reconstructed(type, line)))
            .toArray(CauseFrame[]::new);
    }

    private static Stream<CauseFrame> reconstructed(StackTraceEntry pattern, String line) {
        String[] matches = pattern.parts(line);
        return matches.length == 0
            ? Stream.empty()
            : new StackTraceParts(pattern, matches).reconstruct();
    }

}
