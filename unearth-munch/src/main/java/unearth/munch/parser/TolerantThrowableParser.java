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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import unearth.munch.print.CauseFrame;

final class TolerantThrowableParser {
    
    static final String CAUSED_BY = "Caused by: ";
    
    static final Pattern LINE = Pattern.compile("\n");
    
    static final String AT = "\n\tat ";
    
    static Throwable parse(String in) {
        List<String> trimmedLines = trimmed(in);
        List<Integer> causeIndices = causeIndices(trimmedLines);
        List<ParsedThrowable> parsedThrowables =
            parsed(trimmedLines, causeIndices);
        return ParsedThrowable.reconstructed(parsedThrowables);
    }
    
    private TolerantThrowableParser() {
    
    }
    
    private static final String EXCEPTION = Exception.class.getSimpleName();
    
    private static final String ERROR = Error.class.getSimpleName();
    
    private static final Pattern AT_PREAMBLE_PATTERN = Pattern.compile("\\s+at\\s");
    
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
    
    private static CauseFrame[] parsed(List<String> trimmedLines, int startIndex, int endIndex) {
        
        if (startIndex > endIndex) {
            String lines =
                trimmedLines.size() > startIndex ? trimmedLines.get(startIndex) : trimmedLines.size() + " lines";
            throw new IllegalArgumentException("Line start " + startIndex + " > end " + endIndex + " @ " + lines);
        }
        return trimmedLines.subList(startIndex, endIndex).stream()
            .map(TolerantThrowableParser::parseCauseFrame)
            .flatMap(Optional::stream)
            .toArray(CauseFrame[]::new);
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
}
