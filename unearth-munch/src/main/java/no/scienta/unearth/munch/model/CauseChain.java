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
import no.scienta.unearth.munch.print.ThrowableRenderer;
import no.scienta.unearth.munch.util.Streams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A cause chain is a moldable mirror image of an actual {@link Throwable}.
 */
public class CauseChain extends AbstractHashable {

    public static CauseChain build(Fault fault) {
        return Streams.quickReduce(
            Streams.reverse(fault.getCauses()),
            (chainedFault, cause) ->
                new CauseChain(cause, chainedFault, null));
    }

    private final Cause cause;

    private final String className;

    private final String message;

    private final List<String> printout;

    private final List<CauseFrame> causeFrames;

    private final CauseChain chainedCause;

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

    public <T> List<T> map(Function<CauseChain, T> toT) {
        List<T> ts = new ArrayList<>();
        return map(toT, ts);
    }

    private <T> List<T> map(Function<CauseChain, T> toT, List<T> ts) {
        ts.add(toT.apply(this));
        return chainedCause == null ? Collections.unmodifiableList(ts) : map(toT, ts);
    }

    public CauseChain withPrintout(ThrowableRenderer renderer) {
        List<String> printout = renderer.render(this);
        return new CauseChain(
            cause,
            chainedCause == null
                ? null
                : chainedCause.withPrintout(renderer),
            printout);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, cause, chainedCause);
    }
}
