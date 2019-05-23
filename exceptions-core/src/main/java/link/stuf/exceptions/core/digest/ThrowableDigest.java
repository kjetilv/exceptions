package link.stuf.exceptions.core.digest;

import link.stuf.exceptions.core.inputs.ChameleonException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableDigest extends AbstractHashed {

    private final String message;

    private final String className;

    private final List<StackTraceElement> stackTrace;

    private final ThrowableDigest cause;

    ThrowableDigest(Throwable throwable, ThrowableDigest cause) {
        this(
            className(throwable),
            throwable.getMessage(),
            copy(throwable.getStackTrace()),
            cause
        );
    }

    private ThrowableDigest(
        String className,
        String message,
        List<StackTraceElement> stackTrace,
        ThrowableDigest cause
    ) {
        this.className = className;
        this.message = message;
        this.stackTrace = stackTrace;
        this.cause = cause;
    }

    private String getMessage() {
        return message;
    }

    public ThrowableDigest getCause() {
        return cause;
    }

    List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    Throwable toException(Throwable cause) {
        Throwable exception = new Throwable(getClassName() + ": " + getMessage(), cause);
        exception.setStackTrace(getStackTrace().toArray(StackTraceElement[]::new));
        return exception;
    }

    ThrowableDigest withStacktrace(List<StackTraceElement> stackTrace) {
        return new ThrowableDigest(className, message, stackTrace, cause);
    }

    private String getClassName() {
        return className;
    }

    private static List<StackTraceElement> copy(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(element ->
            new StackTraceElement(
                element.getClassLoaderName(),
                element.getModuleName(),
                element.getModuleVersion(),
                element.getClassName(),
                element.getMethodName(),
                element.getFileName(),
                element.getLineNumber()
            )).collect(Collectors.toUnmodifiableList());
    }

    private static String className(Throwable throwable) {
        if (throwable instanceof ChameleonException) {
            return ((ChameleonException)throwable).getProxiedClassName();
        }
        return throwable.getClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ThrowableDigest &&
            Objects.equals(className, ((ThrowableDigest) o).className) &&
            Objects.equals(stackTrace, ((ThrowableDigest) o).stackTrace);
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        hash.accept(this.className.getBytes(StandardCharsets.UTF_8));
        for (StackTraceElement el : this.stackTrace) {
            hash.accept(el.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
