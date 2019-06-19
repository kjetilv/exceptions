/*
 *     This file is part of Unearth.
 *
 *     Unearth is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Unearth is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package no.scienta.unearth.munch.model;

import no.scienta.unearth.munch.base.AbstractHashable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A chained fault is a mirror image of an actual {@link Throwable}.
 */
public class CauseChain extends AbstractHashable {

    private final Cause cause;

    private final String className;

    private final String message;

    private final List<String> printout;

    private final List<CauseFrame> causeFrames;

    private final CauseChain chainedCause;

    CauseChain(Cause cause, CauseChain chainedCause) {
        this(cause, chainedCause, null);
    }

    private CauseChain(Cause cause, CauseChain chainedCause, List<String> printout) {
        this.cause = cause;
        this.message = cause.getMessage();
        this.className = cause.getCauseStrand().getClassName();
        this.causeFrames = List.copyOf(cause.getCauseStrand().getCauseFrames());
        this.printout =
            printout == null || printout.isEmpty() ? Collections.emptyList() : List.copyOf(printout);
        this.chainedCause = chainedCause;
    }

    public String getClassName() {
        return className;
    }

    public String getMessage() {
        return message;
    }

    public Cause getCause() {
        return cause;
    }

    public CauseChain getCauseChain() {
        return chainedCause;
    }

    public List<CauseFrame> getCauseFrames() {
        return causeFrames;
    }

    public List<String> getPrintout() {
        return printout;
    }

    public CauseChain withPrintout(Function<CauseChain, List<String>> writer) {
        List<String> printout = writer.apply(this);
        return new CauseChain(
            cause,
            chainedCause == null
                ? null
                : chainedCause.withPrintout(writer),
            printout);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, cause, chainedCause);
    }
}
