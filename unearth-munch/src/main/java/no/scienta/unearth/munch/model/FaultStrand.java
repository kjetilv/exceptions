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
import no.scienta.unearth.munch.id.FaultStrandId;
import no.scienta.unearth.munch.util.Streams;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A fault strand consists of a list of {@link CauseStrand cause strand}.
 */
public class FaultStrand extends AbstractHashableIdentifiable<FaultStrandId> {

    private final List<CauseStrand> causeStrands;

    public static FaultStrand create(Throwable throwable) {
        List<Cause> causes = Streams.reverse(
            Streams.causes(throwable).map(Cause::create)
        ).collect(Collectors.toList());
        List<CauseStrand> causeStrands = causes.stream().map(Cause::getCauseStrand).collect(Collectors.toList());
        return new FaultStrand(causeStrands);
    }

    public List<CauseStrand> getCauseStrands() {
        return causeStrands;
    }

    public FaultStrand withCauseStrands(List<CauseStrand> causeStrands) {
        return new FaultStrand(causeStrands);
    }

    int getCauseCount() {
        return causeStrands.size();
    }

    private FaultStrand(Collection<CauseStrand> causeStrands) {
        if (Objects.requireNonNull(causeStrands).isEmpty()) {
            throw new IllegalArgumentException("Expected one or more causes");
        }
        this.causeStrands = Collections.unmodifiableList(new ArrayList<>(causeStrands));
    }

    @Override
    protected String toStringBody() {
        return "(" +
            causeStrands.stream().map(Objects::toString).collect(Collectors.joining(" <= ")) +
            ")";
    }

    @Override
    public void hashTo(Consumer<byte[]> h) {
        hash(h, causeStrands);
    }

    @Override
    protected FaultStrandId id(UUID hash) {
        return new FaultStrandId(hash);
    }
}
