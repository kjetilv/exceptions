package link.stuf.exceptions.munch.dto;

import link.stuf.exceptions.munch.AbstractHashed;
import link.stuf.exceptions.munch.data.CauseType;

import java.util.function.Consumer;

public class ThrowableDto extends AbstractHashed {

    private final String className;

    private final String message;

    private final CauseType stack;

    private final ThrowableDto cause;

    public ThrowableDto(String className, String message, CauseType stack, ThrowableDto cause) {
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

    public CauseType getStack() {
        return stack;
    }

    public ThrowableDto getCause() {
        return cause;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashStrings(h, className, message);
        hashHashables(h, stack);
    }
}
