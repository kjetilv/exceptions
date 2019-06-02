package link.stuf.exceptions.munch;

import java.util.Objects;

public class ChameleonException extends Exception implements NamedException {

    private final String proxiedClassName;

    public ChameleonException(String proxiedClassName, String message, Throwable cause) {
        super(message, cause, false, true);
        this.proxiedClassName = Objects.requireNonNull(proxiedClassName, "proxiedClassName");
    }

    @Override
    public String getProxiedClassName() {
        return proxiedClassName;
    }

    @Override
    public String toString() {
        return proxiedClassName + ": " + getMessage();
    }
}
