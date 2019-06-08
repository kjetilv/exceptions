package link.stuf.exceptions.munch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ThrowableStack extends AbstractHashedIdentified<ThrowableStackId> {

    private static final Logger log = LoggerFactory.getLogger(ThrowableStack.class);

    static ThrowableStack create(Throwable throwable) {
        return new ThrowableStack(className(throwable), copy(throwable.getStackTrace()));
    }

    private static final Field formatField = formatField();

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
    protected ThrowableStackId id(UUID hash) {
        return new ThrowableStackId(hash);
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
        return Arrays.stream(stackTrace).map(orig ->
            withFormat(orig, new StackTraceElement(
                orig.getClassLoaderName(),
                orig.getModuleName(),
                orig.getModuleVersion(),
                orig.getClassName(),
                orig.getMethodName(),
                orig.getFileName(),
                orig.getLineNumber()
            ))).collect(Collectors.toUnmodifiableList());
    }

    private static StackTraceElement withFormat(StackTraceElement original, StackTraceElement copy) {
        if (formatField != null) {
            try {
                formatField.set(copy, formatField.get(original));
            } catch (IllegalAccessException e) {
                log.info("Could not replicate format field: " + original, e);
            }
        }
        return copy;
    }

    private static String className(Throwable throwable) {
        if (throwable instanceof ChameleonException) {
            return ((ChameleonException) throwable).getProxiedClassName();
        }
        return throwable.getClass().getName();
    }

    private static Field formatField() {
        try {
            Field field = StackTraceElement.class.getDeclaredField("format");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            log.error("Could not find 'format' field", e);
            return null;
        }
    }

    @Override
    String toStringBody() {
        int dotIndex = className.lastIndexOf(".");
        return (dotIndex >= 0 ? className.substring(dotIndex + 1) : className) + " <" + stackTrace.size() + ">";
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        h.accept(this.className.getBytes(StandardCharsets.UTF_8));
        for (StackTraceElement el : this.stackTrace) {
            h.accept(el.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
