package link.stuf.exceptions.core.inputs;

import java.util.Objects;

public class ChameleonException extends Exception {

    private final String proxiedClassName;

    ChameleonException(String proxiedClassName, String message, Throwable cause) {
        super(message, cause, false, true);
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
