package link.stuf.exceptions.core;

public interface ThrowablesHandler {

    HandlingPolicy handle(Throwable throwable);
}
