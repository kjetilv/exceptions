package link.stuf.exceptions.munch;

import java.util.function.Consumer;

public class ThrowableDto extends AbstractHashed {

    private final String className;

    private final String message;

    private final ThrowableStack stack;

    private final ThrowableDto cause;

    ThrowableDto(String className, String message, ThrowableStack stack, ThrowableDto cause) {
        this.className = className;
        this.message = message;
        this.stack = stack;
        this.cause = cause;
    }

    public String getClassName() {
        return className;
    }

    public String getMessage() {
        return message;
    }

    public ThrowableStack getStack() {
        return stack;
    }

    public ThrowableDto getCause() {
        return cause;
    }

    @Override
    public void hashTo(Consumer<byte[]> hash) {
        hashStrings(hash, className, message);
        hash(hash, stack);
    }
}
