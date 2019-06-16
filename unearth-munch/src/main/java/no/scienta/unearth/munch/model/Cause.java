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

import no.scienta.unearth.munch.ChameleonException;
import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.CauseId;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A cause has a {@link CauseStrand cause strand} and a given {@link #message message}.
 */
public class Cause extends AbstractHashableIdentifiable<CauseId> {

    private final CauseStrand causeStrand;

    private final String message;

    static Cause create(Throwable cause) {
        return new Cause(CauseStrand.create(cause), cause.getMessage());
    }

    private Cause(CauseStrand causeStrand, String message) {
        this.causeStrand = Objects.requireNonNull(causeStrand);
        this.message = message;
    }

    public CauseStrand getCauseStrand() {
        return causeStrand;
    }

    public Cause withCauseStrand(CauseStrand causeStrand) {
        return new Cause(causeStrand, message);
    }

    public String getMessage() {
        return message;
    }

    ChainedFault chain(ChainedFault cause) {
        return new ChainedFault(this, cause);
    }

    Throwable toChameleon(Throwable t) {
        Throwable exception = new ChameleonException(causeStrand.getClassName(), message, t);
        exception.setStackTrace(causeStrand.getStackTrace().toArray(StackTraceElement[]::new));
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
        hashHashables(h, causeStrand);
        hashString(h, message);
    }
}
