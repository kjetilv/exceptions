package link.stuf.exceptions.core.inputs;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ThrowableParser {

    private static final String CAUSED_BY = "Caused by: ";

    public static String echo(ByteBuffer buffer) {
        return echo(buffer.array());
    }

    public static String echo(byte[] array) {
        return echo(new String(array, StandardCharsets.UTF_8));
    }

    public static String echo(String input) {
        return print(parse(input));
    }

    public static Throwable parse(ByteBuffer buffer) {
        return parse(buffer.array());
    }

    public static Throwable parse(byte[] array) {
        return parse(new String(array, StandardCharsets.UTF_8));
    }

    public static Throwable parse(String in) {
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
    }

    private static String print(Throwable e) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(out)) {
            e.printStackTrace(pw);
        }
        return Arrays.stream(new String(out.toByteArray()).split("\n"))
            .filter(line -> StackTraceEntry.MORE.parts(line).length == 0)
            .collect(Collectors.joining("\n"));
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

}
