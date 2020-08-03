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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import unearth.munch.print.CauseFrame;
import unearth.util.Streams;

final class WellformedThrowableParser {
    
    static Throwable parse(String in) {
        int level = 0;
        List<String> lines = lines(in);
        List<String> indents = getIndents(lines);
        return parse(level, lines, indents);
    }
    
    private WellformedThrowableParser() {
    }
    
    private static final String SUPPRESSED = "Suppressed: ";
    
    private static final String CAUSED_BY = "Caused by: ";
    
    private static final Pattern LINE = Pattern.compile("\n");
    
    private static Optional<ExceptionHeading> parseExceptionHeading(String line) {
        return Optional.ofNullable(line).map(String::trim).flatMap(l ->
            Arrays.stream(ExceptionHeadingType.values()).flatMap(type ->
                reconstructed(type, l).stream())
                .findFirst());
    }
    
    private static Throwable parse(int level, List<String> lines, List<String> indents) {
        List<ParsedThrowable> throwables =
            parseLevel(lines, 0, lines.size(), level, indents, true);
        return ParsedThrowable.reconstructed(throwables);
    }
    
    private static Map<Integer, Integer> indexToIndex(Collection<Integer> ts, int lastIndex) {
        if (ts == null || ts.isEmpty()) {
            return Collections.emptyMap();
        }
        if (ts.length == 1) {
            return Map.of(ts[0], lastIndex);
        }
        return Stream.concat(
            Streams.tuplify(ts, 2),
            Stream.of(List.of(new LinkedList<>(ts).getLast(), lastIndex)))
            .collect(Collectors.toMap(
                list -> list.get(0),
                list -> list.get(1),
                (i1, i2) -> {
                    throw new IllegalStateException(i1 + "/" + i2);
                },
                LinkedHashMap::new));
    }
    
    private static List<ParsedThrowable> parseLevel(
        List<String> lines,
        int startIndex,
        int stopIndex,
        int level,
        List<String> indents,
        boolean descend
    ) {
        int[] causeIndexes = descend
            ? getIndexes(
            lines,
            startIndex + 1,
            stopIndex,
            indents.get(level),
            CAUSED_BY)
            : new int[0];
        Map<Integer, Integer> causeStartStops = indexToIndex(causeIndexes, stopIndex);
        
        int suppressStopIndex = causeIndexes.length == 0 ? stopIndex : causeIndexes[0];
        int[] suppressedIndexes = getIndexes(
            lines,
            startIndex + 1,
            suppressStopIndex,
            indents.get(level + 1),
            SUPPRESSED);
        Map<Integer, Integer> suppressStartStops = indexToIndex(suppressedIndexes, suppressStopIndex);
        int mainStopIndex = firstIndex(suppressedIndexes, causeIndexes).orElse(stopIndex);
    
        ParsedThrowable mainThrowable = parseExceptionHeading(lines.get(startIndex))
            .map(heading ->
                new ParsedThrowable(
                    heading,
                    stackFrames(lines, startIndex + 1, mainStopIndex)))
            .orElseThrow(() ->
                new IllegalArgumentException("Not an exception start @" + startIndex + ": " + lines.get(startIndex)));
        
        List<List<ParsedThrowable>> suppressions =
            parseSuppressions(lines, level, indents, suppressStartStops);
        
        List<ParsedThrowable> causes =
            parseCauses(lines, level, indents, causeStartStops);
        
        return Stream.concat(
            Stream.of(mainThrowable.withSuppressed(suppressions)),
            causes.stream())
            .collect(Collectors.toList());
    }
    
    private static List<ParsedThrowable> parseCauses(
        List<String> lines,
        int level,
        List<String> indents,
        Map<Integer, Integer> causeStartStops
    ) {
        return causeStartStops.entrySet().stream()
            .flatMap(e ->
                parseLevel(lines, e.getKey(), e.getValue(), level, indents, false).stream())
            .collect(Collectors.toList());
    }
    
    private static List<List<ParsedThrowable>> parseSuppressions(
        List<String> lines,
        int level,
        List<String> indents,
        Map<Integer, Integer> suppressStartStops
    ) {
        return suppressStartStops.entrySet().stream()
            .map(e ->
                parseLevel(lines, e.getKey(), e.getValue(), level + 1, indents, true))
            .collect(Collectors.toList());
    }
    
    private static int mainStopIndex(
        List<Integer> suppressedIndexes,
        List<Integer> causeIndexes,
        int stopIndex
    ) {
        if (!suppressedIndexes.isEmpty()) {
            return suppressedIndexes.get(0);
        }
        ;
        if (!causeIndexes.isEmpty()) {
            return causeIndexes.get(0);
        }
        return stopIndex;
    }
    
    private static List<CauseFrame> stackFrames(List<String> lines, int startIndex, int stopIndex) {
        return lines.subList(startIndex, stopIndex).stream()
            .map(WellformedThrowableParser::parseCauseFrame)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }
    
    private static List<String> lines(String in) {
        return Arrays.stream(LINE.split(in))
            .filter(obj -> !
                (obj == null || obj.isBlank()))
            .collect(Collectors.toList());
    }
    
    private static List<String> getIndents(List<String> lines) {
        return lines.stream()
            .map(line ->
                Character.isWhitespace(line.charAt(0)) ? line.substring(0, line.indexOf(line.trim())) : "")
            .distinct()
            .sorted(Comparator.comparing(String::length))
            .collect(Collectors.toList());
    }
    
    private static int[] getIndexes(
        List<String> lines,
        int startIndex,
        int endIndex,
        String indent,
        String type
    ) {
        return IntStream.range(startIndex, endIndex)
            .filter(i -> lines.get(i).startsWith(indent + type))
            .toArray();
    }
    
    private static Optional<CauseFrame> parseCauseFrame(String line) {
        return Arrays.stream(StackTraceElementType.values()).flatMap(type ->
            reconstructed(type, line))
            .findFirst();
    }
    
    private static Stream<CauseFrame> reconstructed(StackTraceElementType type, String line) {
        return Optional.ofNullable(type.toParts(line)).stream()
            .map(matches ->
                new StackTraceParts(type, matches))
            .flatMap(StackTraceParts::reconstruct);
    }
    
    private static Optional<ExceptionHeading> reconstructed(ExceptionHeadingType type, String line) {
        return Optional.ofNullable(type.toParts(line)).map(matches ->
            new ExceptionHeading(type.type(matches), type.message(matches)));
    }
}
