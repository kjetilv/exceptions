package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.ThrowableStack;

public interface ThrowablesReducer {

    ThrowableStack reduce(ThrowableStack digest);
}
