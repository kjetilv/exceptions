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

import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.util.Streams;
import no.scienta.unearth.util.Util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A fault has a {@link FaultStrand fault strand} and a list of {@link Cause causes}.
 */
@SuppressWarnings("unused")
public final class Fault extends AbstractHashableIdentifiable<FaultId> {

    private final FaultStrand faultStrand;

    private final List<Cause> causes;

    private Fault(FaultStrand faultStrand, Collection<Cause> causes) {
        this.faultStrand = Objects.requireNonNull(faultStrand);
        this.causes = Util.orEmptyList(causes);
        if (this.faultStrand.getCauseCount() != this.causes.size()) {
            throw new IllegalStateException(
                "Expected same arity: " + this.faultStrand.getCauseStrands().size() + '/' + this.causes.size());
        }
    }

    public static Fault create(Throwable throwable) {
        List<Cause> causes = Cause.causes(throwable);
        FaultStrand faultStrand = FaultStrand.create(causes);
        return new Fault(faultStrand, causes);
    }

    public static Fault create(FaultStrand faultStrand, Collection<Cause> causes) {
        return new Fault(faultStrand, causes);
    }

    public FaultStrand getFaultStrand() {
        return faultStrand;
    }

    public List<Cause> getCauses() {
        return causes;
    }

    public List<CauseStrand> getCauseStrands() {
        return causes.stream().map(Cause::getCauseStrand).collect(Collectors.toList());
    }

    public Throwable toChameleon() {
        return Streams.quickReduce(Streams.reverse(causes), (throwable, cause) -> cause.toChameleon(throwable));
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, faultStrand);
        hash(h, causes);
    }

    @Override
    protected FaultId id(UUID hash) {
        return new FaultId(hash);
    }

    @Override
    protected String toStringBody() {
        String ell = "...";
        int len = 30;
        return faultStrand + ", " + causes.stream()
            .map(Cause::getMessage)
            .map(message ->
                message.length() > len ? message.substring(0, len - ell.length()) + ell : message)
            .collect(Collectors.joining(" <= "));
    }
}
