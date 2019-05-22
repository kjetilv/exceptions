package link.stuf.exceptions.api;

import java.util.UUID;

public interface ThrowablesHandler {

    Handling onException(Throwable throwable);

    Throwable lookup(UUID uuid);
}
