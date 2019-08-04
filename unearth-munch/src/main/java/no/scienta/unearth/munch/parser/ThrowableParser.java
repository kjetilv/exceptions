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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableParser {

    private static final String CAUSED_BY = "Caused by: ";

    private static final String EXCEPTION = "Exception: ";

    private static final String ERROR = "Error: ";

    private static final Pattern HEADING = Pattern.compile(".*\\s+([\\w.]+): (.*)");

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
            .filter(index ->
                !lines.get(index).trim().startsWith("at "))
            .filter(index ->
                Stream.of(EXCEPTION, CAUSED_BY, ERROR).anyMatch(lines.get(index)::startsWith))
            .boxed()
            .collect(Collectors.toList());
    }

    private static List<String> trimmed(String in) {
        return Arrays.stream(in.trim().split("\n"))
            .filter(Objects::nonNull)
            .filter(line -> !line.trim()
                .isEmpty())
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private static List<ParsedThrowable> parsed(List<String> lines, List<Integer> causeIndices) {
        return IntStream.range(0, causeIndices.size()).mapToObj(causeIndex -> {
            int endIndex = causeIndex >= causeIndices.size() - 1
                ? lines.size()
                : causeIndices.get(causeIndex + 1);
            CauseFrame[] parsedStackTrace = parsed(
                lines,
                causeIndices.get(causeIndex) + 1,
                endIndex);
            Matcher matcher = matcher(lines.get(causeIndices.get(causeIndex)));
            return new ParsedThrowable(matcher.group(1), matcher.group(2), parsedStackTrace);
        }).collect(Collectors.toList());
    }

    private static Matcher matcher(String line) {
        Matcher matcher = HEADING.matcher(line);
        if (matcher.matches()) {
            return matcher;
        }
        throw new IllegalStateException("Could not parse heading: " +
            (line.length() > 20 ? line.substring(0, 20) + " ..." : line));
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
