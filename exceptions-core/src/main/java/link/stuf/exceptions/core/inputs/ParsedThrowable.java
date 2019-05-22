package link.stuf.exceptions.core.inputs;

class ParsedThrowable {

    private final String exceptionHeading;

    private final StackTraceElement[] parsedStackTrace;

    ParsedThrowable(String exceptionHeading, StackTraceElement[] parsedStackTrace) {
        this.exceptionHeading = exceptionHeading;
        this.parsedStackTrace = parsedStackTrace;
    }

    ChameleonException reconstruct(ChameleonException caused) {
        String[] split = exceptionHeading.split(": ", 2);
        String exceptionName = split[0];
        String message = split[1];
        ChameleonException chameleonException = new ChameleonException(exceptionName, message, caused);
        chameleonException.setStackTrace(parsedStackTrace);
        return chameleonException;
    }
}
