package link.stuf.exceptions.core;

public interface FaultHandler {

    HandlingPolicy handle(Throwable throwable);
}
