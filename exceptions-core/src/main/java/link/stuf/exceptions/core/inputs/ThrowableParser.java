package link.stuf.exceptions.core.inputs;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableParser {

    private static final String CAUSED_BY = "Caused by: ";

    public ChameleonException parse(String output) {
        List<String> lines = Arrays.stream(output.split("\n"))
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
        ChameleonException cause = null;
        for (ParsedThrowable parsedThrowable : parsedThrowables) {
            cause = parsedThrowable.reconstruct(cause);
        }
        return cause;
    }

    private String exceptionHeading(List<String> lines, List<Integer> causeIndices, int causeIndex) {
        String line = lines.get(causeIndices.get(causeIndex));
        return line.startsWith(CAUSED_BY) ? line.substring(CAUSED_BY.length()) : line;
    }

    private static StackTraceElement[] parsed(List<String> lines, int startIndex, int endIndex) {
        return lines.subList(startIndex, endIndex).stream().flatMap(line ->
            Stream.of(StackTraceElementType.values()).flatMap(pattern ->
                reconstructed(line, pattern)))
            .toArray(StackTraceElement[]::new);
    }

    private static Stream<StackTraceElement> reconstructed(String line, StackTraceElementType pattern) {
        String[] matches = pattern.parts(line);
        return matches.length == 0
            ? Stream.empty()
            : new ParsedStackTraceElement(pattern, matches).reconstruct();
    }

    private static boolean whitespaceAtStart(String line) {
        return Pattern.compile("^\\s+.*").matcher(line).matches();
    }

}
