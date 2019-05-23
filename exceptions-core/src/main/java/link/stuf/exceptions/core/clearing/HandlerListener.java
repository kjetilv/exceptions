package link.stuf.exceptions.core.clearing;

import link.stuf.exceptions.core.digest.Digest;
import link.stuf.exceptions.core.digest.Occurrence;

public interface HandlerListener {

    void handled(Digest chain, Occurrence occurrence, Throwable source);
}
