package link.stuf.exceptions.core.inputs;

import java.util.Objects;

@SuppressWarnings("ExtendsThrowable")
public class ChameleonException extends Throwable {

    private final String proxiedClassName;

    ChameleonException(String proxiedClassName, String message, Throwable cause) {
        super(message, cause);
        this.proxiedClassName = Objects.requireNonNull(proxiedClassName, "proxiedClassName");
    }

    public String getProxiedClassName() {
        return proxiedClassName;
    }

    @Override
    public String toString() {
        return proxiedClassName + ": " + getMessage();
    }
}
