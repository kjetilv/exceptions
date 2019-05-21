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
            StackTraceElement[] parsed = parsed(
                lines,
                causeIndices.get(causeIndex) + 1,
                causeIndex == causeIndices.size() - 1 ? lines.size() : causeIndices.get(causeIndex + 1));
            String s = exceptionHeading(lines, causeIndices, causeIndex);
            return new ParsedThrowable(s, parsed);
        }).collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(parsedThrowables);
        ChameleonException walker = null;
        for (ParsedThrowable parsedThrowable : parsedThrowables) {
            walker = parsedThrowable.chain(walker);
        }
        return walker;
    }

    private String exceptionHeading(List<String> lines, List<Integer> causeIndices, int causeIndex) {
        String line = lines.get(causeIndices.get(causeIndex));
        return line.startsWith(CAUSED_BY) ? line.substring(CAUSED_BY.length()) : line;
    }

    private static StackTraceElement[] parsed(List<String> lines, int startIndex, int endIndex) {
        return lines.subList(startIndex, endIndex).stream().flatMap(line ->
            Stream.of(StackTraceElementParser.values()).flatMap(pattern ->
                reconstructed(line, pattern)))
            .toArray(StackTraceElement[]::new);
    }

    private static Stream<StackTraceElement> reconstructed(String line, StackTraceElementParser pattern) {
        String[] matches = pattern.matches(line);
        return matches.length > 0
            ? new ParsedStackTraceElement(pattern, matches).reconstruct()
            : Stream.empty();
    }

    private static boolean whitespaceAtStart(String line) {
        return Pattern.compile("^\\s+.*").matcher(line).matches();
    }

}
