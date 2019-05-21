package link.stuf.exceptions.core.inputs;

import java.util.regex.Pattern;
import java.util.stream.IntStream;

enum StackTraceElementParser {

    NATIVE_METHOD_WITH_MODULE(Pattern.compile("^\\s*at\\s([\\w.]*)/([\\w.]*)\\(Native Method\\)$"), 2) {
        @Override
        String module(String... matches) {
            return matches[0];
        }

        @Override
        String className(String... matches) {
            return StackTraceElementParser.getClassName(matches[1]);
        }

        @Override
        String method(String... matches) {
            return StackTraceElementParser.getMethodName(matches[1]);
        }
    },

    BASIC_NATIVE_METHOD(Pattern.compile("^\\s*at\\s([$\\w.]*)\\(Native Method\\)$"), 1) {

        @Override
        String className(String... matches) {
            return StackTraceElementParser.getClassName(matches[0]);
        }

        @Override
        String method(String... matches) {
            return StackTraceElementParser.getMethodName(matches[0]);
        }
    },

    WITH_MODULE(Pattern.compile("^\\s*at\\s([$\\w.]*)/([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$"), 4) {
        @Override
        String module(String... matches) {
            return matches[0];
        }

        @Override
        String className(String... matches) {
            return StackTraceElementParser.getClassName(matches[1]);
        }

        @Override
        String method(String... matches) {
            return StackTraceElementParser.getMethodName(matches[1]);
        }

        @Override
        String file(String... matches) {
            return matches[2];
        }

        @Override
        Integer lineNo(String... matches) {
            return Integer.parseInt(matches[3]);
        }
    },

    BASIC(Pattern.compile("^\\s*at\\s([$\\w.]*)\\(([$\\w.]*):(\\d*)\\)$"), 3) {
        @Override
        String className(String... matches) {
            return StackTraceElementParser.getClassName(matches[0]);
        }

        @Override
        String method(String... matches) {
            return StackTraceElementParser.getMethodName(matches[0]);
        }

        @Override
        String file(String... matches) {
            return matches[1];
        }

        @Override
        Integer lineNo(String... matches) {
            return Integer.parseInt(matches[2]);
        }
    },

    MORE(Pattern.compile("^\\s*...\\s*(\\d*)\\s*more$"), 1) {
        @Override
        Integer more(String... matches) {
            return Integer.parseInt(matches[0]);
        }
    };

    private static final String[] NONE = new String[0];

    private Pattern pattern;

    private final int groups;

    StackTraceElementParser(Pattern pattern, int groups) {
        this.pattern = pattern;
        this.groups = groups;
    }

    String[] matches(String line) {
        java.util.regex.Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return IntStream.range(0, groups)
                .mapToObj(group ->
                    matcher.group(group + 1))
                .toArray(String[]::new);
        }
        return NONE;
    }

    String module(String... matches) {
        return null;
    }

    String moduleVersion(String... matches) {
        return null;
    }

    String className(String... matches) {
        return null;
    }

    String method(String... matches) {
        return null;
    }

    String file(String... matches) {
        return null;
    }

    Integer lineNo(String... matches) {
        return null;
    }

    Integer more(String... matches) {
        return null;
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
