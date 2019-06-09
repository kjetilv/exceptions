package link.stuf.exceptions.core;

import link.stuf.exceptions.munch.data.CauseType;

public interface ThrowablesReducer {

    CauseType reduce(CauseType digest);
}
