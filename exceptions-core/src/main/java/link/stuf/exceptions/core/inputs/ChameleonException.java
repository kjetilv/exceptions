package link.stuf.exceptions.core.inputs;

import link.stuf.exceptions.api.NamedException;

import java.util.Objects;

public class ChameleonException extends Exception implements NamedException {

    private final String proxiedClassName;

    ChameleonException(String proxiedClassName, String message, Throwable cause) {
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
