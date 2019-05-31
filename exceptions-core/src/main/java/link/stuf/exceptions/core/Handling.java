package link.stuf.exceptions.core;

import java.util.UUID;

public interface Handling {

    UUID getId();

    Throwable getSource();

    boolean isLoggable();

    boolean isNew();
}
