package link.stuf.exceptions.munch;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableStack extends AbstractHashed implements Identified<ThrowableStackId> {

    static ThrowableStack create(Throwable throwable) {
        return new ThrowableStack(className(throwable), copy(throwable.getStackTrace()));
    }

    private static final Field formatField;

    static {
        try {
            formatField = StackTraceElement.class.getDeclaredField("format");
            formatField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Could not find 'format' field", e);
        }
    }

    private final String className;

    private final List<StackTraceElement> stackTrace;

    private ThrowableStack(String className, List<StackTraceElement> stackTrace) {
        this.className = className;
        this.stackTrace = stackTrace == null || stackTrace.isEmpty()
            ? Collections.emptyList()
            : List.copyOf(stackTrace);
    }

    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    @Override
    public ThrowableStackId getId() {
        return new ThrowableStackId(getHash());
    }

    public String getClassName() {
        return className;
    }

    Throwable toException(String message, Throwable cause) {
        Throwable exception = new ChameleonException(getClassName(), message, cause);
        exception.setStackTrace(getStackTrace().toArray(StackTraceElement[]::new));
        return exception;
    }

    ThrowableDto toExceptionDto(String message, ThrowableStack stack, ThrowableDto cause) {
        return new ThrowableDto(getClassName(), message, stack, cause);
    }

    public ThrowableStack withStacktrace(List<StackTraceElement> stackTrace) {
        return new ThrowableStack(className, stackTrace);
    }

    private static List<StackTraceElement> copy(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).map(original ->
            withFormat(original, new StackTraceElement(
                original.getClassLoaderName(),
                original.getModuleName(),
                original.getModuleVersion(),
                original.getClassName(),
                original.getMethodName(),
                original.getFileName(),
                original.getLineNumber()
            ))).collect(Collectors.toUnmodifiableList());
    }

    private static StackTraceElement withFormat(StackTraceElement original, StackTraceElement copy) {
        try {
            formatField.set(copy, formatField.get(original));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not replicate: " + original, e);
        }
        return copy;
    }

    private static String className(Throwable throwable) {
        if (throwable instanceof ChameleonException) {
            return ((ChameleonException) throwable).getProxiedClassName();
        }
        return throwable.getClass().getName();
    }

    @Override
    public String toString() {
        int dotIndex = className.lastIndexOf(".");
        return getClass().getSimpleName() + "[" +
            (dotIndex >= 0 ? className.substring(dotIndex + 1) : className) + " <" + stackTrace.size() + ">" +
            "]";
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ThrowableStack &&
            Objects.equals(className, ((ThrowableStack) o).className) &&
            Objects.equals(stackTrace, ((ThrowableStack) o).stackTrace);
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        hash.accept(this.className.getBytes(StandardCharsets.UTF_8));
        for (StackTraceElement el : this.stackTrace) {
            hash.accept(el.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
