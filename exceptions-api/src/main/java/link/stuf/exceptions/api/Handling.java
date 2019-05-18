package link.stuf.exceptions.api;

import java.util.UUID;

public interface Handling {

    UUID getId();

    Throwable getSource();

    Throwable getReduced();

    boolean isLoggable();

    boolean isNew();
}
