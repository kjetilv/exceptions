package link.stuf.exceptions.core.inputs;

import java.util.Arrays;
import java.util.stream.Stream;

public class ParsedStackTraceElement {

    private final StackTraceElementType parser;

    private final String[] parts;

    public ParsedStackTraceElement(StackTraceElementType parser, String[] parts) {
        this.parser = parser;
        this.parts = parts;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
            "[" + parser + " => " + (parts == null ? null : Arrays.asList(parts)) + "]";
    }

    Stream<StackTraceElement> reconstruct() {
        if (parser == StackTraceElementType.MORE){
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
                file == null ? parser.otherSource(parts): file,
                lineNumber == null ? -1 : lineNumber
            ));
        } catch (Exception e) {
            throw new IllegalStateException(this + " failed to reconstruct", e);
        }
    }
}
