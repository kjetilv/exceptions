package link.stuf.exceptions.core.inputs;

import java.util.Arrays;
import java.util.stream.Stream;

public class ParsedStackTraceElement {

    private final StackTraceElementParser parser;

    private final String[] matches;

    public ParsedStackTraceElement(StackTraceElementParser parser, String[] matches) {
        this.parser = parser;
        this.matches = matches;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + parser + " => " + (matches == null ? null : Arrays.asList(matches)) + "]";
    }

    Stream<StackTraceElement> reconstruct() {
        if (parser == StackTraceElementParser.MORE){
            return Stream.empty();
        }
        try {
            Integer lineNumber = parser.lineNo(matches);
            return Stream.of(new StackTraceElement(
                null,
                parser.module(matches),
                parser.moduleVersion(matches),
                parser.className(matches),
                parser.method(matches),
                parser.file(matches),
                lineNumber == null ? -1 : lineNumber
            ));
        } catch (Exception e) {
            throw new IllegalStateException(this + " failed to reconstruct", e);
        }
    }
}
