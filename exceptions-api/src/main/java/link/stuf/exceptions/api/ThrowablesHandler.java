package link.stuf.exceptions.api;

import java.util.UUID;

public interface ThrowablesHandler {

    Handling handle(Throwable throwable);

    Throwable lookup(UUID uuid);
}
