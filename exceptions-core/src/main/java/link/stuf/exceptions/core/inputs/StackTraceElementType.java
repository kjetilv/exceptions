package link.stuf.exceptions.core.inputs;

import java.util.regex.Pattern;
import java.util.stream.IntStream;

enum StackTraceElementType implements StackTraceElementPicker {

    NATIVE_METHOD_WITH_MODULE(Pattern.compile("^\\s*at\\s([\\w.]*)/([\\w.]*)\\(([\\w\\s]*)\\)$"), 3) {

        @Override
        public String module(String... parts) {
            return parts[0];
        }

        @Override
        public String className(String... parts) {
            return StackTraceElementType.getClassName(parts[1]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceElementType.getMethodName(parts[1]);
        }

        @Override
        public String otherSource(String... parts) {
            return parts[2];
        }
    },

    BASIC_NATIVE_METHOD(Pattern.compile("^\\s*at\\s([$\\w.]*)\\(([\\w\\s]*)\\)$"), 2) {

        @Override
        public String className(String... parts) {
            return StackTraceElementType.getClassName(parts[0]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceElementType.getMethodName(parts[0]);
        }

        @Override
        public String otherSource(String... parts) {
            return parts[1];
        }
    },

    WITH_MODULE(Pattern.compile("^\\s*at\\s([$\\w.]*)/([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$"), 4) {

        @Override
        public String module(String... parts) {
            return parts[0];
        }

        @Override
        public String className(String... parts) {
            return StackTraceElementType.getClassName(parts[1]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceElementType.getMethodName(parts[1]);
        }

        @Override
        public String file(String... parts) {
            return parts[2];
        }

        @Override
        public Integer lineNo(String... parts) {
            return Integer.parseInt(parts[3]);
        }
    },

    BASIC(Pattern.compile("^\\s*at\\s([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$"), 3) {

        @Override
        public String className(String... parts) {
            return StackTraceElementType.getClassName(parts[0]);
        }

        @Override
        public String method(String... parts) {
            return StackTraceElementType.getMethodName(parts[0]);
        }

        @Override
        public String file(String... parts) {
            return parts[1];
        }

        @Override
        public Integer lineNo(String... parts) {
            return Integer.parseInt(parts[2]);
        }
    },

    MORE(Pattern.compile("^\\s*...\\s*(\\d*)\\s*more$"), 1) {

        @Override
        public Integer more(String... parts) {
            return Integer.parseInt(parts[0]);
        }
    };

    private static final String[] NONE = new String[0];

    private Pattern pattern;

    private final int groups;

    StackTraceElementType(Pattern pattern, int groups) {
        this.pattern = pattern;
        this.groups = groups;
    }

    String[] parts(String line) {
        java.util.regex.Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return IntStream.range(0, groups)
                .mapToObj(group -> matcher.group(group + 1))
                .toArray(String[]::new);
        }
        return NONE;
    }

    private static String getMethodName(String part) {
        int lastDot = part.lastIndexOf(".");
        return lastDot < 0 ? null : part.substring(lastDot + 1);
    }

    private static String getClassName(String part) {
        int lastDot = part.lastIndexOf(".");
        return lastDot < 0 ? null : part.substring(0, lastDot);
    }
}
