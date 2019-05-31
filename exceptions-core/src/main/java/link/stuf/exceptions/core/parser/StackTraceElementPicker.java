package link.stuf.exceptions.core.parser;

interface StackTraceElementPicker {

    default String module(String... parts) {
        return null;
    }

    default String moduleVersion(String... parts) {
        return null;
    }

    default String className(String... parts) {
        return null;
    }

    default String method(String... parts) {
        return null;
    }

    default String file(String... parts) {
        return null;
    }

    default String otherSource(String... parts) {
        return null;
    }

    default Integer lineNo(String... parts) {
        return null;
    }

    default Integer more(String... parts) {
        return null;
    }
}
