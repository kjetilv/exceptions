package link.stuf.exceptions.api;

public interface ThrowablesClearingHouse {

    HandlingPolicy onException(Throwable throwable);
}
