package link.stuf.exceptions.core;

import link.stuf.exceptions.digest.ThrowablesDigest;

import java.util.UUID;

public interface HandlingPolicy {

    UUID getId();

    Throwable getSource();

    ThrowablesDigest getReduced();

    boolean isLoggable();

    boolean isNew();
}
