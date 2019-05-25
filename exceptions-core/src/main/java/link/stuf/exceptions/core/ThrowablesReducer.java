package link.stuf.exceptions.core;

import link.stuf.exceptions.core.digest.ShadowThrowable;

public interface ThrowablesReducer {

    ShadowThrowable reduce(ShadowThrowable digest);
}
