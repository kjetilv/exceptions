package link.stuf.exceptions.core.inputs;

import java.util.Objects;

public class ChameleonException extends Throwable {

    private final String className;

    public ChameleonException(String className, String message, ChameleonException cause) {
        super(message, cause);
        this.className = Objects.requireNonNull(className, "className");
    }

    @Override
    public String toString() {
        return className + ": " + getMessage();
    }
}
