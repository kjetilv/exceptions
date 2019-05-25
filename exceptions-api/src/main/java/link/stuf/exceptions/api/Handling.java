package link.stuf.exceptions.api;

import java.util.UUID;

public interface Handling {

    UUID getId();

    Throwable getSource();

    boolean isLoggable();

    boolean isNew();
}
