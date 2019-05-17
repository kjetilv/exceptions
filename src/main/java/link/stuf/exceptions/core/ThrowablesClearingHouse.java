package link.stuf.exceptions.core;

public interface ThrowablesClearingHouse {

    HandlingPolicy onException(Throwable throwable);
}
