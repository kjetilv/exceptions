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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import unearth.munch.print.CauseFrame;
import unearth.util.Streams;

public final class ThrowableParser {
    
    static final String CAUSED_BY = "Caused by: ";
    
    static final String SUPPRESSED = "Suppressed: ";
    
    static final Pattern LINE = Pattern.compile("\n");
    
    static final String AT = "\n\tat ";
    
    static final Pattern COLON = Pattern.compile(": ");
    
    public static Throwable parse(ByteBuffer buffer) {
        return parse(new String(buffer.array(), StandardCharsets.UTF_8));
    }
    
    public static Throwable parse(String in) {
        Objects.requireNonNull(in, "in");
        if (in.contains(SUPPRESSED)) {
            return wellFormedParse(in);
        }
        try {
            return tolerantParse(in);
        } catch (Exception e) {
            return failedParse(in, e);
        }
    }
    
    public static Throwable parseLevel(int level, List<String> lines, List<String> indents) {
        List<ParsedThrowable> throwables = parseLevel(lines, 0, lines.size(), level, indents);
        return reconstructed(throwables);
    }
    
    public static Optional<CauseFrame> parseCauseFrame(String line) {
        return Arrays.stream(StackTraceElementType.values()).flatMap(type ->
            reconstructed(type, line))
            .findFirst();
    }
    
    public static Optional<ExceptionHeading> parseExceptionHeading(String line) {
        return Optional.ofNullable(line).map(String::trim).flatMap(l ->
            Arrays.stream(ExceptionHeadingType.values()).flatMap(type ->
                reconstructed(type, l).stream())
                .findFirst());
    }
    
