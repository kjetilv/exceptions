package link.stuf.exceptions.core.digest;

import link.stuf.exceptions.core.hashing.AbstractHashed;
import link.stuf.exceptions.core.inputs.ChameleonException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ShadowThrowable extends AbstractHashed {

    static ShadowThrowable create(Throwable throwable) {
        return new ShadowThrowable(className(throwable), throwable.getMessage(), copy(throwable.getStackTrace()));
    }

    private final String message;

    private final String className;

    private final List<StackTraceElement> stackTrace;

    private ShadowThrowable(String className, String message, List<StackTraceElement> stackTrace) {
        this.className = className;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    Throwable toException(Throwable cause) {
        Throwable exception = new Throwable(getClassName() + ": " + getMessage(), cause);
        exception.setStackTrace(getStackTrace().toArray(StackTraceElement[]::new));
        return exception;
    }

    public ShadowThrowable withStacktrace(List<StackTraceElement> stackTrace) {
        return new ShadowThrowable(className, message, stackTrace);
    }

    private String getMessage() {
        return message;
    }

    private String getClassName() {
        return className;
    }

    private static List<StackTraceElement> copy(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(traceElement ->
            new StackTraceElement(
                traceElement.getClassLoaderName(),
                traceElement.getModuleName(),
                traceElement.getModuleVersion(),
                traceElement.getClassName(),
                traceElement.getMethodName(),
                traceElement.getFileName(),
                traceElement.getLineNumber()
            )
        ).collect(Collectors.toUnmodifiableList());
    }

    private static String className(Throwable throwable) {
        if (throwable instanceof ChameleonException) {
            return ((ChameleonException) throwable).getProxiedClassName();
        }
        return throwable.getClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ShadowThrowable &&
            Objects.equals(className, ((ShadowThrowable) o).className) &&
            Objects.equals(stackTrace, ((ShadowThrowable) o).stackTrace);
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        hash.accept(this.className.getBytes(StandardCharsets.UTF_8));
        for (StackTraceElement el : this.stackTrace) {
            hash.accept(el.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
