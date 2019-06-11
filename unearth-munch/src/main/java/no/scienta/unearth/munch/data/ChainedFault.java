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

package no.scienta.unearth.munch.data;

import no.scienta.unearth.munch.base.AbstractHashable;

import java.util.function.Consumer;

/**
 * A chained fault is a mirror image of an actual {@link Throwable}.
 */
public class ChainedFault extends AbstractHashable {

    private final Cause cause;

    private final ChainedFault chainedCause;

    ChainedFault(Cause cause, ChainedFault chainedCause) {
        this.cause = cause;
        this.chainedCause = chainedCause;
    }

    public Cause getCause() {
        return cause;
    }

    public ChainedFault getChainedCause() {
        return chainedCause;
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hashHashables(h, cause, chainedCause);
    }
}