    public static Map<Integer, Integer> indexToIndex(Collection<Integer> ts, int lastIndex) {
        if (ts == null || ts.isEmpty()) {
            return Collections.emptyMap();
        }
        if (ts.size() == 1) {
            return Map.of(ts.iterator().next(), lastIndex);
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
    
    private ThrowableParser() {
    
    }
    
    private static final String EXCEPTION = Exception.class.getSimpleName();
    
    private static final String ERROR = Error.class.getSimpleName();
    
    private static final Pattern AT_PREAMBLE_PATTERN = Pattern.compile("\\s+at\\s");
    
    private static List<ParsedThrowable> parseLevel(
        List<String> lines,
        int startIndex,
        int stopIndex,
        int level,
        List<String> indents
    ) {
        List<ParsedThrowable> causes = parseCauses(lines, startIndex, stopIndex, level, indents);
        List<List<ParsedThrowable>> suppressions = parseSuppressions(lines, startIndex, stopIndex, level, indents);
        ParsedThrowable mainThrowable = parseException(startIndex, stopIndex, lines);
        
        return Stream.concat(
            Stream.of(mainThrowable),
            causes.stream()
        ).collect(Collectors.toList());
    }
    
    private static List<List<ParsedThrowable>> parseSuppressions(
        List<String> lines,
        int startIndex,
        int stopIndex,
        int level,
        List<String> indents
    ) {
        List<Integer> suppressedIndexes = getIndexes(
            lines,
            startIndex,
            stopIndex,
            indents.get(level + 1),
            SUPPRESSED);
        Map<Integer, Integer> suppressIndexes = indexToIndex(suppressedIndexes, stopIndex);
        
        return suppressIndexes.entrySet().stream().map(e ->
            parseLevel(lines, e.getKey(), e.getValue(), level + 1, indents)
        ).collect(Collectors.toList());
    }
    
    private static List<ParsedThrowable> parseCauses(
        List<String> lines,
        int startIndex,
        int stopIndex,
        int level,
        List<String> indents
    ) {
        List<Integer> causeIndexes = getIndexes(
            lines,
            startIndex,
            stopIndex,
            indents.get(level),
            CAUSED_BY);
        Map<Integer, Integer> causeStartStops =
            indexToIndex(causeIndexes, stopIndex);
        
        return causeStartStops.entrySet().stream().map(e ->
            parseException(e.getKey(), e.getValue(), lines)
        ).collect(Collectors.toList());
    }
    
    private static Throwable wellFormedParse(String in) {
        int level = 0;
        List<String> lines = lines(in);
        List<String> indents = getIndents(lines);
        return parseLevel(level, lines, indents);
    }
    
    private static ParsedThrowable parseException(int startIndex, int stopIndex, List<String> lines) {
        return parseExceptionHeading(lines.get(startIndex)).map(exceptionHeading ->
            new ParsedThrowable(
                exceptionHeading,
                stackFrames(
                    lines,
                    startIndex + 1, stopIndex))
        ).orElseThrow(() ->
            new IllegalArgumentException("Not an exception start @" + startIndex + ": " + lines.get(startIndex)));
    }
    
    private static Collection<CauseFrame> stackFrames(List<String> lines, int startIndex, int stopIndex) {
        return lines.subList(startIndex, stopIndex).stream()
            .map(ThrowableParser::parseCauseFrame)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }
    
    private static List<String> lines(String in) {
        return Arrays.stream(LINE.split(in))
            .filter(Objects::nonNull)
            .filter(line -> !line.isBlank())
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
    
    private static List<Integer> getIndexes(List<String> lines, String indent, String type) {
        return getIndexes(lines, 0, lines.size(), indent, type);
    }
    
    private static List<Integer> getIndexes(
        List<String> lines,
        int startIndex,
        int endIndex,
        String indent,
        String type
    ) {
        return IntStream.range(startIndex, endIndex)
            .filter(i -> lines.get(i).startsWith(indent + type))
            .boxed()
            .collect(Collectors.toList());
    }
    
    private static Throwable tolerantParse(String in) {
        List<String> trimmedLines = trimmed(in);
        List<Integer> causeIndices = causeIndices(trimmedLines);
        List<ParsedThrowable> parsedThrowables =
            parsed(trimmedLines, causeIndices);
        return reconstructed(parsedThrowables);
    }
    
    private static Throwable reconstructed(List<ParsedThrowable> parsedThrowables) {
        
        List<ParsedThrowable> list = new ArrayList<>(parsedThrowables);
        Collections.reverse(list);
        Throwable cause = null;
        for (ParsedThrowable parsedThrowable: list) {
            cause = parsedThrowable.reconstruct(cause);
        }
        return cause;
    }
    
    private static List<Integer> causeIndices(List<String> trimmedLines) {
        
        return IntStream.range(0, trimmedLines.size())
            .filter(index ->
                !isStacktraceLine(trimmedLines, index))
            .filter(index -> {
                String line = trimmedLines.get(index);
                return types(EXCEPTION, ERROR).anyMatch(line::contains) || line.startsWith(CAUSED_BY);
            })
            .boxed()
            .collect(Collectors.toList());
    }
    
    private static boolean isStacktraceLine(List<String> trimmedLines, int index) {
        
        return trimmedLines.get(index).startsWith("at ");
    }
    
    private static Stream<String> types(String... types) {
        
        return Arrays.stream(types).map(type -> type + ": ");
    }
    
    private static List<String> trimmed(String in) {
        String[] lines = LINE.split(AT_PREAMBLE_PATTERN.matcher(in.trim()).replaceAll(AT));
        return Arrays.stream(lines)
            .filter(Objects::nonNull)
            .filter(line -> !line.isBlank())
            .map(String::trim)
            .collect(Collectors.toList());
    }
    
    private static List<ParsedThrowable> parsed(List<String> trimmmedLines, List<Integer> causeIndices) {
        
        return causeIndices.stream().map(causeIndex -> {
                CauseFrame[] causeFrames =
                    stackTrace(trimmmedLines, causeIndices, causeIndex);
                ExceptionHeading exceptionHeading =
                    getExceptionHeading(trimmmedLines, causeIndex);
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
    
    private static CauseFrame[] stackTrace(List<String> trimmedLines, List<Integer> causeIndices, int causeIndex) {
        
        int endIndex = causeIndex >= causeIndices.size() - 1
            ? trimmedLines.size() - 1
            : causeIndices.get(causeIndex + 1);
        return parsed(
            trimmedLines,
            causeIndex + 1,
            endIndex);
    }
    
    private static <T> T failedParse(String in, Exception e) {
        
        throw new IllegalArgumentException(
            "Failed to parse as exception: " + in.substring(0, Math.min(30, in.length())) + "...", e);
    }
    
    private static CauseFrame[] parsed(List<String> trimmedLines, int startIndex, int endIndex) {
        
        if (startIndex > endIndex) {
            String lines =
                trimmedLines.size() > startIndex ? trimmedLines.get(startIndex) : trimmedLines.size() + " lines";
            throw new IllegalArgumentException("Line start " + startIndex + " > end " + endIndex + " @ " + lines);
        }
        return trimmedLines.subList(startIndex, endIndex).stream()
            .map(ThrowableParser::parseCauseFrame)
            .flatMap(Optional::stream)
            .toArray(CauseFrame[]::new);
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
