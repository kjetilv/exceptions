package link.stuf.exceptions.core.inputs;

import java.util.Objects;

@SuppressWarnings("ExtendsThrowable")
public class ChameleonException extends Throwable {

    private final String proxiedClassName;

    ChameleonException(String proxiedClassName, String message, ChameleonException cause) {
        super(message, cause);
        this.proxiedClassName = Objects.requireNonNull(proxiedClassName, "proxiedClassName");
    }

    @Override
    public String toString() {
        return proxiedClassName + ": " + getMessage();
    }
}
