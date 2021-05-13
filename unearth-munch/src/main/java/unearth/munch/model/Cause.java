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
 *     along with Unearth.  If not, see <https://www.gnu.org/licenses/>.
 */

package unearth.munch.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import unearth.munch.ChameleonException;
import unearth.munch.id.AbstractHashableIdentifiable;
import unearth.munch.id.CauseId;
import unearth.munch.print.CauseFrame;
import unearth.util.Streams;

/**
 * A cause has a {@link CauseStrand cause strand} and a given {@link #message message}.
 */
public final class Cause extends AbstractHashableIdentifiable<CauseId> {

    public static List<Cause> causes(Throwable throwable) {
        return Streams.reverse(Streams.causes(throwable))
            .map(t ->
                new Cause(t.getMessage(), CauseStrand.create(t),
                    suppressed(t)))
            .collect(Collectors.toList());
    }

    public static Cause create(String message, CauseStrand causeStrand) {
        return new Cause(message, causeStrand);
    }

    private final String message;

    private final CauseStrand causeStrand;

    private final Collection<Fault> suppressedFaults;

    private Cause(
        String message,
        CauseStrand causeStrand
    ) {
        this(message, causeStrand, null);
    }

    private Cause(
        String message,
        CauseStrand causeStrand,
        Collection<Fault> suppressedFaults
    ) {
        this.message = message;
        this.causeStrand = Objects.requireNonNull(causeStrand);
        this.suppressedFaults =
            suppressedFaults == null || suppressedFaults.isEmpty()
                ? Collections.emptyList()
                : List.copyOf(suppressedFaults);
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, causeStrand);
        hash(h, message);
    }

    @Override
    protected CauseId id(UUID hash) {
        return new CauseId(hash);
    }

    @Override
    protected StringBuilder withStringBody(StringBuilder sb) {
        return sb.append("causeStrand:").append(causeStrand).append(" message:").append(message);
    }

    public CauseStrand getCauseStrand() {
        return causeStrand;
    }

    public String getMessage() {
        return message;
    }

    Throwable toChameleon(Throwable cause) {
        Throwable exception =
            new ChameleonException(causeStrand.getClassName(), message, !suppressedFaults.isEmpty(), cause);
        exception.setStackTrace(causeStrand.getCauseFrames().stream()
            .map(CauseFrame::toStackTraceElement)
            .toArray(StackTraceElement[]::new));
        suppressedFaults.forEach(suppressedFault ->
            exception.addSuppressed(suppressedFault.toChameleon()));
        return exception;
    }

    private static Collection<Fault> suppressed(Throwable t) {
        Throwable[] suppressed = t.getSuppressed();
        return suppressed == null || suppressed.length == 0
            ? Collections.emptySet()
            : Arrays.stream(suppressed).map(Fault::create).collect(Collectors.toList());
    }
}
