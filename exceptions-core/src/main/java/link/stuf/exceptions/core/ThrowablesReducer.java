package link.stuf.exceptions.core;

import link.stuf.exceptions.core.throwables.ShadowThrowable;

public interface ThrowablesReducer {

    ShadowThrowable reduce(ShadowThrowable digest);
}
