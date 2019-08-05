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

package no.scienta.unearth.munch.model;

import no.scienta.unearth.munch.ChameleonException;
import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.CauseId;
import no.scienta.unearth.munch.print.CauseFrame;
import no.scienta.unearth.util.Streams;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A cause has a {@link CauseStrand cause strand} and a given {@link #message message}.
 */
public class Cause extends AbstractHashableIdentifiable<CauseId> {

    public static List<Cause> causes(Throwable throwable) {
        return Streams.reverse(Streams.causes(throwable))
            .map(t ->
                new Cause(CauseStrand.create(t), t.getMessage()))
            .collect(Collectors.toList());
    }

    private final CauseStrand causeStrand;

    private final String message;

    private Cause(CauseStrand causeStrand, String message) {
        this.causeStrand = Objects.requireNonNull(causeStrand);
        this.message = message;
    }

    public CauseStrand getCauseStrand() {
        return causeStrand;
    }

    public String getMessage() {
        return message;
    }

    Throwable toChameleon(Throwable t) {
        Throwable exception = new ChameleonException(causeStrand.getClassName(), message, t);
        exception.setStackTrace(
            causeStrand.getCauseFrames().stream()
                .map(CauseFrame::toStackTraceElement)
                .toArray(StackTraceElement[]::new));
        return exception;
    }

    @Override
    protected CauseId id(UUID hash) {
        return new CauseId(hash);
    }

    @Override
    protected String toStringBody() {
        return "causeStrand:" + causeStrand + " message:" + message;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, causeStrand);
        hash(h, message);
    }
}
