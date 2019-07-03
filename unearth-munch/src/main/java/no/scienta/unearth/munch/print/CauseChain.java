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

package no.scienta.unearth.munch.print;

import no.scienta.unearth.munch.base.AbstractHashable;
import no.scienta.unearth.munch.model.Cause;
import no.scienta.unearth.munch.model.CauseFrame;
import no.scienta.unearth.munch.model.Fault;
import no.scienta.unearth.munch.util.Streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A cause chain is a moldable mirror image of an actual {@link Throwable}.
 */
public class CauseChain extends AbstractHashable {

    public static CauseChain build(Fault fault) {
        return Streams.reverse(fault.getCauses()).reduce(
            null,
            (chainedFault, cause) ->
                new CauseChain(cause, chainedFault), noCombine());
    }

    private final Cause cause;

    private final String className;

    private final String message;

    private final List<String> printout;

    private final List<CauseFrame> causeFrames;

    private final CauseChain chainedCause;

    public CauseChain(Cause cause, CauseChain chainedCause) {
        this(cause, chainedCause, null);
    }

    private CauseChain(Cause cause, CauseChain chainedCause, List<String> printout) {
        this.cause = cause;
        this.message = cause.getMessage();
        this.className = cause.getCauseStrand().getClassName();
        this.causeFrames = Collections.unmodifiableList(new ArrayList<>(cause.getCauseStrand().getCauseFrames()));
        this.printout = printout == null || printout.isEmpty()
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(printout));
        this.chainedCause = chainedCause;
    }

    public static <T> BinaryOperator<T> noCombine() {
        return (t1, t2) -> {
            throw new IllegalStateException("No combine: " + t1 + " <> " + t2);
        };
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

    List<CauseFrame> getCauseFrames() {
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
        hash(h, cause, chainedCause);
    }
}
