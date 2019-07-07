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

import no.scienta.unearth.munch.base.AbstractHashableIdentifiable;
import no.scienta.unearth.munch.id.FaultId;
import no.scienta.unearth.munch.util.Streams;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A fault has a {@link FaultStrand fault strand} and a list of {@link Cause causes}.
 */
public class Fault extends AbstractHashableIdentifiable<FaultId> {

    public static Fault create(Throwable throwable) {
        return new Fault(FaultStrand.create(throwable), causes(throwable));
    }

    private final FaultStrand faultStrand;

    private final List<Cause> causes;

    private Fault(FaultStrand faultStrand, Collection<Cause> causes) {
        this.faultStrand = Objects.requireNonNull(faultStrand);
        this.causes = causes == null || causes.isEmpty()
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(causes));
        if (this.faultStrand.getCauseCount() != this.causes.size()) {
            throw new IllegalStateException(
                "Expected same arity: " + this.faultStrand.getCauseStrands().size() + "/" + this.causes.size());
        }
    }

    public FaultStrand getFaultStrand() {
        return faultStrand;
    }

    public List<Cause> getCauses() {
        return causes;
    }

    public List<String> getMessages() {
        return causes.stream().map(cause ->
            cause.getCauseStrand().getClassName() + ": " + cause.getMessage()
        ).collect(Collectors.toList());
    }

    public Throwable toCameleon() {
        return Streams.quickReduce(Streams.reverse(causes), (throwable, cause) ->
            cause.toChameleon(throwable));
    }

    private static List<Cause> causes(Throwable throwable) {
        return Streams.causes(throwable).map(Cause::create).collect(Collectors.toList());
    }

    @Override
    protected FaultId id(UUID hash) {
        return new FaultId(hash);
    }

    @Override
    protected String toStringBody() {
        return "faultStrand:" + faultStrand + ": " + causes.size();
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, faultStrand);
        hash(h, causes);
    }
}
